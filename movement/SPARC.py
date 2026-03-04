import os
import sys
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.signal import find_peaks, butter, filtfilt, welch
from dataclasses import dataclass

# ---------------------------------------------------------------------------
# 1. SPARC Core & Helper Functions (from sparc_analysis.py)
# ---------------------------------------------------------------------------

def sparc(speed: np.ndarray, fs: float,
          padlevel: int = 4, fc: float = 10.0, amp_th: float = 0.05):
    """
    Compute the SPARC smoothness metric for a speed profile.

    Parameters
    ----------
    speed : 1-D array
        Movement speed (magnitude of velocity / angular velocity).
    fs : float
        Sampling frequency (Hz).
    padlevel : int
        Zero-padding factor for the FFT (2**padlevel * len(speed)).
    fc : float
        Max cutoff frequency (Hz) for the arc-length calculation.
    amp_th : float
        Amplitude threshold (fraction of max) below which the spectrum
        is not considered when choosing the adaptive cutoff.

    Returns
    -------
    sparc_value : float
        The spectral arc length.  Values closer to 0 indicate smoother
        movement; more negative values indicate jerkier movement.
    freq : ndarray
        Frequency array (Hz) of the spectrum used.
    magnitude : ndarray
        Normalized magnitude spectrum.
    """
    # Helper to clean invalid values
    if len(speed) == 0:
        return -999.0, np.array([]), np.array([])
        
    n = len(speed)
    nfft = int(2 ** (np.ceil(np.log2(n)) + padlevel))

    # FFT and normalised magnitude spectrum
    freq_full = np.fft.rfftfreq(nfft, d=1.0 / fs)
    spec = np.abs(np.fft.rfft(speed, n=nfft))
    spec_norm = spec / spec.max() if spec.max() > 0 else spec

    # Adaptive frequency cutoff: highest freq where amplitude > amp_th, capped at fc
    above_th = np.where((freq_full <= fc) & (spec_norm >= amp_th))[0]
    if len(above_th) == 0:
        fc_idx = 1
    else:
        fc_idx = above_th[-1] + 1      # include that bin

    freq = freq_full[:fc_idx]
    magnitude = spec_norm[:fc_idx]

    # Arc length of the normalised magnitude spectrum
    # Check for empty arrays to avoid errors
    if len(freq) < 2:
        return -999.0, freq, magnitude

    d_freq = np.diff(freq) / (freq[-1] - freq[0]) if (freq[-1] - freq[0]) > 0 else np.diff(freq)
    d_mag = np.diff(magnitude)
    arc_lengths = np.sqrt(d_freq ** 2 + d_mag ** 2)
    sparc_value = -float(np.sum(arc_lengths))

    return sparc_value, freq, magnitude

def interpret_sparc(value: float) -> str:
    """
    Qualitative interpretation of a SPARC value.
    Reference ranges (approximate, task-dependent):
      -1.0 …  0.0  →  very smooth / fluid
      -2.0 … -1.0  →  moderately smooth
      -4.0 … -2.0  →  somewhat jerky
         < -4.0     →  very jerky / fragmented
    """
    if value > -1.0:
        return "Very Smooth"
    elif value > -2.0:
        return "Moderately Smooth"
    elif value > -4.0:
        return "Somewhat Jerky"
    else:
        return "Very Jerky / Fragmented"

def angular_speed(gyro_x: np.ndarray, gyro_y: np.ndarray, gyro_z: np.ndarray) -> np.ndarray:
    """Euclidean norm of 3-axis gyroscope → angular speed (rad/s)."""
    return np.sqrt(gyro_x ** 2 + gyro_y ** 2 + gyro_z ** 2)

def rms(signal: np.ndarray) -> float:
    return float(np.sqrt(np.mean(signal ** 2)))

def jerk_metric(speed: np.ndarray, fs: float) -> float:
    """RMS of the second derivative of speed (≈ jerk of angular motion)."""
    if len(speed) < 3:
        return 0.0
    accel = np.gradient(speed, 1.0 / fs)
    jerk = np.gradient(accel, 1.0 / fs)
    return rms(jerk)

# ---------------------------------------------------------------------------
# 2. Segmentation Logic (from segment_and_analyze.py)
# ---------------------------------------------------------------------------

