# Recovery of Upper Limb Locomotor Functions Following Stroke

## 📖 Overview

### Context & Motivation
 Stroke (Cerebrovascular Accident - AVC) is the third leading cause of mortality and morbidity in developed countries (after myocardial infarction and cancer) and the leading cause of long-term disability for adults in Europe.
*  **Global Impact:** Demographic changes have led to an increase in incidence, with approximately  million cases annually worldwide, resulting in significant mortality and disability.
*  **Local Context:** In Romania, stroke mortality rates were historically high ( per 00,000 inhabitants in 000) compared to Western nations like the USA ( per 00,000).  Romania also ranks among the highest in the world for incidence.


Post-stroke recovery is a lengthy process.  Only approximately % of patients fully recover complex motor functions of the limbs and return to professional life within  years of the incident.  Locomotor functions are significantly impacted, impeding simple daily activities (eating, self-care) and affecting quality of life.

### Proposed Solution
 This project provides a technical solution to guide the recovery of upper limb functions using a **smartwatch application**.  The system monitors movement parameters and biometric markers to evaluate the "quality" of rehabilitation exercises in real-time, categorizing them into different correctness levels[: 0].

---

## 🏗️ System Architecture

 The solution implements a three-tier architecture: Smartwatch, Mobile, and Cloud.


### . Smartwatch Application (Wear OS)
 The primary data collection unit.
*  **Sensors:** Monitors detailed parameters including position, velocity, tremors, and movement fluidity.
*  **Biometrics:** Tracks pulse, oxygen saturation (SpO), and electrodermal activity to indicate stress or effort levels during exercises.
*  **Feedback:** Provides haptic alerts to the user during wear or exercises.
*  **Processing:** Performs calculations internally or offloads to the phone/cloud depending on complexity.

### . Mobile Application (Android)
 Acts as the companion interface.
*  **Data Aggregation:** Collects data transmitted by the watch during wear.
*  **Visualization:** Displays metrics and session reports for progress evaluation.
*  **Connectivity:** Sends and receives data from the Cloud.
*  **Roles:** Accessible by both the patient (for self-evaluation) and the medic/therapist (for monitoring patient progress).

### . Cloud Application
 Handles storage and heavy computation.
*  **Storage:** Manages data in SQL/NoSQL databases depending on their nature.
*  **Advanced Processing:** Handles processing tasks too demanding for mobile devices using hosted Machine Learning models or Kubernetes (KS) clusters.

---

## 🛠️ Technology Stack

| Component | Technologies |
| :--- | :--- |
| **Wearable** |  Kotlin, Wear OS, Jetpack Compose  |
| **Mobile** |  Kotlin, Ktor, Jetpack Compose  |
| **Cloud/Backend** |  Google Firebase, TensorFlow, PyTorch  |
| **Infrastructure** |  SQL, NoSQL, KS [: , ] |
