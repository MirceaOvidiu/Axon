# Axon - GCP Integration Setup Guide

This guide will help you set up Google Cloud Platform (GCP) integration for user accounts and cloud session storage in your Axon fitness tracking app.

## Prerequisites

1. Google Cloud Platform account
2. Firebase project
3. Android Studio
4. Valid Google Play Console account (for Google Sign-In)

## Step 1: Firebase Project Setup

### 1.1 Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Name your project (e.g., "axon-fitness-app")
4. Enable Google Analytics (optional)
5. Create the project

### 1.2 Enable Authentication
1. In Firebase Console, go to **Authentication** > **Sign-in method**
2. Enable **Email/Password** provider
3. Enable **Google** provider
   - Click on Google provider
   - Enable the provider
   - Add your project's support email
   - Download the config files (we'll use this later)

### 1.3 Enable Firestore Database
1. Go to **Firestore Database** > **Create database**
2. Start in **test mode** (we'll secure it later)
3. Choose your preferred location

### 1.4 Enable Firebase Storage (Optional)
1. Go to **Storage** > **Get started**
2. Start in test mode
3. Choose your preferred location

## Step 2: Android App Configuration

### 2.1 Add Android App to Firebase
1. In Firebase Console, click **Add app** > **Android**
2. Register app with package name: `com.axon`
3. Download `google-services.json`
4. Place it in your `mobile/` directory (replace the placeholder file)

### 2.2 Configure Google Sign-In
1. In Firebase Console, go to **Authentication** > **Sign-in method** > **Google**
2. Copy the **Web client ID**
3. Replace `your_web_client_id_here` in `mobile/src/main/res/values/strings.xml`:

```xml
<string name="default_web_client_id">YOUR_ACTUAL_WEB_CLIENT_ID</string>
```

### 2.3 Update google-services.json
Replace the placeholder content in `mobile/google-services.json` with your actual configuration file downloaded from Firebase.

## Step 3: Firestore Security Rules

### 3.1 Configure Security Rules
In Firebase Console, go to **Firestore Database** > **Rules** and update with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /sessions/{document=**} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    match /sensor_data/{document=**} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    match /users/{userId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == userId;
    }
  }
}
```

### 3.2 Storage Security Rules (if using Firebase Storage)
Go to **Storage** > **Rules** and update with:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/{allPaths=**} {
      allow read, write: if request.auth != null && 
        request.auth.uid == userId;
    }
  }
}
```

## Step 4: Features Overview

### 4.1 Authentication Features
- **Email/Password Authentication**: Users can create accounts and sign in
- **Google Sign-In**: One-tap Google authentication (needs additional setup)
- **Password Reset**: Email-based password recovery
- **Profile Management**: Users can update display name and photo
- **Account Deletion**: Complete account and data removal

### 4.2 Cloud Storage Features
- **Session Upload**: Upload workout sessions with sensor data to Firestore
- **Session Download**: Download sessions from cloud to local device
- **Automatic Sync**: Batch upload all local sessions
- **Progress Tracking**: Real-time upload/download progress indicators
- **Data Security**: All data is user-scoped and encrypted in transit

### 4.3 Data Structure

#### Session Document (Firestore)
```json
{
  "id": 1234567890,
  "userId": "firebase_user_id",
  "startTime": 1640995200000,
  "endTime": 1640995800000,
  "receivedAt": 1640995900000,
  "dataPointCount": 120,
  "uploadedAt": 1640996000000
}
```

#### Sensor Data Document (Firestore)
```json
{
  "sessionId": 1234567890,
  "userId": "firebase_user_id",
  "timestamp": 1640995210000,
  "heartRate": 75.5,
  "gyroX": 0.1,
  "gyroY": -0.2,
  "gyroZ": 0.05
}
```

## Step 5: Usage Instructions