def butter_lowpass_filter(data, cutoff, fs, order=4):
    """Apply a low-pass filter to smooth the data for segmentation."""
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
    
    Returns:
        segments: List of tuples (start_idx, end_idx)
        speed_smooth: The smoothed speed signa used for detection
        peaks: Indices of the detected peaks
    """
    
    # 1. Smooth signal for segmentation purposes
    # A 3-5 Hz cutoff is usually good for human movement envelopes
    speed_smooth = butter_lowpass_filter(speed, cutoff=3.0, fs=fs)
    
    # 2. Find peaks
    # height: minimum speed to be considered a rep (e.g. 20% of max speed)
    # distance: minimum time between peaks
    max_speed = np.max(speed_smooth) if len(speed_smooth) > 0 else 0
    if max_speed == 0:
        return [], speed_smooth, []

    min_height = max_speed * height_factor
    distance = int(min_distance_sec * fs)
    
    peaks, _ = find_peaks(speed_smooth, height=min_height, distance=distance)
    
    segments = []
    
    # Threshold to define start/stop of movement 
    # (e.g. 10% of the peak velocity for *that specific* repetition)
    
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
            
        # Add a small buffer (optional, e.g. 5 samples ~0.2s at 25Hz)
        start_idx = max(0, start_idx - 5)
        end_idx = min(len(speed) - 1, end_idx + 5)
        
        # Check overlaps
        # If the new start is before the previous end, we have an overlap.
        # We can merge them or just take the new one if the overlap is small.
        # Here we strictly resolve overlap by capping the previous end or skipping if too close.
        if segments:
            prev_start, prev_end = segments[-1]
            if start_idx < prev_end:
                # If peak is within previous segment, it might be a double peak of same movement
                # But find_peaks 'distance' param handles most of that.
                # If it's a new peak but starts early, let's just adjust boundaries to midpoint
                mid_point = (prev_end + start_idx) // 2
                segments[-1] = (prev_start, mid_point)
                start_idx = mid_point
        
        segments.append((start_idx, end_idx))
        
    return segments, speed_smooth, peaks

# ---------------------------------------------------------------------------
# 3. Main Analysis & Plotting
# ---------------------------------------------------------------------------

def main():
    # --- Configuration ---
    FS = 25.0  # Sampling frequency in Hz (approx for WearOS)
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    CSV_PATH = os.path.join(SCRIPT_DIR, "sessionData.csv")
    
    # --- Load Data ---
    if not os.path.exists(CSV_PATH):
        print(f"Error: {CSV_PATH} not found.")
        print("Please ensure sessionData.csv is in the same directory.")
        return

    try:
        df = pd.read_csv(CSV_PATH)
        print(f"Loaded {len(df)} samples from {CSV_PATH}")
    except Exception as e:
        print(f"Error reading CSV: {e}")
        return
    
    # Check columns
    required_cols = ["gyro_x", "gyro_y", "gyro_z"]
    if not all(col in df.columns for col in required_cols):
        print(f"Error: CSV missing required columns: {required_cols}")
        return
    
    # --- Pre-processing ---
    # Calculate angular speed (magnitude)
    raw_speed = angular_speed(df["gyro_x"].values, df["gyro_y"].values, df["gyro_z"].values)
    
    # --- Segmentation ---
    print(f"Segmenting movements (assuming {FS} Hz)...")
    segments, speed_smooth, peaks = segment_movements(raw_speed, FS)
    print(f"Found {len(segments)} potential repetitions.")
    
    if len(segments) == 0:
        print("No movements detected. Check thresholds or data.")
        return

    # --- Analysis per Segment ---
    results = []
    
    print("\n--- Repetition Analysis ---")
    print(f"{'Rep':<5} | {'Duration':<10} | {'SPARC':<10} | {'Jerk (RMS)':<10} | {'Status'}")
    print("-" * 65)

    for i, (start, end) in enumerate(segments):
        # Important: Use RAW speed for SPARC (preserving frequency content)
        rep_speed = raw_speed[start:end]
        
        # Calculate Duration
        duration = (end - start) / FS
        
        # Skip very short artifacts (e.g. < 0.4s)
        if duration < 0.4:
            continue
            
        # Calculate SPARC
        sparc_val, _, _ = sparc(rep_speed, FS)
        
        # Calculate Jerk RMS for this segment
        jerk_val = jerk_metric(rep_speed, FS)
        
        interpretation = interpret_sparc(sparc_val)
        
        results.append({
            "Rep Loop": i + 1,
            "Start Index": start,
            "End Index": end,
            "Duration (s)": round(duration, 2),
            "SPARC": round(sparc_val, 4),
            "Jerk RMS": round(jerk_val, 2),
            "Interpretation": interpretation
        })
        
        print(f"{i+1:<5} | {duration:<10.2f} | {sparc_val:<10.4f} | {jerk_val:<10.2f} | {interpretation}")

    # Convert results to DataFrame for easy export/handling
    results_df = pd.DataFrame(results)
    
    # --- Visualization ---
    plt.figure(figsize=(14, 8))
    
    # Time axis
    time = np.arange(len(raw_speed)) / FS
    
    # 1. Plot Speed Profile
    plt.plot(time, raw_speed, color='lightgray', label='Raw Angular Speed', alpha=0.8)
    plt.plot(time, speed_smooth, color='steelblue', label='Smoothed (Detection)', linewidth=1.5)
    
    # 2. Highlight Segments
    # Use distinct colors or alternating shades
    for idx, row in results_df.iterrows():
        s, e = int(row['Start Index']), int(row['End Index'])
        mid_t = (time[s] + time[e]) / 2
        
        # Highlight area
        plt.axvspan(time[s], time[e], color='green', alpha=0.1)
        
        # Mark the Rep Number and SPARC value
        # Position text slightly above the peak of that segment
        segment_peak = np.max(speed_smooth[s:e])
        plt.text(mid_t, segment_peak + 0.2, 
                 f"R{int(row['Rep Loop'])}\n{row['SPARC']}", 
                 ha='center', va='bottom', fontsize=8, color='darkgreen', fontweight='bold')

    plt.title(f"Movement Segmentation & SPARC Analysis\n(Total Reps: {len(results_df)})")
    plt.xlabel("Time (seconds)")
    plt.ylabel("Angular Speed (rad/s)")
    plt.legend(loc='upper right')
    plt.grid(True, alpha=0.3)
    
    # Save plot
    save_path = os.path.join(SCRIPT_DIR, "SPARC_analysis.png")
    plt.savefig(save_path, dpi=150, bbox_inches='tight')
    print(f"\nPlot saved to: {save_path}")
    
    # Show plot
    plt.tight_layout()
    plt.show()

if __name__ == "__main__":
    main()
