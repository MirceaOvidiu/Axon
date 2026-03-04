import os
import io
import datetime
import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import find_peaks, butter, filtfilt
from google.cloud import firestore
from google.cloud import storage
import firebase_admin
from firebase_admin import credentials

# Initialize Firebase Admin SDK
if not firebase_admin._apps:
    firebase_admin.initialize_app()

db = firestore.Client()
storage_client = storage.Client()
# Use the specific bucket provided by the user
bucket_name = os.environ.get('STORAGE_BUCKET', 'axon-bucket') 

# ---------------------------------------------------------------------------
# Shared Helper Functions
# ---------------------------------------------------------------------------

def angular_speed(gyro_x, gyro_y, gyro_z):
    return np.sqrt(gyro_x**2 + gyro_y**2 + gyro_z**2)

def butter_lowpass_filter(data, cutoff, fs, order=4):
    nyq = 0.5 * fs
    normal_cutoff = cutoff / nyq
    b, a = butter(order, normal_cutoff, btype='low', analog=False)
    y = filtfilt(b, a, data)
    return y

def segment_movements(speed, fs, height_factor=0.2, min_distance_sec=1.0):
    """
    Segment the speed profile into individual repetitions.
    Returns: segments (list of tuples), speed_smooth, peaks
    """
    speed_smooth = butter_lowpass_filter(speed, cutoff=3.0, fs=fs)
    max_speed = np.max(speed_smooth) if len(speed_smooth) > 0 else 0
    if max_speed == 0:
        return [], speed_smooth, []

    min_height = max_speed * height_factor
    distance = int(min_distance_sec * fs)
    
    peaks, _ = find_peaks(speed_smooth, height=min_height, distance=distance)
    
    segments = []
    
    for peak_idx in peaks:
        peak_val = speed_smooth[peak_idx]
        threshold = peak_val * 0.10
        
        # Search backwards
        start_idx = peak_idx
        while start_idx > 0 and speed_smooth[start_idx] > threshold:
            start_idx -= 1
            
        # Search forwards
        end_idx = peak_idx
        while end_idx < len(speed_smooth) - 1 and speed_smooth[end_idx] > threshold:
            end_idx += 1
            
        # Buffers
        start_idx = max(0, start_idx - 5)
        end_idx = min(len(speed) - 1, end_idx + 5)
        
        # Overlap handling
        if segments:
            prev_start, prev_end = segments[-1]
            if start_idx < prev_end:
                mid_point = (prev_end + start_idx) // 2
                segments[-1] = (prev_start, mid_point)
                start_idx = mid_point
        
        segments.append((start_idx, end_idx))
        
    return segments, speed_smooth, peaks

def upload_plot(fig, filename):
    """Save matplotlib figure to buffer and upload to Firebase Storage"""
    buf = io.BytesIO()
    fig.savefig(buf, format='png', dpi=100, bbox_inches='tight')
    buf.seek(0)
    plt.close(fig)
    
    bucket = storage_client.bucket(bucket_name)
    blob = bucket.blob(f"plots/{filename}")
    blob.upload_from_file(buf, content_type='image/png')
    
    # Generate signed URL (valid for 7 days)
    url = blob.generate_signed_url(expiration=datetime.timedelta(days=7), method='GET')
    return url

# ---------------------------------------------------------------------------
# SPARC Specifics
# ---------------------------------------------------------------------------

def calculate_sparc_metric(speed, fs, padlevel=4, fc=10.0, amp_th=0.05):
    if len(speed) == 0: return -999.0
    n = len(speed)
    nfft = int(2 ** (np.ceil(np.log2(n)) + padlevel))
    freq_full = np.fft.rfftfreq(nfft, d=1.0 / fs)
    spec = np.abs(np.fft.rfft(speed, n=nfft))
    spec_norm = spec / spec.max() if spec.max() > 0 else spec

    above_th = np.where((freq_full <= fc) & (spec_norm >= amp_th))[0]
    fc_idx = above_th[-1] + 1 if len(above_th) > 0 else 1

    freq = freq_full[:fc_idx]
    magnitude = spec_norm[:fc_idx]

    if len(freq) < 2: return -999.0

    d_freq = np.diff(freq) / (freq[-1] - freq[0]) if (freq[-1] - freq[0]) > 0 else np.diff(freq)
    d_mag = np.diff(magnitude)
    arc_lengths = np.sqrt(d_freq ** 2 + d_mag ** 2)
    return -float(np.sum(arc_lengths))

# ---------------------------------------------------------------------------
# LDLJ Specifics
# ---------------------------------------------------------------------------

def calculate_ldlj_metric(speed, fs):
    if len(speed) < 3: return -999.0
    N = len(speed)
    D = N / fs
    A = np.max(speed)
    if A == 0: return -999.0
    
    accel = np.gradient(speed, 1.0/fs)
    jerk = np.gradient(accel, 1.0/fs)
    
    if hasattr(np, 'trapezoid'):
        integrated_squared_jerk = np.trapezoid(jerk**2, dx=1.0/fs)
    else:
        integrated_squared_jerk = np.trapz(jerk**2, dx=1.0/fs)
    
    dimensionless_jerk = integrated_squared_jerk * (D**3) / (A**2)
    if dimensionless_jerk <= 0: return -999.0
    
    return -np.log(dimensionless_jerk)

# ---------------------------------------------------------------------------
# Entry Points
# ---------------------------------------------------------------------------

