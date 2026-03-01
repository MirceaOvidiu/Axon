import os
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.signal import find_peaks, butter, filtfilt

# Import sparc and helper functions from the existing script
# We assume sparc_analysis.py is in the same directory
try:
    from sparc_analysis import sparc, angular_speed, interpret_sparc
except ImportError:
    # Fallback if running from a different directory or if import fails
    import sys
    sys.path.append(os.path.dirname(os.path.abspath(__file__)))
    from sparc_analysis import sparc, angular_speed, interpret_sparc

def butter_lowpass_filter(data, cutoff, fs, order=4):
    nyq = 0.5 * fs
    normal_cutoff = cutoff / nyq
    b, a = butter(order, normal_cutoff, btype='low', analog=False)
    y = filtfilt(b, a, data)
    return y

def segment_movements(speed, fs, height_factor=0.2, min_distance_sec=1.0):
    """
    Segment the speed profile into individual repetitions.
    
    Strategy:
    1. Low-pass filter the speed to remove noise.
    2. Find peaks that represent the core of a movement.
    3. For each peak, search backwards and forwards to find the start/end 
       where speed drops below a threshold.
    """
    
    # 1. Smooth signal for segmentation purposes
    # A 3-5 Hz cutoff is usually good for human movement envelopes
    speed_smooth = butter_lowpass_filter(speed, cutoff=3.0, fs=fs)
    
    # 2. Find peaks
    # height: minimum speed to be considered a rep (e.g. 20% of max speed)
    # distance: minimum time between peaks
    max_speed = np.max(speed_smooth)
    min_height = max_speed * height_factor
    distance = int(min_distance_sec * fs)
    
    peaks, _ = find_peaks(speed_smooth, height=min_height, distance=distance)
    
    segments = []
    
    # Threshold to define start/stop of movement (e.g. 5% of peak or absolute value)
    # Using a dynamic threshold based on the specific peak is often better
    
    for peak_idx in peaks:
        peak_val = speed_smooth[peak_idx]
        threshold = peak_val * 0.10  # Stop when speed drops to 10% of this rep's peak
        
        # Search backwards for start
        start_idx = peak_idx
        while start_idx > 0 and speed_smooth[start_idx] > threshold:
            start_idx -= 1
            
        # Search forwards for end
        end_idx = peak_idx
        while end_idx < len(speed_smooth) - 1 and speed_smooth[end_idx] > threshold:
            end_idx += 1
            
        # Add a small buffer if possible
        start_idx = max(0, start_idx - 5)
        end_idx = min(len(speed) - 1, end_idx + 5)
        
        # Check if this segment overlaps significantly with the previous one
        if segments and start_idx < segments[-1][1]:
            # Merge if peaks are very close (optional, here we just skip or take the merged extent)
            # For now, let's keep distinct reps, but ensure we don't duplicate
            pass
        
        segments.append((start_idx, end_idx))
        
    return segments, speed_smooth, peaks

def main():
    # --- Load Data ---
    script_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(script_dir, "sessionData.csv")
    
    if not os.path.exists(csv_path):
        print(f"Error: {csv_path} not found.")
        return

    df = pd.read_csv(csv_path)
    
    # Sampling frequency (matches sparc_analysis.py default)
    FS = 25.0
    
    # Calculate angular speed
    raw_speed = angular_speed(df["gyro_x"].values, df["gyro_y"].values, df["gyro_z"].values)
    
    # --- Segment Data ---
    print(f"Analyzing {len(raw_speed)} samples at {FS} Hz...")
    segments, speed_smooth, peaks = segment_movements(raw_speed, FS)
    
    print(f"\nFound {len(segments)} potential repetitions.\n")
    
    # --- Analyze Each Segment ---
    results = []
    
    for i, (start, end) in enumerate(segments):
        # Extract the segment from the RAW speed (not smoothed) for SPARC calc
        # SPARC needs the original frequency content
        rep_speed = raw_speed[start:end]
        
        # Skip very short segments (e.g. < 0.5s)
        if len(rep_speed) < 0.5 * FS:
            continue
            
        sparc_val, _, _ = sparc(rep_speed, FS)
        duration = (end - start) / FS
        
        results.append({
            "Rep": i + 1,
            "Start_Idx": start,
            "End_Idx": end,
            "Duration_s": duration,
            "SPARC": sparc_val,
            "Interpretation": interpret_sparc(sparc_val)
        })
        
        print(f"Rep {i+1}: Duration={duration:.2f}s | SPARC={sparc_val:.4f} ({interpret_sparc(sparc_val)})")

    # --- Plotting ---
    plt.figure(figsize=(12, 6))
    time = np.arange(len(raw_speed)) / FS
    
    # Plot raw speed
    plt.plot(time, raw_speed, color='lightgray', label='Raw Speed')
    # Plot smoothed speed used for segmentation
    plt.plot(time, speed_smooth, color='steelblue', label='Smoothed (for detection)')
    
    # Plot detected Segments
    for i, res in enumerate(results):
        s, e = res["Start_Idx"], res["End_Idx"]
        mid_time = time[peaks[i]] if i < len(peaks) else (time[s] + time[e])/2
        
        # Highlight the segment
        plt.axvspan(time[s], time[e], color='green', alpha=0.1)
        
        # Label the rep
        plt.text(mid_time, speed_smooth[peaks[i]] + 0.5, 
                 f"Rep {res['Rep']}\n{res['SPARC']:.2f}", 
                 ha='center', fontsize=9, fontweight='bold', color='darkgreen')

    plt.title(f"Segmentation & SPARC Analysis ({len(results)} valid reps)")
    plt.xlabel("Time (s)")
    plt.ylabel("Angular Speed (rad/s)")
    plt.legend()
    plt.tight_layout()
    
    save_path = os.path.join(script_dir, "segmented_report.png")
    plt.savefig(save_path, dpi=150)
    print(f"\nSaved segmentation plot to: {save_path}")
    plt.show()

if __name__ == "__main__":
    main()
