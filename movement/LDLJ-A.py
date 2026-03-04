import os
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.signal import butter, filtfilt, find_peaks

# ---------------------------------------------------------------------------
# 1. LDLJ-A Core Functions
# ---------------------------------------------------------------------------

def calculate_ldlj_a(speed: np.ndarray, fs: float) -> float:
    """
    Calculate the Log Dimensionless Jerk (LDLJ) for a speed profile,
    corrected for amplitude and duration (LDLJ-A).
    
    Formula based on Balasubramanian et al. (2015) and Hogan (1984):
    LDLJ ≈ -ln( ∫ |jerk|^2 dt * (D^5 / A^2) )
    
    Where:
      - jerk is the 2nd derivative of speed (or 3rd of position)
      - D is the duration of the movement
      - A is the peak speed (or path length, depending on specific definition)
        * For discrete movements, Peak Speed is often used as the scaling factor 'A'
          to make it dimensionless.
    
    Returns:
        float: The LDLJ-A score.
               Values closer to 0 (e.g., -6) indicate smoother movement.
               Values more negative (e.g., -10, -20) indicate jerkier movement.
    """
    if len(speed) < 3:
        return -999.0
        
    # 1. Calculate Duration (D)
    N = len(speed)
    D = N / fs
    
    # 2. Calculate Peak Speed (A) for normalization
    # Note: Some versions use Path Length. For speed profiles, Peak Speed is robust.
    A = np.max(speed)
    if A == 0: 
        return -999.0
    
    # 3. Calculate Jerk
    # Acceleration = 1st derivative of speed
    accel = np.gradient(speed, 1.0/fs)
    # Jerk = derivative of acceleration
    jerk = np.gradient(accel, 1.0/fs)
    
    # 4. Integrate Squared Jerk
    # ∫ |jerk(t)|^2 dt
    # Using trapezoidal rule for integration (NumPy 2.0+ compatible)
    if hasattr(np, 'trapezoid'):
        integrated_squared_jerk = np.trapezoid(jerk**2, dx=1.0/fs)
    else:
        integrated_squared_jerk = np.trapz(jerk**2, dx=1.0/fs)
    
    # 5. Calculate Dimensionless Jerk (DJ)
    # The scaling factor standardizes for time and distance so you can compare 
    # fast/short moves with slow/long ones.
    # Scale factor = D^3 / v_peak^2 (standard for speed-based) or D^5 / Length^2 (pos-based)
    # Let's use the standard speed-profile formulation: DLJ = ∫ J^2 dt * (D^3 / V_peak^2)
    
    dimensionless_jerk = integrated_squared_jerk * (D**3) / (A**2)
    
    # 6. Log Transform (LDLJ)
    # We take negative log so 'higher' (less negative) is smoother, matching SPARC intuition
    # Note: The original metric is often just ln(...).
    # If using -ln, then:
    #   Smoother -> Smaller DJ -> Less Negative Log
    #   Jerkier  -> Larger DJ  -> More Negative Log
    
    if dimensionless_jerk <= 0:
        return -999.0
        
    ldlj = -np.log(dimensionless_jerk)
    
    return ldlj

def interpret_ldlj(value: float) -> str:
    """
    Qualitative interpretation of LDLJ values.
    Note: These thresholds are heuristic and task-dependent.
    Ref: Minimum jerk (perfect bell curve) has a specific theoretical value (~ -6).
    """
    if value > -6.0:
        return "Exceptionally Smooth (Super-human?)"
    elif value > -7.5:
        return "Very Smooth"
    elif value > -9.0:
        return "Moderately Smooth"
    elif value > -11.0:
        return "Somewhat Jerky"
    else:
        return "Very Jerky / Fragmented"

def angular_speed(gyro_x: np.ndarray, gyro_y: np.ndarray, gyro_z: np.ndarray) -> np.ndarray:
    return np.sqrt(gyro_x ** 2 + gyro_y ** 2 + gyro_z ** 2)

# ---------------------------------------------------------------------------
# 2. Segmentation Logic (Identical to SPARC script)
# ---------------------------------------------------------------------------

def butter_lowpass_filter(data, cutoff, fs, order=4):
    nyq = 0.5 * fs
    normal_cutoff = cutoff / nyq
    b, a = butter(order, normal_cutoff, btype='low', analog=False)
    y = filtfilt(b, a, data)
    return y

