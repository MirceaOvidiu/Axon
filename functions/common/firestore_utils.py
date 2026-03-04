import os
import io
import datetime
import numpy as np
from google.cloud import firestore
from google.cloud import storage
import firebase_admin

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
    
    # Check if the session is ready for processing
    # This is a placeholder for a real status field if you add one
    if data.get('status') == 'processing' or data.get('status') == 'completed':
         print(f"Session {session_id} not ready (status: {data.get('status')})")
         return None, None, None

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
