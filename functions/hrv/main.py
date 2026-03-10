import os
import io
import base64
import datetime
import numpy as np
from google.auth import compute_engine, iam
from google.auth.transport import requests as google_auth_requests
from google.oauth2 import service_account
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
from google.cloud import firestore
from google.cloud import storage
import firebase_admin
import functions_framework
from cloudevents.http import CloudEvent
from google.events.cloud import datastore

# --- Firestore & Storage Utils ---

if not firebase_admin._apps:
    firebase_admin.initialize_app()

db = firestore.Client()
storage_client = storage.Client()
bucket_name = os.environ.get('STORAGE_BUCKET', 'axon-bucket')


def get_session_hr_data(user_id, session_id):
    """Fetches session document and heart rate data from sensor_data subcollection."""
    doc_ref = db.collection('users').document(user_id).collection('sessions').document(session_id)
    doc = doc_ref.get()

    if not doc.exists:
        print(f"Error: Session document {session_id} not found.")
        return None, None, None, None

    data = doc.to_dict()

    sensor_data_ref = doc_ref.collection('sensor_data').order_by('timestamp').stream()
    sensor_data_list = [d.to_dict() for d in sensor_data_ref]

    if not sensor_data_list:
        print(f"Error: No sensor data found for session {session_id}")
        return None, None, None, None

    timestamps = np.array([d['timestamp'] for d in sensor_data_list])
    heart_rates = np.array([
        d.get('heartRate') if d.get('heartRate') and d.get('heartRate') > 0 else np.nan
        for d in sensor_data_list
    ])

    # Filter to valid HR readings only
    valid_mask = ~np.isnan(heart_rates)
    if np.sum(valid_mask) < 10:
        print(f"Error: Not enough valid heart rate samples ({np.sum(valid_mask)}) for session {session_id}")
        return doc_ref, data, None, None

    return doc_ref, data, timestamps[valid_mask], heart_rates[valid_mask]


def upload_plot(figure, destination_blob_name):
    """Uploads a matplotlib figure to Google Cloud Storage and returns a signed URL."""
    bucket = storage_client.bucket(bucket_name)
    blob = bucket.blob(destination_blob_name)

    buf = io.BytesIO()
    figure.savefig(buf, format='png', bbox_inches='tight', facecolor='#1a1a2e')
    buf.seek(0)
    blob.upload_from_file(buf, content_type='image/png')

    auth_request = google_auth_requests.Request()
    credentials = compute_engine.Credentials()
    credentials.refresh(auth_request)
    signer = iam.Signer(auth_request, credentials, credentials.service_account_email)
    signing_credentials = service_account.Credentials(
        signer=signer,
        service_account_email=credentials.service_account_email,
        token_uri="https://oauth2.googleapis.com/token",
    )
    return blob.generate_signed_url(
        version="v4",
        expiration=datetime.timedelta(days=7),
        method="GET",
        credentials=signing_credentials,
    )


# --- HRV Analysis ---

def bpm_to_rr(bpm: np.ndarray) -> np.ndarray:
    return 60000.0 / bpm


def rolling_rmssd(rr: np.ndarray, window: int = 10) -> np.ndarray:
    result = np.full(len(rr), np.nan)
    for i in range(window, len(rr) + 1):
        diffs = np.diff(rr[i - window:i])
        if len(diffs) > 0:
            result[i - 1] = float(np.sqrt(np.mean(diffs ** 2)))
    return result


def calculate_hrv_metrics(rr: np.ndarray) -> dict:
    if len(rr) < 4:
        return {}
    diffs = np.diff(rr)
    mean_rr = float(np.mean(rr))
    sdnn = float(np.std(rr, ddof=1))
    rmssd = float(np.sqrt(np.mean(diffs ** 2)))
    pnn50 = float(np.sum(np.abs(diffs) > 50) / len(diffs) * 100)
    mean_hr = float(60000.0 / mean_rr) if mean_rr > 0 else 0.0
    cv_rr = float(sdnn / mean_rr * 100) if mean_rr > 0 else 0.0
    return {
        "mean_rr": round(mean_rr, 2),
        "sdnn": round(sdnn, 2),
        "rmssd": round(rmssd, 2),
        "pnn50": round(pnn50, 2),
        "mean_hr": round(mean_hr, 1),
        "cv_rr": round(cv_rr, 2),
    }