def segment_movements(speed, fs, height_factor=0.2, min_distance_sec=1.0):
    speed_smooth = butter_lowpass_filter(speed, cutoff=3.0, fs=fs)
    max_speed = np.max(speed_smooth) if len(speed_smooth) > 0 else 0
    if max_speed == 0: return [], speed_smooth, []

    min_height = max_speed * height_factor
    distance = int(min_distance_sec * fs)
    peaks, _ = find_peaks(speed_smooth, height=min_height, distance=distance)
    segments = []
    
    for peak_idx in peaks:
        peak_val = speed_smooth[peak_idx]
        threshold = peak_val * 0.10
        
        start_idx = peak_idx
        while start_idx > 0 and speed_smooth[start_idx] > threshold:
            start_idx -= 1
            
        end_idx = peak_idx
        while end_idx < len(speed_smooth) - 1 and speed_smooth[end_idx] > threshold:
            end_idx += 1
            
        start_idx = max(0, start_idx - 5)
        end_idx = min(len(speed) - 1, end_idx + 5)
        
        if segments:
            prev_start, prev_end = segments[-1]
            if start_idx < prev_end:
                mid_point = (prev_end + start_idx) // 2
                segments[-1] = (prev_start, mid_point)
                start_idx = mid_point
        
        segments.append((start_idx, end_idx))
    return segments, speed_smooth, peaks

# ---------------------------------------------------------------------------
# 3. Main Analysis
# ---------------------------------------------------------------------------

def main():
    FS = 25.0
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    CSV_PATH = os.path.join(SCRIPT_DIR, "sessionData.csv")
    
    if not os.path.exists(CSV_PATH):
        print(f"Error: {CSV_PATH} not found.")
        return

    try:
        df = pd.read_csv(CSV_PATH)
        print(f"Loaded {len(df)} samples from {CSV_PATH}")
    except Exception as e:
        print(f"Error reading CSV: {e}")
        return
    
    raw_speed = angular_speed(df["gyro_x"].values, df["gyro_y"].values, df["gyro_z"].values)
    
    print(f"Segmenting movements...")
    segments, speed_smooth, peaks = segment_movements(raw_speed, FS)
    print(f"Found {len(segments)} potential repetitions.")
    
    if len(segments) == 0:
        print("No movements detected.")
        return

    results = []
    
    print("\n--- Repetition Analysis (LDLJ-A) ---")
    print(f"{'Rep':<5} | {'Duration':<10} | {'LDLJ-A':<10} | {'Status'}")
    print("-" * 50)

    for i, (start, end) in enumerate(segments):
        rep_speed = raw_speed[start:end]
        duration = (end - start) / FS
        
        if duration < 0.4: continue
            
        # --- LDLJ Calculation ---
        ldlj_val = calculate_ldlj_a(rep_speed, FS)
        interpretation = interpret_ldlj(ldlj_val)
        
        results.append({
            "Rep Loop": i + 1,
            "Start Index": start,
            "End Index": end,
            "Duration (s)": round(duration, 2),
            "LDLJ-A": round(ldlj_val, 4),
            "Interpretation": interpretation
        })
        
        print(f"{i+1:<5} | {duration:<10.2f} | {ldlj_val:<10.4f} | {interpretation}")

    results_df = pd.DataFrame(results)
    
    # --- Plotting ---
    plt.figure(figsize=(14, 8))
    time = np.arange(len(raw_speed)) / FS
    plt.plot(time, raw_speed, color='lightgray', label='Raw Angular Speed', alpha=0.8)
    plt.plot(time, speed_smooth, color='steelblue', label='Smoothed (Detection)', linewidth=1.5)
    
    for idx, row in results_df.iterrows():
        s, e = int(row['Start Index']), int(row['End Index'])
        mid_t = (time[s] + time[e]) / 2
        
        plt.axvspan(time[s], time[e], color='purple', alpha=0.1)
        
        segment_peak = np.max(speed_smooth[s:e])
        plt.text(mid_t, segment_peak + 0.2, 
                 f"R{int(row['Rep Loop'])}\n{row['LDLJ-A']}", 
                 ha='center', va='bottom', fontsize=8, color='purple', fontweight='bold')

    plt.title(f"Movement Segmentation & LDLJ-Analysis\n(Total Reps: {len(results_df)})")
    plt.xlabel("Time (seconds)")
    plt.ylabel("Angular Speed (rad/s)")
    plt.legend(loc='upper right')
    plt.grid(True, alpha=0.3)
    
    save_path = os.path.join(SCRIPT_DIR, "LDLJ-A_analysis.png")
    plt.savefig(save_path, dpi=150, bbox_inches='tight')
    print(f"\nPlot saved to: {save_path}")
    plt.show()

if __name__ == "__main__":
    main()
