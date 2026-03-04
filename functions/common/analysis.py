import numpy as np
from scipy.signal import find_peaks, butter, filtfilt

def angular_speed(gyro_x, gyro_y, gyro_z):
    """Calculates the resultant angular speed."""
    return np.sqrt(gyro_x**2 + gyro_y**2 + gyro_z**2)

def butter_lowpass_filter(data, cutoff, fs, order=4):
    """Applies a Butterworth low-pass filter."""
    nyq = 0.5 * fs
    normal_cutoff = cutoff / nyq
    b, a = butter(order, normal_cutoff, btype='low', analog=False)
    y = filtfilt(b, a, data)
    return y

def segment_movements(speed, fs, height_factor=0.2, min_distance_sec=1.0):
    """
    Segments the speed profile into individual repetitions based on peaks.
    Returns a list of (start, end) index tuples for each segment.
    """
    speed_smooth = butter_lowpass_filter(speed, cutoff=3.0, fs=fs)
    max_speed = np.max(speed_smooth) if len(speed_smooth) > 0 else 0
    if max_speed == 0:
        return [], speed_smooth, []

    min_height = max_speed * height_factor
    distance = int(min_distance_sec * fs)
    
    peaks, _ = find_peaks(speed_smooth, height=min_height, distance=distance)
    
    segments = []
    for i in range(len(peaks)):
        # Find start of the movement (e.g., 10% of peak height before the peak)
        start_threshold = speed_smooth[peaks[i]] * 0.1
        start_candidates = np.where(speed_smooth[:peaks[i]] < start_threshold)[0]
        start = start_candidates[-1] if len(start_candidates) > 0 else (peaks[i-1] if i > 0 else 0)

        # Find end of the movement (e.g., 10% of peak height after the peak)
        end_threshold = speed_smooth[peaks[i]] * 0.1
        end_candidates = np.where(speed_smooth[peaks[i]:] < end_threshold)[0]
        end = peaks[i] + end_candidates[0] if len(end_candidates) > 0 else (peaks[i+1] if i < len(peaks)-1 else len(speed_smooth)-1)
        
        # Refine start: look for the last minimum before the peak
        if i > 0:
            search_area_start = peaks[i-1]
        else:
            search_area_start = 0
        
        valleys, _ = find_peaks(-speed_smooth[search_area_start:peaks[i]])
        if len(valleys) > 0:
            start = search_area_start + valleys[-1]

        segments.append((start, end))
        
    return segments, speed_smooth, peaks

def prepare_signal(t, gx, gy, gz):
    """Calculates angular speed and sampling frequency."""
    speed = angular_speed(gx, gy, gz)
    
    # Calculate sampling frequency (fs)
    if len(t) > 1:
        avg_dt_sec = np.mean(np.diff(t)) / 1000.0  # Timestamps are in ms
        fs = 1.0 / avg_dt_sec if avg_dt_sec > 0 else 50.0
    else:
        fs = 50.0 # Default fs
    return speed, fs

def calculate_sparc_metric(speed, fs):
    """Calculates the SPARC (Spectral Arc Length) metric for smoothness."""
    speed_fft = np.fft.fft(speed)
    speed_fft_mag = np.abs(speed_fft)
    
    freq_range = np.fft.fftfreq(len(speed), d=1.0/fs)
    
    # Select frequencies up to a cutoff (e.g., 10Hz)
    cutoff_freq = 10.0
    valid_indices = np.where((freq_range >= 0) & (freq_range <= cutoff_freq))[0]
    
    freqs = freq_range[valid_indices]
    mags = speed_fft_mag[valid_indices]
    
    # Normalize magnitude
    mags_normalized = mags / np.max(mags) if np.max(mags) > 0 else mags
    
    # Calculate arc length
    arc_length = -np.sum(np.sqrt((np.diff(freqs))**2 + (np.diff(mags_normalized))**2))
    return arc_length

def calculate_ldlj_metric(speed, fs):
    """Calculates the LDLJ (Log Dimensionless Jerk) metric for smoothness."""
    # Jerk is the derivative of acceleration. Here we use speed's derivative.
    dt = 1.0 / fs
    jerk = np.diff(speed, 2) / (dt**2)
    
    # Dimensionless Jerk calculation
    duration = len(speed) * dt
    scale_factor = duration**3 / np.mean(speed)**2
    
    integral_jerk_squared = np.sum(jerk**2) * dt
    
    ldlj = -np.log(scale_factor * integral_jerk_squared)
    return ldlj
