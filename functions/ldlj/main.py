import os
import io
import datetime
import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import find_peaks, butter, filtfilt
from google.cloud import firestore
from google.cloud import storage
import firebase_admin
import functions_framework
from cloudevents.http import CloudEvent
from google.events.cloud import datastore
from google.cloud.datastore.entity import Entity

# --- Firestore & Storage Utils ---

# Initialize Firebase Admin SDK
if not firebase_admin._apps:
    firebase_admin.initialize_app()

db = firestore.Client()
storage_client = storage.Client()
bucket_name = os.environ.get('STORAGE_BUCKET', 'axon-bucket') 

def get_session_data(uid, session_id):
    """Fetches session document and sensor data subcollection."""
    doc_ref = db.collection('users').document(uid).collection('sessions').document(session_id)
    doc = doc_ref.get()

    if not doc.exists:
        print(f"Error: Session document {session_id} not found.")
        return None, None, None

    data = doc.to_dict()
    
    sensor_data_ref = doc_ref.collection('sensor_data').order_by('timestamp').stream()
    sensor_data_list = [d.to_dict() for d in sensor_data_ref]

    if not sensor_data_list:
        print(f"Error: No sensor data found for session {session_id}")
        return None, None, None

    t = np.array([d['timestamp'] for d in sensor_data_list])
    gx = np.array([d['gyro_x'] for d in sensor_data_list])
    gy = np.array([d['gyro_y'] for d in sensor_data_list])
    gz = np.array([d['gyro_z'] for d in sensor_data_list])

    return doc_ref, data, (t, gx, gy, gz)

def upload_plot(figure, destination_blob_name):
    """Uploads a matplotlib figure to Google Cloud Storage and returns public URL."""
    bucket = storage_client.bucket(bucket_name)
    blob = bucket.blob(destination_blob_name)

    # Save the plot to a BytesIO object
    buf = io.BytesIO()
    figure.savefig(buf, format='png', bbox_inches='tight')
    buf.seek(0)

    # Upload the BytesIO object
    blob.upload_from_file(buf, content_type='image/png')
    
    # Make the blob publicly viewable
    blob.make_public()

    # Return the public URL
    return blob.public_url

# --- Analysis Utils ---

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

def calculate_ldlj_metric(speed, fs):
    """
    Calculate the Log Dimensionless Jerk (LDLJ) for a speed profile,
    corrected for amplitude and duration (LDLJ-A).
    """
    if len(speed) < 3:
        return -999.0
        
    # 1. Calculate Duration (D)
    N = len(speed)
    D = N / fs
    
    # 2. Calculate Peak Speed (A) for normalization
    A = np.max(speed)
    if A == 0: 
        return -999.0
    
    # 3. Calculate Jerk
    # Acceleration = 1st derivative of speed
    accel = np.gradient(speed, 1.0/fs)
    # Jerk = derivative of acceleration
    jerk = np.gradient(accel, 1.0/fs)
    
    # 4. Integrate Squared Jerk
    # Using trapezoidal rule for integration
    if hasattr(np, 'trapezoid'):
        integrated_squared_jerk = np.trapezoid(jerk**2, dx=1.0/fs)
    else:
        integrated_squared_jerk = np.trapz(jerk**2, dx=1.0/fs)
    
    # 5. Calculate Dimensionless Jerk (DJ)
    dimensionless_jerk = integrated_squared_jerk * (D**3) / (A**2)
    
    # 6. Log Transform (LDLJ)
    if dimensionless_jerk <= 0:
        return -999.0
        
    ldlj = -np.log(dimensionless_jerk)
    
    return ldlj

# --- Main Logic ---

