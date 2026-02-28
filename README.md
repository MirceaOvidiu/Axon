# Axon

An Android fitness tracking application with mobile and wear OS support.

![CI](https://github.com/MirceaOvidiu/Axon/workflows/CI%20%E2%80%94%20Build%20%26%20Test/badge.svg)
![Release](https://github.com/MirceaOvidiu/Axon/workflows/CD%20%E2%80%94%20Release%20Build/badge.svg)

## 📱 Project Structure

- **`mobile/`** - Android mobile application
- **`wear/`** - Wear OS watch application
- **`docs/`** - Project documentation

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM with Hilt DI
- **Database**: Room
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Build**: Gradle with Kotlin DSL

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 36
- Firebase project with `google-services.json`

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/MirceaOvidiu/Axon.git
   cd Axon
   ```

2. **Configure Firebase**
   - Download `google-services.json` from [Firebase Console](https://console.firebase.google.com/)
   - Place it in `mobile/google-services.json`

3. **Set up signing (for release builds)**
   ```bash
   cp keystore.properties.template keystore.properties
   # Edit keystore.properties with your keystore details
   ```

4. **Build and run**
   ```bash
   ./gradlew :mobile:assembleDebug
   # Or open in Android Studio and run
   ```

## 🔄 CI/CD Pipeline

This project uses GitHub Actions for automated builds and releases.

### Workflows

- **CI**: Runs on every push/PR to `master`, `main`, or `develop`
  - Builds debug APKs
  - Runs unit tests
  - Performs lint checks

- **CD**: Runs on version tags (`v*.*.*`)
  - Builds signed release APKs
  - Creates Android App Bundle (AAB)
  - Publishes GitHub Release

### Documentation

- **[CI/CD Setup Guide](./docs/CI_CD_SETUP.md)** - Complete workflow documentation
- **[Secrets Management](./docs/SECRETS_MANAGEMENT.md)** - How to configure GitHub secrets
- **[Workflows Quick Reference](./.github/README.md)** - Quick start guide

## 📦 Building

### Debug Build
```bash
./gradlew :mobile:assembleDebug :wear:assembleDebug
```

### Release Build (requires signing setup)
```bash
./gradlew :mobile:assembleRelease :wear:assembleRelease
```

### Run Tests
```bash
./gradlew test
```

### Run Lint
```bash
./gradlew lint
```

## 🏗️ Project Configuration

### Version Catalog
Dependencies are managed in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml)

### Modules
- **mobile**: Main Android application
  - Supports API 30+
  - Firebase integration
  - Material 3 UI
  
- **wear**: Wear OS application
  - Health Services integration
  - Tiles support
  - Complications support

## 🔒 Security

- ⚠️ **Never commit** `keystore.properties`, `*.keystore`, or `*-b64.txt` files
- ⚠️ **Never commit** `google-services.json` to public repositories
- ✅ Use GitHub Secrets for CI/CD credentials
- ✅ Keep Firebase API keys restricted

See [SECRETS_MANAGEMENT.md](./docs/SECRETS_MANAGEMENT.md) for details.

## 📝 License

[Add your license here]

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📧 Contact

[Add your contact information here]

---

**Built with ❤️ using Kotlin & Jetpack Compose**