### 5.1 Authentication Flow
1. App starts and checks authentication state
2. If not authenticated, shows login/signup screen
3. User can sign up with email/password or sign in
4. Upon successful authentication, navigates to dashboard
5. User profile shows in top header with sign-out option

### 5.2 Cloud Sync Flow
1. Navigate to "Cloud" tab in bottom navigation
2. View local sessions and cloud sessions in separate tabs
3. Upload individual sessions or all sessions at once
4. Download sessions from cloud to local storage
5. Delete sessions from cloud if needed

### 5.3 Data Export Options
Users can download their data in several ways:
- **Individual Session**: Download specific session with all sensor data
- **Batch Export**: Upload all sessions to cloud, then access via Firebase Console
- **Manual Export**: Use Firebase Console to export Firestore collections

## Step 6: Advanced Configuration

### 6.1 Custom Claims (Optional)
For premium features or admin access, you can set custom claims:

```javascript
// Firebase Cloud Function example
const admin = require('firebase-admin');

exports.addPremiumUser = functions.https.onCall(async (data, context) => {
  await admin.auth().setCustomUserClaims(data.uid, { premium: true });
  return { success: true };
});
```

### 6.2 Cloud Functions (Optional)
Add server-side processing for data analysis:

```javascript
// Example: Calculate session statistics
exports.processSession = functions.firestore
  .document('sessions/{sessionId}')
  .onCreate(async (snap, context) => {
    const sessionData = snap.data();
    // Process and analyze session data
    // Store results back to Firestore
  });
```

### 6.3 Analytics Integration
Firebase Analytics is automatically integrated. You can track custom events:

```kotlin
// In your ViewModel
FirebaseAnalytics.getInstance(context).logEvent("session_uploaded") {
    param("session_duration", sessionDuration)
    param("data_points", dataPointCount)
}
```

## Step 7: Testing

### 7.1 Authentication Testing
1. Test email/password signup and signin
2. Test password reset functionality
3. Test sign out and re-authentication
4. Verify user profile updates

### 7.2 Cloud Sync Testing
1. Create test sessions on watch
2. Sync sessions from watch to phone
3. Upload sessions to cloud
4. Verify data in Firebase Console
5. Test downloading sessions on different device
6. Test offline functionality

## Step 8: Production Deployment

### 8.1 Security Checklist
- ✅ Firestore security rules implemented
- ✅ Storage security rules implemented (if using)
- ✅ API keys restricted in Google Cloud Console
- ✅ Test mode disabled in Firestore
- ✅ Authentication requirements enforced

### 8.2 Performance Optimization
- Enable Firestore indexing for queries
- Implement pagination for large datasets
- Use Firestore offline persistence
- Optimize batch operations

### 8.3 Monitoring
- Set up Firebase Crashlytics
- Monitor authentication metrics
- Set up alerts for unusual usage patterns
- Monitor Firestore usage and costs

## Troubleshooting

### Common Issues
1. **Google Sign-In not working**: Verify SHA-1 certificates in Firebase Console
2. **Firestore permission denied**: Check security rules and user authentication
3. **Upload failures**: Verify network connectivity and file size limits
4. **Authentication state issues**: Clear app data and test again

### Debug Commands
```bash
# Check Firebase project configuration
firebase projects:list

# Deploy security rules
firebase deploy --only firestore:rules

# Check Firestore indexes
firebase firestore:indexes
```

## Cost Considerations

### Firebase Pricing (as of 2024)
- **Authentication**: Free up to 10k MAU, then $0.0055/MAU
- **Firestore**: Free tier includes 1GB storage, 50k reads/day
- **Storage**: Free tier includes 1GB storage, 1GB transfer/day
- **Functions**: Free tier includes 2M invocations/month

### Optimization Tips
- Batch operations to reduce read/write costs
- Use Firestore offline caching
- Implement data archiving for old sessions
- Monitor usage in Firebase Console

This setup provides a complete, production-ready cloud infrastructure for your Axon fitness app with secure user accounts and scalable session storage.