def process_ldlj_logic(speed, fs):
    """Core logic for LDLJ analysis, separated for local testing."""
    segments, speed_smooth, _ = segment_movements(speed, fs)
    print(f"LDLJ: Found {len(segments)} segments")

    if not segments:
        return [], speed_smooth, []

    results = []
    fig, ax = plt.subplots(figsize=(10, 6))
    time_ax = np.arange(len(speed)) / fs
    ax.plot(time_ax, speed, color='lightgray', label='Raw Speed')
    ax.plot(time_ax, speed_smooth, color='blue', label='Smoothed', linewidth=1.5)
    
    for i, (start, end) in enumerate(segments):
        rep_speed = speed[start:end]
        duration = (end - start) / fs
        if duration < 0.4: continue
        
        val = calculate_ldlj_metric(rep_speed, fs)
        results.append({
            "rep": i + 1, "duration": float(duration), "score": float(val)
        })
        
        ax.axvspan(time_ax[start], time_ax[end], color='blue', alpha=0.1)
        ax.text((time_ax[start] + time_ax[end]) / 2, np.max(speed_smooth[start:end]), 
                f"R{i+1}\n{val:.2f}", ha='center', va='bottom', fontsize=8, color='darkblue')

    ax.set_title(f"LDLJ Analysis (Reps: {len(results)})")
    ax.set_xlabel("Time (s)")
    ax.set_ylabel("Angular Speed (rad/s)")
    ax.legend(loc='upper right')
    ax.grid(True, linestyle='--', alpha=0.6)
    
    return results, fig

@functions_framework.cloud_event
def process_ldlj(cloud_event: CloudEvent) -> None:
    """
    Triggers on Firestore document update.
    Processes sensor data to calculate LDLJ and saves results only when status changes to 'completed'.
    """
    try:
        # The data is a protobuf message, so we need to parse it.
        datastore_payload = datastore.EntityEventData()
        datastore_payload._pb.ParseFromString(cloud_event.data)

        if not datastore_payload.value:
            print("No data in Datastore event.")
            return

        # Check if it's an important update
        old_value = datastore_payload.old_value
        new_value = datastore_payload.value

        old_status = old_value.properties.get("status", {}).string_value if old_value else None
        new_status = new_value.properties.get("status", {}).string_value if new_value else None
        
        # Only process if status changes to 'upload_completed'
        if not (new_status == 'upload_completed' and old_status != 'upload_completed'):
            print(f"Skipping processing for status update from '{old_status}' to '{new_status}'.")
            return

        # Extract user and session IDs from the document path
        key_path = new_value.key.path
        if len(key_path) < 4:
            print(f"Invalid key path: {key_path}")
            return
            
        user_id = key_path[1].name_or_id
        session_id = key_path[3].name_or_id

        print(f"Processing LDLJ for user: {user_id}, session: {session_id}")

        doc_ref, data, sensor_arrays = get_session_data(user_id, session_id)
        if not doc_ref: 
            print(f"Session data not found for {session_id}")
            return
        
        if data.get('ldlj_results'):
            print(f"LDLJ: Already processed for {session_id}.")
            return

        t, gx, gy, gz = sensor_arrays
        speed, fs = prepare_signal(t, gx, gy, gz)
        
        results, fig = process_ldlj_logic(speed, fs)

        update_data = {}
        if fig:
            plot_url = upload_plot(fig, f"ldlj/{session_id}_ldlj.png")
            update_data['ldlj_plot_url'] = plot_url
            
        if results:
            avg_ldlj_score = np.mean([r['score'] for r in results])
            update_data['ldljScore'] = avg_ldlj_score
            
        update_data.update({
            'ldlj_results': results,
            'ldlj_processed_at': firestore.SERVER_TIMESTAMP
        })
        doc_ref.update(update_data)
        
        print(f"Successfully processed LDLJ for session: {session_id}")

    except Exception as e:
        print(f"Error processing LDLJ: {e}")
        # Optionally, update Firestore with error status
        try:
            if 'datastore_payload' in locals() and datastore_payload.value:
                key_path = datastore_payload.value.key.path
                if len(key_path) >= 4:
                    user_id = key_path[1].name_or_id
                    session_id = key_path[3].name_or_id
                    session_ref = db.collection("users").document(user_id).collection("sessions").document(session_id)
                    session_ref.update({"ldljProcessingError": str(e)})
        except Exception as update_e:
            print(f"Failed to update session with error status: {update_e}")

