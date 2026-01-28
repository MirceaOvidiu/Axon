# Recovery of Upper Limb Locomotor Functions Following Stroke

## 📖 Overview

### Context & Motivation
[cite_start]Stroke (Cerebrovascular Accident - AVC) is the third leading cause of mortality and morbidity in developed countries (after myocardial infarction and cancer) and the leading cause of long-term disability for adults in Europe[cite: 12].
* [cite_start]**Global Impact:** Demographic changes have led to an increase in incidence, with approximately 15 million cases annually worldwide, resulting in significant mortality and disability[cite: 13].
* [cite_start]**Local Context:** In Romania, stroke mortality rates were historically high (282 per 100,000 inhabitants in 2000) compared to Western nations like the USA (41 per 100,000)[cite: 14]. [cite_start]Romania also ranks among the highest in the world for incidence[cite: 15].


Post-stroke recovery is a lengthy process. [cite_start]Only approximately 29% of patients fully recover complex motor functions of the limbs and return to professional life within 7 years of the incident[cite: 17]. [cite_start]Locomotor functions are significantly impacted, impeding simple daily activities (eating, self-care) and affecting quality of life[cite: 16].

### Proposed Solution
[cite_start]This project provides a technical solution to guide the recovery of upper limb functions using a **smartwatch application**[cite: 18]. [cite_start]The system monitors movement parameters and biometric markers to evaluate the "quality" of rehabilitation exercises in real-time, categorizing them into different correctness levels[cite: 20].

---

## 🏗️ System Architecture

[cite_start]The solution implements a three-tier architecture: Smartwatch, Mobile, and Cloud[cite: 23].


### 1. Smartwatch Application (Wear OS)
[cite_start]The primary data collection unit[cite: 25].
* [cite_start]**Sensors:** Monitors detailed parameters including position, velocity, tremors, and movement fluidity[cite: 19].
* [cite_start]**Biometrics:** Tracks pulse, oxygen saturation (SpO2), and electrodermal activity to indicate stress or effort levels during exercises[cite: 19].
* [cite_start]**Feedback:** Provides haptic alerts to the user during wear or exercises[cite: 27].
* [cite_start]**Processing:** Performs calculations internally or offloads to the phone/cloud depending on complexity[cite: 26].

### 2. Mobile Application (Android)
[cite_start]Acts as the companion interface[cite: 21].
* [cite_start]**Data Aggregation:** Collects data transmitted by the watch during wear[cite: 29].
* [cite_start]**Visualization:** Displays metrics and session reports for progress evaluation[cite: 21].
* [cite_start]**Connectivity:** Sends and receives data from the Cloud[cite: 31].
* [cite_start]**Roles:** Accessible by both the patient (for self-evaluation) and the medic/therapist (for monitoring patient progress)[cite: 22].

### 3. Cloud Application
[cite_start]Handles storage and heavy computation[cite: 32].
* [cite_start]**Storage:** Manages data in SQL/NoSQL databases depending on their nature[cite: 33].
* [cite_start]**Advanced Processing:** Handles processing tasks too demanding for mobile devices using hosted Machine Learning models or Kubernetes (K8S) clusters[cite: 34].

---

## 🛠️ Technology Stack

| Component | Technologies |
| :--- | :--- |
| **Wearable** | [cite_start]Kotlin, Wear OS, Jetpack Compose [cite: 24] |
| **Mobile** | [cite_start]Kotlin, Ktor, Jetpack Compose [cite: 28] |
| **Cloud/Backend** | [cite_start]Google Firebase, TensorFlow, PyTorch [cite: 32] |
| **Infrastructure** | [cite_start]SQL, NoSQL, K8S [cite: 33, 34] |
