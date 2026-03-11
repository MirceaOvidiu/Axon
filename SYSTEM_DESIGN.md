# Axon — System Design Document

## Table of Contents
1. [Overview](#1-overview)
2. [Architecture Diagram](#2-architecture-diagram)
3. [Components](#3-components)
   - 3.1 [Wear OS App](#31-wear-os-app)
   - 3.2 [Mobile App](#32-mobile-app)
   - 3.3 [Cloud Run Functions](#33-cloud-run-functions)
   - 3.4 [Firebase / GCP Infrastructure](#34-firebase--gcp-infrastructure)
4. [Data Flow](#4-data-flow)
   - 4.1 [Recording & Transport (Watch → Phone)](#41-recording--transport-watch--phone)
   - 4.2 [Upload to Cloud (Phone → Firestore)](#42-upload-to-cloud-phone--firestore)
   - 4.3 [Analysis Pipeline (Firestore → Cloud Run → Firestore)](#43-analysis-pipeline-firestore--cloud-run--firestore)
   - 4.4 [Results Display (Firestore → Phone)](#44-results-display-firestore--phone)
5. [Firestore Data Schema](#5-firestore-data-schema)
6. [Signal Processing Algorithms](#6-signal-processing-algorithms)
   - 6.1 [SPARC — Movement Smoothness (Frequency Domain)](#61-sparc--movement-smoothness-frequency-domain)
   - 6.2 [LDLJ-A — Movement Smoothness (Time Domain)](#62-ldlj-a--movement-smoothness-time-domain)
   - 6.3 [HRV — Autonomic Recovery](#63-hrv--autonomic-recovery)
7. [API Contracts](#7-api-contracts)
8. [Security Model](#8-security-model)
9. [Deployment Architecture](#9-deployment-architecture)
10. [Technology Stack](#10-technology-stack)

---

## 1. Overview

**Axon** is a wearable-assisted physiotherapy monitoring system. It captures gyroscope and heart rate data during rehabilitation exercises on a Wear OS smartwatch, transfers that data to an Android phone, uploads it to Google Cloud, and asynchronously runs three signal-processing analyses (SPARC, LDLJ-A, HRV) as serverless Cloud Run functions. Results and annotated plots are written back to Firestore and surfaced to the user in the Android app.

**Primary use case:** A patient performs a structured exercise session wearing the watch. The physiotherapist or patient can later review per-repetition movement quality scores and heart rate variability trends, providing quantitative feedback on motor rehabilitation progress.

---

## 2. Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────┐
│  WEAR OS WATCH                                                     │
│                                                                    │
│  ┌─────────────────┐   ┌──────────────────────┐                  │
│  │  GyroDataSource │   │HealthServicesDataSource│                  │
│  │ (SensorManager) │   │  (Wear Health Svcs)   │                  │
│  │    20 ms/sample │   │   continuous HR BPM   │                  │
│  └────────┬────────┘   └──────────┬────────────┘                  │
│           │  live readings         │                               │
│           ▼                        ▼                               │
│       RecordingUseCase ──► Room (watch-local)                     │
│       MainViewModel    ──► SyncUseCase                             │
│                               │                                    │
│              ┌────────────────┴──────────────┐                    │
│              │ DataClient.putDataMap          │ MessageClient      │
│              │ /sensor_data (live)            │ /session_data (bulk│
│              │                               │  JSON on stop)     │
└──────────────┼───────────────────────────────┼────────────────────┘
               │   Wearable Data Layer API      │
               ▼                                ▼
┌───────────────────────────────────────────────────────────────────┐
│  ANDROID PHONE                                                     │
│                                                                    │
│  DataLayerListenerService (WearableListenerService)               │
│  ├── onDataChanged  → WearableEventBus → WatchDataScreen (live)   │
│  └── onMessageReceived → SessionRepository.insert  (Room)         │
│                                 │                                  │
│  CloudSyncViewModel             │                                  │
│  └── CloudSyncUseCase           │                                  │
│      └── CloudSessionRepository │                                  │
│          ├── Firestore batch write: session + sensor_data          │
│          └── status = "upload_completed"  ◄── triggers CF         │
│                                                                    │
│  SessionDetailScreen ← SessionViewModel ← SessionRepository       │
│  (shows SPARC/LDLJ/HRV scores + plots via Coil)                   │
└──────────────────────────────────┬────────────────────────────────┘
                                   │ Firestore Eventarc trigger
                     ┌─────────────┼──────────────┐
                     ▼             ▼               ▼
              ┌────────────┐ ┌──────────┐ ┌──────────┐
              │  axon-sparc │ │axon-ldlj │ │ axon-hrv │  Cloud Run
              │  (Python)   │ │(Python)  │ │(Python)  │  europe-west1
              └──────┬──────┘ └────┬─────┘ └────┬─────┘
                     │              │              │
              reads sensor_data subcollection (Firestore)
              runs signal analysis (numpy / scipy)
              uploads PNG plot → axon-bucket (GCS)
              writes results + signed URL → session document
                     │
                     ▼
             ┌───────────────┐
             │   axon-bucket  │  Google Cloud Storage
             │  (private,     │  V4 signed URLs (7-day)
             │  uniform IAM)  │
             └───────────────┘
```

---

## 3. Components

### 3.1 Wear OS App

**Package:** `com.axon` (wear module)  
**Architecture:** MVI + Hilt DI + Jetpack Compose

#### Sensor Layer

| Class | Responsibility |
|---|---|
| `GyroDataSource` | Registers `TYPE_GYROSCOPE` sensor at 20 ms interval; emits `StateFlow<FloatArray(gx, gy, gz)>` |
| `HealthServicesDataSourceAdapter` | Implements Wear Health Services passive/active listener; emits `heartRateBpm: StateFlow<Double>` and availability state |
| `HealthServicesManager` | Lifecycle-aware manager for registering/unregistering the HR listener |

#### Data & Domain Layer

| Class | Responsibility |
|---|---|
| `RecordingUseCase` | Start/stop session, persist `SensorData` readings to watch-local Room DB |
| `SyncUseCase` | Trigger full session transfer to phone via `WearableDataSender` |
| `WearableDataSender` | **Channel 1:** `DataClient.putDataMap("/sensor_data")` for live streaming. **Channel 2:** `MessageClient.sendMessage("/session_data")` with full `SessionTransferData` JSON on session stop |
| `RecordingRepositoryContractImplementation` | Room CRUD on watch (sessions + readings) |
| `SyncRepositoryImplementation` | Manages bulk transfer handshake |

#### Presentation Layer

| Class | Responsibility |
|---|---|
| `MainViewModel` | MVI; intents: `StartRecording`, `StopRecording`, `SyncAllSessions`. Exposes `MainUiState` |
| `MainActivity` | Single-activity Compose host |
| `MainTileService` | Wear OS Tile (quick-start recording without opening app) |
| `MainComplicationService` | Wear OS complication data source |

---

### 3.2 Mobile App

**Package:** `com.axon` (mobile module)  
**Architecture:** MVVM + Clean Architecture + Hilt DI + Compose Navigation

#### Wearable Reception

| Class | Responsibility |
|---|---|
| `DataLayerListenerService` | `WearableListenerService` — receives both Wearable Data Layer channels from watch |
| `WearableEventBus` | `MutableSharedFlow` singleton; fan-out point for live sensor data to `WatchDataScreen` |

#### Data Layer

| Class | Responsibility |
|---|---|
| `SessionRepositoryImplementation` | Room CRUD: sessions, sensor data, firestore sync logic |
| `CloudSessionRepositoryImplementation` | Uploads/downloads sessions and sensor data to/from Firestore; exposes `Flow<Float>` progress |
| `AuthRepositoryImplementation` | Firebase Auth + Google Sign-In via Credential Manager |
| `SessionMapper` | Bidirectional mapping: `SessionEntity` ↔ `Session` (domain) ↔ `SessionFirestore` |
| `DataLayerListenerService` | Handles `/session_data` message → deserialises `SessionTransferData` JSON → inserts to Room |

#### Domain Layer

| Class | Responsibility |
|---|---|
| `Session` | Core model — all session metadata, scores, and plot URLs |
| `SensorData` | Per-reading model — `timestamp (ms)`, `heartRate`, `gyroX/Y/Z` |
| `SessionRepResult` | Per-repetition result — `rep`, `duration`, `score` |
| `CloudSyncUseCase` | Orchestrates: upload session → upload sensor data → set `status = upload_completed` |

#### Presentation Layer

| Screen | ViewModel | Description |
|---|---|---|
| `AuthScreen` | `AuthViewModel` | Google sign-in via Credential Manager |
| `RecoveryDashboard` | — | Summary of aggregate scores across sessions |
| `SessionListScreen` | `SessionViewModel` | List of sessions with status; navigate to detail |
| `SessionDetailScreen` | `SessionViewModel` | Per-rep SPARC/LDLJ tables, HRV metrics, SPARC/LDLJ/HRV plots loaded with Coil |
| `CloudSyncScreen` | `CloudSyncViewModel` | Upload/download progress UI |
| `WatchDataScreen` | `SensorDataViewModel` | Live sensor values from `WearableEventBus` |

#### Polling Strategy

After a session is uploaded, `SessionViewModel.pollForAnalysisScores()` polls Firestore at 2-second intervals (max 20 s = 10 iterations). On every tick the UI is updated. The loop stops early only when both `sparcScore` and `ldljScore` are non-null (HRV is optional). The sync condition also detects when a plot URL arrives after the score was already saved.

---

### 3.3 Cloud Run Functions

All three services are containerised Python services triggered by Firestore Eventarc events.

#### Common Pattern

```
Eventarc (document.written on users/{uid}/sessions/{sessionId})
    │
    ▼ Pub/Sub message → Cloud Run
    parse CloudEvent (DatastoreEntityEventData proto)
    guard: old_status != "upload_completed" AND new_status == "upload_completed"
    fetch sensor_data subcollection from Firestore
    run algorithm (numpy / scipy)
    upload PNG → GCS (V4 signed URL via IAM signBlob API)
    write results + plot URL back to session document
```

**GCS Signing:** Cloud Run default service account calls the IAM `signBlob` API (no key file required). Requires `roles/iam.serviceAccountTokenCreator` binding on the service account.

| Service | Image name | Target function | Port |
|---|---|---|---|
| `axon-sparc` | `gcr.io/axon-a4b3b/axon-sparc` | `process_sparc` | 8080 |
| `axon-ldlj` | `gcr.io/axon-a4b3b/axon-ldlj` | `process_ldlj` | 8080 |
| `axon-hrv` | `gcr.io/axon-a4b3b/axon-hrv` | `process_hrv` | 8080 |

---

### 3.4 Firebase / GCP Infrastructure

| Resource | Details |
|---|---|
| **GCP Project** | `axon-a4b3b` |
| **Region** | `europe-west1` |
| **Firestore** | Native mode, database `(default)`, multi-region `eur3` |
| **GCS Bucket** | `axon-bucket` — uniform bucket-level IAM, public access prevention enforced |
| **Eventarc** | Firestore document write triggers for all three Cloud Run services |
| **BigQuery Export** | Firebase extension `firestore-bigquery-export@0.2.8` streams Firestore changes to dataset `axon_firestore` |
| **Cloud Build** | CI trigger per service (Dockerfile-based); triggered on push to `main` |
| **Firebase Auth** | Google provider; UID is the user namespace in Firestore |

---

## 4. Data Flow

### 4.1 Recording & Transport (Watch → Phone)

```
1. User presses Start on watch
2. RecordingUseCase opens a session (Room insert)
3. Every 20 ms:
   a. GyroDataSource fires → (gx, gy, gz) stored in Room
   b. HealthServicesDataSource fires → heartRateBpm stored in Room
   c. LIVE: DataClient.putDataMap("/sensor_data") → phone receives onDataChanged
      → WearableEventBus.emit() → WatchDataScreen updates in real-time

4. User presses Stop
5. SyncUseCase queries all readings, builds SessionTransferData (JSON)
6. MessageClient.sendMessage("/session_data", payload)

7. Phone DataLayerListenerService.onMessageReceived:
   deserialise JSON → Session + List<SensorData>
   sessionRepository.insertSession(session)        ← Room
   sessionRepository.insertSensorData(readings)    ← Room (bulk)
```

### 4.2 Upload to Cloud (Phone → Firestore)

```
1. User navigates to CloudSyncScreen and triggers upload
2. CloudSyncUseCase:
   a. Write session document to users/{uid}/sessions/{sessionId}
   b. Batch-write sensor_data subcollection documents
   c. Update session document: status = "upload_completed"

3. CloudSyncScreen shows upload progress via Flow<Float>
```

### 4.3 Analysis Pipeline (Firestore → Cloud Run → Firestore)

```
1. Firestore document write event fires Eventarc
2. Pub/Sub delivers CloudEvent to all three Cloud Run services in parallel

Per service:
3. Parse DatastoreEntityEventData proto
4. Extract old_status / new_status from entity properties
5. Guard: only proceed if new_status == "upload_completed"
6. Extract user_id and session_id from entity key path

7. [SPARC / LDLJ] fetch all sensor_data docs → timestamps, gx, gy, gz
   [HRV]          fetch sensor_data docs with heartRate > 0

8. Run algorithm (see §6)
9. Generate matplotlib figure → upload to GCS → get signed URL

10. Firestore session document update:
    [SPARC] sparcScore, sparc_results, sparc_plot_url, sparc_processed_at
    [LDLJ]  ldljScore,  ldlj_results,  ldlj_plot_url,  ldlj_processed_at
    [HRV]   hrvScore, hrv_sdnn, hrv_mean_hr, hrv_pnn50, hrv_results, hrv_plot_url, hrv_processed_at
```

### 4.4 Results Display (Firestore → Phone)

```
1. SessionViewModel.pollForAnalysisScores() starts polling (2s interval, max 20s)
2. Each tick: CloudSessionRepository.syncSessionsFromCloud()
   → for each Firestore session doc, compare against Room entity
   → if any score or plot URL appeared: update Room entity (copy + re-insert)

3. StateFlow<Session?> cloudSession updates → recompose SessionDetailScreen
4. Coil loads plot image from signed URL (PNG, cached locally)
5. Polling stops when sparcScore AND ldljScore are both non-null
```

---

## 5. Firestore Data Schema

```
users/                                   ← top-level collection
  {uid}/                                 ← user document (Firebase UID)
    sessions/                            ← subcollection
      {sessionId}/                       ← session document (UUID string)
        # Core metadata
        firestoreId:       String
        userId:            String
        startTime:         Long   (epoch ms)
        endTime:           Long   (epoch ms)
        receivedAt:        Long   (epoch ms)
        uploadedAt:        Timestamp (server)
        dataPointCount:    Int
        status:            String   "pending" | "upload_completed"

        # SPARC results (written by axon-sparc)
        sparcScore:        Double   (mean across reps, higher is smoother, range ≈ -5 to 0)
        sparc_results:     Array<{rep: Int, start_idx: Int, end_idx: Int,
                                   duration: Double, score: Double}>
        sparc_plot_url:    String   (GCS V4 signed URL, 7-day TTL)
        sparc_processed_at: Timestamp

        # LDLJ-A results (written by axon-ldlj)
        ldljScore:         Double   (mean across reps, higher is smoother)
        ldlj_results:      Array<{rep: Int, start_idx: Int, end_idx: Int,
                                   duration: Double, score: Double}>
        ldlj_plot_url:     String   (GCS V4 signed URL, 7-day TTL)
        ldlj_processed_at: Timestamp

        # HRV results (written by axon-hrv)
        hrvScore:          Double   (= RMSSD in ms)
        hrv_sdnn:          Double   (ms)
        hrv_mean_hr:       Double   (BPM)
        hrv_pnn50:         Double   (%)
        hrv_results:       Map<String, Double>  (all metrics)
        hrv_plot_url:      String   (GCS V4 signed URL, 7-day TTL)
        hrv_processed_at:  Timestamp

        sensor_data/                     ← subcollection
          {auto-id}/                     ← one document per reading (~20 ms interval)
            timestamp:   Long  (epoch ms)
            gyroX:       Float (rad/s)
            gyroY:       Float (rad/s)
            gyroZ:       Float (rad/s)
            heartRate:   Double (BPM, nullable)
```

**Indexes:** Composite indexes defined in `firestore.indexes.json` for collection group queries on `sensor_data`.

---

## 6. Signal Processing Algorithms

### 6.1 SPARC — Movement Smoothness (Frequency Domain)

**Input:** Angular velocity time series from gyroscope (gx, gy, gz sampled at ~50 Hz after pre-processing).  
**Output:** SPARC score per repetition (dimensionless, typically −5 to 0; higher = smoother).

**Pipeline:**

1. **Vectorial speed:** $\omega(t) = \sqrt{g_x^2 + g_y^2 + g_z^2}$
2. **Filtering:** 4th-order Butterworth low-pass at 3 Hz cutoff
3. **Segmentation:** `scipy.signal.find_peaks` with peak prominence → valley-based repetition boundaries
4. **Per-repetition SPARC:**
   - Normalise speed profile to [0, 1] over duration
   - Compute FFT; retain components up to cutoff $f_c = 10$ Hz
   - $\text{SPARC} = -\sqrt{\int_{0}^{f_c}\left[\left(\frac{1}{\hat{\omega}_{peak}}\right)^2 + \left(\frac{d\hat{A}/df}{\hat{\omega}_{peak}}\right)^2\right]\,df}$
5. **Output:** Mean SPARC across all reps; individual rep table; 2-panel plot (speed trace + bar chart)

---

### 6.2 LDLJ-A — Movement Smoothness (Time Domain)

**Input:** Same gyroscope signal.  
**Output:** LDLJ-A score per repetition (dimensionless; higher = smoother, typically +8 to +12 for healthy adults).

**Pipeline:**

1. Same vectorial speed and filtering as SPARC
2. Same segmentation
3. **Per-repetition LDLJ-A:**
   - Numerically differentiate speed → jerk $j(t) = \ddot{\omega}(t)$
   - Integrate squared jerk: $J = \int j^2\,dt$ (trapezoidal rule)
   - Normalise: $j^* = J \cdot \dfrac{D^3}{A^2}$ where $D$ = duration, $A$ = peak speed amplitude
   - $\text{LDLJ\text{-}A} = -\ln(j^*)$
4. **Output:** Mean LDLJ-A; per-rep table; 2-panel plot (speed trace + bar chart)

---

### 6.3 HRV — Autonomic Recovery

**Input:** Heart rate (BPM) time series from Wear Health Services.  
**Output:** RMSSD, SDNN, pNN50, mean HR — indicators of autonomic nervous system recovery.

**Pipeline:**

1. **Filter:** Discard readings with `heartRate == null` or `heartRate == 0`
2. **R-R conversion:** $RR_i = \dfrac{60000}{\text{BPM}_i}$ (ms)
3. **Global metrics:**

   | Metric | Formula | Clinical meaning |
   |---|---|---|
   | **SDNN** | $\sqrt{\frac{1}{N-1}\sum(RR_i - \overline{RR})^2}$ | Overall HRV; general autonomic balance |
   | **RMSSD** | $\sqrt{\frac{1}{N-1}\sum(\Delta RR_i)^2}$ | Parasympathetic activity; primary recovery metric |
   | **pNN50** | $\frac{\vert\Delta RR_i\vert > 50\text{ ms}}{N-1} \times 100$ | Vagal tone |
   | **Mean HR** | $60000 / \overline{RR}$ | Average heart rate |
   | **CV_RR** | $\text{SDNN} / \overline{RR} \times 100$ | Coefficient of variation |

4. **Rolling RMSSD:** 10-beat sliding window to detect stress periods  
   Stress threshold: RMSSD < 15 ms (configurable constant)
5. **Output:** Metrics dict; 2-panel plot (HR trace + rolling RMSSD with stress shading); `hrvScore = RMSSD`

---

## 7. API Contracts

### Wearable Data Layer Paths

| Path | Channel | Direction | Payload |
|---|---|---|---|
| `/sensor_data` | `DataClient` (DataMap) | Watch → Phone | `"data"` key → JSON array of readings |
| `/session_data` | `MessageClient` | Watch → Phone | Raw bytes = UTF-8 JSON of `SessionTransferData` |

### SessionTransferData (JSON)

```json
{
  "sessionId":  "string",
  "startTime":  1234567890000,
  "endTime":    1234567899000,
  "sensorReadings": [
    {
      "timestamp": 1234567890000,
      "gyroX": 0.01,
      "gyroY": -0.02,
      "gyroZ": 0.15,
      "heartRateBpm": 72.0
    }
  ]
}
```

### Firestore Write (Cloud → Session document)

All Cloud Run services write atomic field groups to `users/{uid}/sessions/{sessionId}` using `DocumentReference.update()`. No session fields outside the service's own namespace are touched.

### GCS Object Naming Convention

| Service | GCS Path |
|---|---|
| SPARC | `sparc/{sessionId}_sparc.png` |
| LDLJ | `ldlj/{sessionId}_ldlj.png` |
| HRV | `hrv/{sessionId}_hrv.png` |

---

## 8. Security Model

### Authentication
- All users must authenticate via Firebase Auth (Google Sign-In)
- Firebase UID is used as the namespace key throughout Firestore and in all Cloud Run requests

### Firestore Rules
```
// Users can only access their own data
match /users/{userId}/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
// Default deny
match /{document=**} {
  allow read, write: if false;
}
```

### GCS Access
- Bucket: uniform bucket-level IAM, public access prevention enforced (no `allUsers` IAM, no per-object ACLs)
- Plot images are served via **V4 signed URLs** with 7-day TTL
- Signed URL generation: Cloud Run service account calls IAM `signBlob` API (requires `roles/iam.serviceAccountTokenCreator` on itself)
- Clients (Android) never have direct GCS read permission

### Cloud Run
- Services are not publicly invocable directly — only via Eventarc Pub/Sub push
- Service accounts follow least-privilege: Firestore read/write + GCS write + IAM signBlob

### Android
- Sensitive build values (keystore passwords, Google Services JSON) are injected via environment variables in CI; never committed to VCS
- `google-services.json` is committed (safe: contains only Firebase public identifiers, no API keys that grant write access without auth)

---

## 9. Deployment Architecture

### Cloud Run Services

```
Cloud Build (trigger: push to main)
    │
    ├─ Source: functions/ldlj/  → Build: functions/ldlj/Dockerfile
    │                              Deploy: axon-ldlj (europe-west1)
    ├─ Source: functions/sparc/ → Build: functions/sparc/Dockerfile
    │                              Deploy: axon-sparc (europe-west1)
    └─ Source: functions/hrv/   → Build: functions/hrv/Dockerfile
                                   Deploy: axon-hrv (europe-west1)
```

### Eventarc Triggers

All three Cloud Run services listen to the same Firestore event type but process different fields. The guard inside each function (`new_status == "upload_completed"`) ensures idempotency.

| Trigger | Event | Document pattern | Target |
|---|---|---|---|
| sparc-trigger | `document.written` | `users/{uid}/sessions/{sessionId}` | `axon-sparc` |
| ldlj-trigger | `document.written` | `users/{uid}/sessions/{sessionId}` | `axon-ldlj` |
| hrv-trigger | `document.written` | `users/{uid}/sessions/{sessionId}` | `axon-hrv` |

### BigQuery Export

Firebase extension `firestore-bigquery-export@0.2.8` streams Firestore document changes to BigQuery dataset `axon_firestore`. Table prefix: `sensor_data`. Used for aggregate analytics and offline ML training.

### Android CI

Release builds are signed using keystore credentials injected via CI environment variables (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Debug builds use the default debug keystore.

---

## 10. Technology Stack

| Layer | Technology |
|---|---|
| **Language (Android)** | Kotlin |
| **UI** | Jetpack Compose (mobile + Wear); Horologist (Wear Tiles/tooling) |
| **DI** | Hilt (both modules) |
| **Local DB** | Room / SQLite (both watch and phone) |
| **Serialisation** | Gson |
| **Async** | Kotlin Coroutines + Flow (StateFlow / SharedFlow) |
| **Image loading** | Coil (`AsyncImage`) |
| **Auth** | Firebase Authentication — Google Sign-In via `androidx.credentials` |
| **Cloud DB** | Cloud Firestore (`firebase-firestore-ktx`) |
| **Cloud Storage** | Google Cloud Storage (`google-cloud-storage`) |
| **Watch → Phone** | Wearable Data Layer API (`DataClient`, `MessageClient`) |
| **Watch sensors** | Wear Health Services (HR) + Android `SensorManager` (gyroscope) |
| **Watch extras** | Tiles (`MainTileService`), Complications (`MainComplicationService`) |
| **Cloud backend** | Python 3.10, Cloud Run (containerised Docker) |
| **Signal processing** | `numpy`, `scipy.signal` (Butterworth filter, `find_peaks`) |
| **Visualisation** | `matplotlib` (Agg backend, non-interactive) |
| **CF framework** | `functions-framework` (`cloudevents` CloudEvent handler) |
| **CF trigger** | Eventarc Firestore CDC (document write) via Pub/Sub push |
| **Analytics export** | Firebase Extension: `firestore-bigquery-export@0.2.8` |
| **Min SDK** | Android 30 / Wear OS 3 |
| **Target SDK** | Android 36 |
| **Build system** | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) |
| **CI** | Cloud Build (GCP) |