def get_session_data(uid, session_id):
    doc_ref = db.collection('users').document(uid).collection('sessions').document(session_id)
    doc = doc_ref.get()
    if not doc.exists: return None, None, None
    
    data = doc.to_dict()
    if data.get('status') != 'upload_completed':
        print(f"Session {session_id} not ready (status: {data.get('status')})")
        return None, None, None

    sensor_ref = doc_ref.collection('sensor_data')
    docs = sensor_ref.order_by('timestamp').stream()
    
    timestamps, gx, gy, gz = [], [], [], []
    for d in docs:
        sd = d.to_dict()
        timestamps.append(sd.get('timestamp', 0))
        gx.append(sd.get('gyroX', 0.0))
        gy.append(sd.get('gyroY', 0.0))
        gz.append(sd.get('gyroZ', 0.0))
        
    if not timestamps: return None, None, None
    
    return doc_ref, data, (np.array(timestamps), np.array(gx), np.array(gy), np.array(gz))

def prepare_signal(timestamps, gx, gy, gz):
    speed = angular_speed(gx, gy, gz)
    if len(timestamps) > 1:
        dt = np.diff(timestamps)
        avg_dt_sec = np.mean(dt) / 1000.0
        fs = 1.0 / avg_dt_sec if avg_dt_sec > 0 else 50.0
    else:
        fs = 50.0
    return speed, fs

def process_sparc(event, context):
    """Cloud Function Entry Point for SPARC"""
    path_parts = context.resource.split('/documents/')[1].split('/')
    uid, session_id = path_parts[1], path_parts[3]

    doc_ref, data, sensor_arrays = get_session_data(uid, session_id)
    if not doc_ref: return
    
    # Check idempotency
    if data.get('sparc_results'):
        print(f"SPARC already processed for {session_id}")
        return

    t, gx, gy, gz = sensor_arrays
    speed, fs = prepare_signal(t, gx, gy, gz)
    
    segments, speed_smooth, _ = segment_movements(speed, fs)
    print(f"SPARC: Found {len(segments)} segments")

    results = []
    
    # Setup Plot
    fig, ax = plt.subplots(figsize=(10, 6))
    time_ax = np.arange(len(speed)) / fs
    ax.plot(time_ax, speed, color='lightgray', label='Raw Speed')
    ax.plot(time_ax, speed_smooth, color='green', label='Smoothed', linewidth=1.5)
    
    for i, (start, end) in enumerate(segments):
        rep_speed = speed[start:end]
        duration = (end - start) / fs
        if duration < 0.4: continue
        
        val = calculate_sparc_metric(rep_speed, fs)
        
        results.append({
            "rep": i + 1,
            "start_idx": int(start),
            "end_idx": int(end),
            "duration": float(duration),
            "score": float(val)
        })
        
        # Plot highlight
        ax.axvspan(time_ax[start], time_ax[end], color='green', alpha=0.1)
        ax.text((time_ax[start]+time_ax[end])/2, np.max(speed_smooth[start:end]), 
                f"R{i+1}\n{val:.2f}", ha='center', va='bottom', fontsize=8, color='darkgreen')

    ax.set_title(f"SPARC Analysis (Reps: {len(results)})")
    ax.legend(loc='upper right')
    
    # Upload Plot
    plot_url = upload_plot(fig, f"{session_id}_sparc.png")
    
    # Update Firestore
    doc_ref.update({
        'sparc_results': results,
        'sparc_plot_url': plot_url,
        'sparc_processed_at': firestore.SERVER_TIMESTAMP
    })
    print(f"SPARC completed for {session_id}")

def process_ldlj(event, context):
    """Cloud Function Entry Point for LDLJ"""
    path_parts = context.resource.split('/documents/')[1].split('/')
    uid, session_id = path_parts[1], path_parts[3]

    doc_ref, data, sensor_arrays = get_session_data(uid, session_id)
    if not doc_ref: return
    
    if data.get('ldlj_results'):
        print(f"LDLJ already processed for {session_id}")
        return

    t, gx, gy, gz = sensor_arrays
    speed, fs = prepare_signal(t, gx, gy, gz)
    
    segments, speed_smooth, _ = segment_movements(speed, fs)
    print(f"LDLJ: Found {len(segments)} segments")

    results = []
    
    # Setup Plot
    fig, ax = plt.subplots(figsize=(10, 6))
    time_ax = np.arange(len(speed)) / fs
    ax.plot(time_ax, speed, color='lightgray', label='Raw Speed')
    ax.plot(time_ax, speed_smooth, color='purple', label='Smoothed', linewidth=1.5)
    
    for i, (start, end) in enumerate(segments):
        rep_speed = speed[start:end]
        duration = (end - start) / fs
        if duration < 0.4: continue
        
        val = calculate_ldlj_metric(rep_speed, fs)
        
        results.append({
            "rep": i + 1,
            "start_idx": int(start),
            "end_idx": int(end),
            "duration": float(duration),
            "score": float(val)
        })
        
        # Plot highlight
        ax.axvspan(time_ax[start], time_ax[end], color='purple', alpha=0.1)
        ax.text((time_ax[start]+time_ax[end])/2, np.max(speed_smooth[start:end]), 
                f"R{i+1}\n{val:.2f}", ha='center', va='bottom', fontsize=8, color='purple')

    ax.set_title(f"LDLJ-A Analysis (Reps: {len(results)})")
    ax.legend(loc='upper right')
    
    # Upload Plot
    plot_url = upload_plot(fig, f"{session_id}_ldlj.png")
    
    # Update Firestore
    doc_ref.update({
        'ldlj_results': results,
        'ldlj_plot_url': plot_url,
        'ldlj_processed_at': firestore.SERVER_TIMESTAMP
    })
    print(f"LDLJ completed for {session_id}")
