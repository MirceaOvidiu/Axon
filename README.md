# Axon

**Wearable-assisted physiotherapy monitoring system** that captures gyroscope and heart rate data during rehabilitation exercises on a Wear OS smartwatch, transfers it to an Android companion app, uploads it to Google Cloud, and runs signal-processing analyses (SPARC, LDLJ-A, HRV) via serverless Cloud Run functions. Results -- per-repetition movement quality scores and heart rate variability metrics with annotated plots -- are written back to Firestore and displayed on the phone.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [Technology Stack](#technology-stack)
5. [Prerequisites](#prerequisites)
6. [Getting Started](#getting-started)
7. [Modules](#modules)
   - [Wear OS App](#wear-os-app)
   - [Mobile App](#mobile-app)
   - [Cloud Run Functions](#cloud-run-functions)
8. [Signal Processing Algorithms](#signal-processing-algorithms)
9. [Data Flow](#data-flow)
10. [Firestore Data Schema](#firestore-data-schema)
11. [Security](#security)
12. [Deployment](#deployment)
13. [Local Testing](#local-testing)
14. [License](#license)

---

## Overview

A patient performs a structured exercise session wearing a Wear OS smartwatch. The watch records gyroscope readings at 20 ms intervals and continuous heart rate via Wear Health Services. On session completion, data is transferred to the paired Android phone, which uploads it to Firestore. Three independent Cloud Run services then execute in parallel:

- **SPARC** -- frequency-domain movement smoothness (spectral arc length)
- **LDLJ-A** -- time-domain movement smoothness (log dimensionless jerk)
- **HRV** -- autonomic recovery metrics (RMSSD, SDNN, pNN50, mean HR)

Each service generates a PNG plot, uploads it to Google Cloud Storage with a signed URL, and writes results back to the session document. The mobile app polls for results and renders them with per-repetition tables and inline plots.

---

## Architecture

```
WEAR OS WATCH                          ANDROID PHONE                         GOOGLE CLOUD
+-----------------------+              +---------------------------+          +---------------------------+
| GyroDataSource (20ms) |--+           | DataLayerListenerService  |          | Firestore (eur3)          |
| HealthServicesDSource |  |           |   onDataChanged (live)    |          |   users/{uid}/sessions/   |
+-----------------------+  |           |   onMessageReceived (bulk)|          |     sensor_data/          |
                           |           +---------------------------+          +---------------------------+
  RecordingUseCase         |                      |                                    |
  SyncUseCase  ------------+--DataClient----------+                                    |
               ------------+--MessageClient-------+                           Eventarc triggers
                                                  |                                    |
                                       CloudSyncUseCase                     +----------+----------+
                                       Firestore batch write                |          |          |
                                       status = "upload_completed" -------->| axon-    | axon-    | axon-
                                                                            | sparc    | ldlj     | hrv
                                       SessionDetailScreen   <------------- | (Python) | (Python) | (Python)
                                       (scores + Coil plots)                +----------+----------+
                                                                                       |
                                                                            +----------+----------+
                                                                            | axon-bucket (GCS)   |
                                                                            | V4 signed URLs      |
                                                                            +---------------------+
```

---

## Project Structure

```
Axon/
|-- mobile/                     Android companion app (phone)
|   |-- build.gradle.kts
|   `-- src/main/java/com/axon/
|
|-- wear/                       Wear OS smartwatch app
|   |-- build.gradle.kts
|   `-- src/main/java/com/axon/
|
|-- functions/                  Cloud Run Python services
|   |-- main.py                 Shared utilities (common analysis helpers)
|   |-- requirements.txt
|   |-- common/                 Shared analysis, Firestore utils
|   |-- sparc/                  SPARC analysis service (Dockerfile + main.py)
|   |-- ldlj/                   LDLJ-A analysis service (Dockerfile + main.py)
|   `-- hrv/                    HRV analysis service (Dockerfile + main.py)
|
|-- local-test/                 Standalone Python scripts for offline algorithm testing
|   |-- SPARC.py
|   |-- LDLJ-A.py
|   |-- HRV.py
|   `-- sessionData.csv
|
|-- gradle/
|   |-- libs.versions.toml      Version catalog (all dependency versions)
|   `-- wrapper/
|
|-- firebase.json               Firebase project configuration
|-- firestore.rules             Firestore security rules
|-- firestore.indexes.json      Firestore composite indexes
|-- build.gradle.kts            Root build script
|-- settings.gradle.kts         Gradle module declarations
`-- SYSTEM_DESIGN.md            Detailed system design document
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language (Android) | Kotlin |
| UI Framework | Jetpack Compose (mobile + Wear OS), Horologist (Tiles) |
| Dependency Injection | Hilt |
| Local Database | Room / SQLite (watch and phone) |
| Serialisation | Gson |
| Async | Kotlin Coroutines + Flow (StateFlow, SharedFlow) |
| Image Loading | Coil |
| Authentication | Firebase Auth -- Google Sign-In via `androidx.credentials` |
| Cloud Database | Cloud Firestore |
| Cloud Storage | Google Cloud Storage |
| Watch-Phone Comms | Wearable Data Layer API (DataClient, MessageClient) |
| Watch Sensors | Wear Health Services (HR), Android SensorManager (gyroscope) |
| Cloud Backend | Python 3.10, Cloud Run (Docker containers) |
| Signal Processing | NumPy, SciPy (`butter`, `filtfilt`, `find_peaks`) |
| Plotting | Matplotlib (Agg backend) |
| CF Framework | `functions-framework` (CloudEvent handler) |
| Trigger Mechanism | Eventarc (Firestore document write via Pub/Sub push) |
| Analytics Export | Firebase Extension: `firestore-bigquery-export` |
| Build System | Gradle Kotlin DSL + version catalog |
| CI/CD | Google Cloud Build |
| Min SDK | Android 30 / Wear OS 3 |
| Target SDK | Android 36 |

---

## Prerequisites

- **Android Studio** (latest stable) with Wear OS SDK components
- **JDK 11+**
- **Python 3.10+** (for local algorithm testing)
- **Docker** (for building/testing Cloud Run containers locally)
- **Firebase CLI** (`npm install -g firebase-tools`)
- **Google Cloud SDK** (`gcloud`) with project `<ID>` configured
- A Wear OS smartwatch or emulator (API 30+)
- A physical Android phone or emulator (API 30+)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/MirceaOvidiu/Axon.git
cd Axon
```

### 2. Firebase setup

Place your `google-services.json` in the `mobile/` directory (already committed for this project -- it contains only public Firebase identifiers).

### 3. Build the Android apps

Open the project in Android Studio. The Gradle sync will resolve all dependencies from the version catalog.

```bash
./gradlew :mobile:assembleDebug
./gradlew :wear:assembleDebug
```

### 4. Install on devices

Deploy the mobile app to your phone and the wear app to your smartwatch (or emulators):

```bash
./gradlew :mobile:installDebug
./gradlew :wear:installDebug
```

### 5. Deploy Cloud Run functions

Each service has its own Dockerfile. Build and deploy from the respective directory:

```bash
cd functions/sparc
gcloud builds submit --tag gcr.io/<ID>/axon-sparc
gcloud run deploy axon-sparc --image gcr.io/<ID>/axon-sparc --region europe-west1

cd ../ldlj
gcloud builds submit --tag gcr.io/<ID>/axon-ldlj
gcloud run deploy axon-ldlj --image gcr.io/<ID>/axon-ldlj --region europe-west1

cd ../hrv
gcloud builds submit --tag gcr.io/<ID>/axon-hrv
gcloud run deploy axon-hrv --image gcr.io/<ID>/axon-hrv --region europe-west1
```

### 6. Deploy Firestore rules and indexes

```bash
firebase deploy --only firestore --project <ID>
```

---

## Modules

### Wear OS App

**Module:** `wear/` | **Package:** `com.axon`

The watch app records sensor data and transfers it to the paired phone.

**Key components:**

| Component | Role |
|---|---|
| `GyroDataSource` | Registers `TYPE_GYROSCOPE` at 20 ms interval; emits `StateFlow<FloatArray>` |
| `HealthServicesDataSourceAdapter` | Wear Health Services HR listener; emits `StateFlow<Double>` |
| `RecordingUseCase` | Starts/stops sessions; persists `SensorData` to local Room DB |
| `SyncUseCase` | Transfers full session data to phone via `MessageClient` |
| `WearableDataSender` | Dual-channel: `DataClient` for live streaming, `MessageClient` for bulk JSON |
| `MainViewModel` | MVI architecture; intents: `StartRecording`, `StopRecording`, `SyncAllSessions` |
| `MainTileService` | Wear OS Tile for quick-start recording |
| `MainComplicationService` | Wear OS complication data source |

### Mobile App

**Module:** `mobile/` | **Package:** `com.axon`

The companion app receives data from the watch, manages cloud sync, and displays analysis results.

**Architecture:** MVVM + Clean Architecture + Hilt DI + Compose Navigation

**Key screens:**

| Screen | Description |
|---|---|
| `AuthScreen` | Google Sign-In via Credential Manager |
| `RecoveryDashboard` | Aggregate score summary across sessions |
| `SessionListScreen` | Lists sessions with sync status |
| `SessionDetailScreen` | Per-rep SPARC/LDLJ tables, HRV metrics, analysis plots |
| `CloudSyncScreen` | Upload/download progress with `Flow<Float>` progress |
| `WatchDataScreen` | Live sensor values streamed from watch |

**Key data components:**

| Component | Role |
|---|---|
| `DataLayerListenerService` | Receives both DataClient and MessageClient channels from watch |
| `SessionRepositoryImplementation` | Room CRUD for sessions and sensor data |
| `CloudSessionRepositoryImplementation` | Firestore upload/download with progress tracking |
| `CloudSyncUseCase` | Orchestrates: upload session, upload sensor data, set `status = upload_completed` |
| `SessionViewModel` | Polls Firestore for analysis results (2s interval, max 20s) |

### Cloud Run Functions

**Directory:** `functions/` | **Runtime:** Python 3.10

Three containerised services, each triggered by Eventarc on Firestore document writes. All three follow the same pattern:

1. Receive CloudEvent from Eventarc (document write on `users/{uid}/sessions/{sessionId}`)
2. Guard: proceed only when `status` transitions to `upload_completed`
3. Fetch `sensor_data` subcollection from Firestore
4. Run the respective signal-processing algorithm
5. Generate a PNG plot and upload to GCS (`axon-bucket`)
6. Write results and a V4 signed URL (7-day TTL) back to the session document

| Service | Image | Algorithm | Output Fields |
|---|---|---|---|
| `axon-sparc` | `gcr.io/<ID>/axon-sparc` | Spectral arc length | `sparcScore`, `sparc_results`, `sparc_plot_url` |
| `axon-ldlj` | `gcr.io/<ID>/axon-ldlj` | Log dimensionless jerk | `ldljScore`, `ldlj_results`, `ldlj_plot_url` |
| `axon-hrv` | `gcr.io/<ID>/axon-hrv` | Heart rate variability | `hrvScore`, `hrv_sdnn`, `hrv_mean_hr`, `hrv_pnn50`, `hrv_plot_url` |

---

## Signal Processing Algorithms

### SPARC (Spectral Arc Length)

Frequency-domain metric for movement smoothness. Values range approximately from -5 to 0, where values closer to 0 indicate smoother movement.

1. Compute angular speed: $\omega(t) = \sqrt{g_x^2 + g_y^2 + g_z^2}$
2. Apply 4th-order Butterworth low-pass filter (3 Hz cutoff)
3. Segment repetitions using `scipy.signal.find_peaks` with peak prominence
4. Per repetition: normalise speed, compute FFT, calculate spectral arc length up to 10 Hz cutoff
5. Output: mean SPARC across reps, per-rep scores, 2-panel plot (speed trace + bar chart)

### LDLJ-A (Log Dimensionless Jerk - Angular)

Time-domain metric for movement smoothness. Higher values (typically +8 to +12 for healthy adults) indicate smoother movement.

1. Same angular speed computation and filtering as SPARC
2. Same repetition segmentation
3. Per repetition: compute jerk (second derivative of speed), integrate squared jerk, normalise by duration and peak amplitude
4. $\text{LDLJ-A} = -\ln\left(J \cdot \frac{D^3}{A^2}\right)$
5. Output: mean LDLJ-A across reps, per-rep scores, 2-panel plot

### HRV (Heart Rate Variability)

Autonomic nervous system recovery metrics derived from heart rate data.

| Metric | Description |
|---|---|
| RMSSD | Root mean square of successive R-R differences -- primary parasympathetic recovery metric |
| SDNN | Standard deviation of R-R intervals -- overall autonomic balance |
| pNN50 | Percentage of successive R-R differences exceeding 50 ms -- vagal tone |
| Mean HR | Average heart rate in BPM |

Additional features: 10-beat rolling RMSSD window for stress period detection (threshold: RMSSD < 15 ms). Output includes a 2-panel plot with HR trace and rolling RMSSD with stress shading.

---

## Data Flow

```
1. RECORDING (Watch)
   User starts session -> gyroscope (20ms) + HR sampled continuously
   Live: DataClient streams to phone for real-time display
   On stop: MessageClient sends full session JSON

2. RECEPTION (Phone)
   DataLayerListenerService deserialises JSON
   Session + SensorData inserted into local Room database

3. UPLOAD (Phone -> Cloud)
   User triggers upload via CloudSyncScreen
   Session document written to Firestore
   Sensor data batch-written to subcollection
   Status set to "upload_completed"

4. ANALYSIS (Cloud Run)
   Eventarc fires three Cloud Run services in parallel
   Each reads sensor_data, runs algorithm, uploads plot to GCS
   Results + signed plot URL written back to session document

5. DISPLAY (Phone)
   SessionViewModel polls Firestore every 2s (max 20s)
   Scores and plot URLs synced to local Room DB
   SessionDetailScreen renders tables and plots (Coil)
```

---

## Firestore Data Schema

```
users/{uid}/
  sessions/{sessionId}/
    -- Core metadata --
    firestoreId        String
    userId             String
    startTime          Long        (epoch ms)
    endTime            Long        (epoch ms)
    dataPointCount     Int
    status             String      "pending" | "upload_completed"

    -- SPARC results (written by axon-sparc) --
    sparcScore         Double      (mean across reps, range ~ -5 to 0)
    sparc_results      Array       [{rep, duration, score}, ...]
    sparc_plot_url     String      (GCS signed URL, 7-day TTL)

    -- LDLJ results (written by axon-ldlj) --
    ldljScore          Double      (mean across reps)
    ldlj_results       Array       [{rep, duration, score}, ...]
    ldlj_plot_url      String      (GCS signed URL, 7-day TTL)

    -- HRV results (written by axon-hrv) --
    hrvScore           Double      (= RMSSD in ms)
    hrv_sdnn           Double      (ms)
    hrv_mean_hr        Double      (BPM)
    hrv_pnn50          Double      (%)
    hrv_plot_url       String      (GCS signed URL, 7-day TTL)

    sensor_data/{auto-id}/
      timestamp        Long        (epoch ms)
      gyroX            Float       (rad/s)
      gyroY            Float       (rad/s)
      gyroZ            Float       (rad/s)
      heartRate        Double      (BPM, nullable)
```

---

## Security

- **Authentication:** Firebase Auth with Google Sign-In. Firebase UID serves as the namespace key throughout Firestore.
- **Firestore rules:** Users can only read/write their own data (`request.auth.uid == userId`). Default deny on all other paths.
- **GCS access:** Uniform bucket-level IAM with public access prevention enforced. Plot images served exclusively via V4 signed URLs (7-day TTL). Android clients never have direct GCS read permission.
- **Cloud Run:** Services are not publicly invocable -- triggered only via Eventarc Pub/Sub push. Service accounts follow least-privilege (Firestore read/write, GCS write, IAM signBlob).
- **Build secrets:** Keystore passwords and signing credentials injected via CI environment variables, never committed to version control.

---

## Deployment

### Cloud Run (CI/CD)

Cloud Build triggers are configured per service, firing on push to `main`:

| Service | Source Directory | Dockerfile | Region |
|---|---|---|---|
| `axon-sparc` | `functions/sparc/` | `functions/sparc/Dockerfile` | `europe-west1` |
| `axon-ldlj` | `functions/ldlj/` | `functions/ldlj/Dockerfile` | `europe-west1` |
| `axon-hrv` | `functions/hrv/` | `functions/hrv/Dockerfile` | `europe-west1` |

### Eventarc Triggers

All three services listen to `document.written` events on `users/{uid}/sessions/{sessionId}`. The guard inside each function ensures idempotency -- only `upload_completed` status transitions trigger processing.

### BigQuery Export

The `firestore-bigquery-export` Firebase extension streams Firestore changes to BigQuery dataset `axon_firestore` for aggregate analytics and offline ML training.

### Android Release Builds

Release builds are signed using keystore credentials injected via CI environment variables (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). ProGuard/R8 minification and resource shrinking are enabled for the mobile module.

---

## Local Testing

Standalone Python scripts in `local-test/` allow offline testing of the signal-processing algorithms against a sample CSV dataset without requiring cloud infrastructure.

```bash
cd local-test
pip install -r requirements.txt
python SPARC.py
python LDLJ-A.py
python HRV.py
```

These scripts read from `sessionData.csv` and output metrics and plots locally.

---

## License

This project is part of an academic thesis. All rights reserved.