def process_hrv_logic(timestamps_ms: np.ndarray, bpm: np.ndarray):
    """Core HRV analysis logic."""
    rr = bpm_to_rr(bpm)
    metrics = calculate_hrv_metrics(rr)
    if not metrics:
        return None, None

    roll = rolling_rmssd(rr, window=10)
    time_s = (timestamps_ms - timestamps_ms[0]) / 1000.0
    STRESS_THRESHOLD = 15.0

    plt.style.use('dark_background')
    fig = plt.figure(figsize=(12, 8))
    fig.patch.set_facecolor('#1a1a2e')
    gs = gridspec.GridSpec(2, 1, hspace=0.4)

    ax1 = fig.add_subplot(gs[0])
    ax1.set_facecolor('#16213e')
    ax1.plot(time_s, bpm, color='#FF5252', linewidth=1.2)
    ax1.axhline(metrics['mean_hr'], color='white', linestyle='--', linewidth=0.8,
                label=f"Mean: {metrics['mean_hr']:.1f} BPM")
    ax1.set_ylabel("BPM", color='white')
    ax1.set_title(f"Heart Rate  (mean: {metrics['mean_hr']:.1f} BPM)", color='white')
    ax1.legend(fontsize=8)
    ax1.grid(True, linestyle='--', alpha=0.3)
    ax1.tick_params(colors='white')

    ax2 = fig.add_subplot(gs[1])
    ax2.set_facecolor('#16213e')
    ax2.plot(time_s, roll, color='#66BB6A', linewidth=1.4,
             label=f"Rolling RMSSD (10-beat)")
    ax2.axhline(STRESS_THRESHOLD, color='#FF7043', linestyle='--', linewidth=1.0,
                label=f"Stress threshold ({STRESS_THRESHOLD} ms)")
    ax2.fill_between(time_s, roll, STRESS_THRESHOLD,
                     where=(roll < STRESS_THRESHOLD), color='#FF7043', alpha=0.15)
    ax2.set_ylabel("RMSSD (ms)", color='white')
    ax2.set_xlabel("Time (s)", color='white')
    ax2.set_title(
        f"HRV (RMSSD: {metrics['rmssd']:.1f} ms   SDNN: {metrics['sdnn']:.1f} ms   pNN50: {metrics['pnn50']:.1f}%)",
        color='white'
    )
    ax2.legend(fontsize=8)
    ax2.grid(True, linestyle='--', alpha=0.3)
    ax2.tick_params(colors='white')

    return metrics, fig


# --- Cloud Event Handler ---

@functions_framework.cloud_event
def process_hrv(cloud_event: CloudEvent) -> None:
    """
    Triggers on Firestore document update.
    Processes heart rate data to calculate HRV metrics when status changes to 'upload_completed'.
    """
    try:
        raw_data = cloud_event.data
        if isinstance(raw_data, dict):
            raw_data = base64.b64decode(raw_data["message"]["data"])

        datastore_payload = datastore.EntityEventData()
        datastore_payload._pb.ParseFromString(raw_data)

        if not datastore_payload.value:
            print("No data in Datastore event.")
            return

        old_entity = datastore_payload.old_value.entity if datastore_payload.old_value else None
        new_entity = datastore_payload.value.entity if datastore_payload.value else None

        old_status_prop = old_entity.properties.get("status") if old_entity and old_entity.properties else None
        old_status = old_status_prop.string_value if old_status_prop is not None else None
        new_status_prop = new_entity.properties.get("status") if new_entity and new_entity.properties else None
        new_status = new_status_prop.string_value if new_status_prop is not None else None

        if not (new_status == 'upload_completed' and old_status != 'upload_completed'):
            print(f"Skipping HRV: status update from '{old_status}' to '{new_status}'.")
            return

        key_path = new_entity.key.path
        if len(key_path) < 2:
            print(f"Invalid key path: {key_path}")
            return

        user_id = key_path[0].name
        session_id = key_path[1].name
        print(f"Processing HRV for user: {user_id}, session: {session_id}")

        doc_ref, data, timestamps, bpm = get_session_hr_data(user_id, session_id)
        if not doc_ref:
            print(f"Session data not found for {session_id}")
            return

        if timestamps is None:
            print(f"HRV: No valid heart rate data for {session_id}.")
            return

        if data.get('hrv_results'):
            print(f"HRV: Already processed for {session_id}.")
            return

        metrics, fig = process_hrv_logic(timestamps, bpm)

        if not metrics:
            print(f"HRV: Could not compute metrics for {session_id}.")
            return

        update_data = {
            'hrv_results': metrics,
            'hrvScore': metrics['rmssd'],
            'hrv_sdnn': metrics['sdnn'],
            'hrv_mean_hr': metrics['mean_hr'],
            'hrv_pnn50': metrics['pnn50'],
            'hrv_processed_at': firestore.SERVER_TIMESTAMP,
        }

        if fig:
            plot_url = upload_plot(fig, f"hrv/{session_id}_hrv.png")
            update_data['hrv_plot_url'] = plot_url
            plt.close(fig)

        doc_ref.update(update_data)
        print(f"Successfully processed HRV for session: {session_id} — RMSSD={metrics['rmssd']:.1f} ms")

    except Exception as e:
        print(f"Error processing HRV: {e}")
        try:
            if 'datastore_payload' in locals() and datastore_payload.value and datastore_payload.value.entity:
                key_path = datastore_payload.value.entity.key.path
                if len(key_path) >= 2:
                    user_id = key_path[0].name
                    session_id = key_path[1].name
                    db.collection("users").document(user_id).collection("sessions").document(session_id) \
                        .update({"hrvProcessingError": str(e)})
        except Exception as update_e:
            print(f"Failed to update session with HRV error status: {update_e}")
