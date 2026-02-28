# 🔄 CI/CD Workflows

This directory contains GitHub Actions workflows for the Axon project.

## 📁 Workflows

### [`ci.yml`](./workflows/ci.yml) - Continuous Integration
**Triggers**: Push/PR to `master`, `main`, `develop`  
**Purpose**: Build & test debug versions  
**Secrets Required**: `GOOGLE_SERVICES_JSON`

### [`release.yml`](./workflows/release.yml) - Release Build
**Triggers**: Version tags (`v*.*.*`) or manual  
**Purpose**: Build signed releases for production  
**Secrets Required**: `GOOGLE_SERVICES_JSON`, `KEYSTORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

---

## 🚀 Quick Start

### 1️⃣ Set Up Secrets

Go to **Settings → Secrets and variables → Actions** and add:

#### For CI (Required)
```
GOOGLE_SERVICES_JSON = [base64 of mobile/google-services.json]
```

#### For Releases (Additional)
```
KEYSTORE_FILE = [base64 of release.keystore]
STORE_PASSWORD = [your keystore password]
KEY_ALIAS = [your key alias]
KEY_PASSWORD = [your key password]
```

### 2️⃣ Encode Secrets

**On Linux/macOS:**
```bash
base64 -w 0 mobile/google-services.json > google-services-b64.txt
base64 -w 0 release.keystore > keystore-b64.txt
```

**On Windows PowerShell:**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("mobile\google-services.json")) | Out-File -Encoding ASCII google-services-b64.txt
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -Encoding ASCII keystore-b64.txt
```

### 3️⃣ Trigger Workflows

**CI**: Push to `master`/`main`/`develop` or create a PR

**Release**: Create and push a version tag
```bash
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

---

## 📚 Full Documentation

For detailed instructions, see:
- **[CI/CD Setup Guide](../docs/CI_CD_SETUP.md)** - Complete workflow documentation
- **[Secrets Management Guide](../docs/SECRETS_MANAGEMENT.md)** - Detailed secrets setup

---

## 🎯 Workflow Status

![CI](https://github.com/MirceaOvidiu/Axon/workflows/CI%20%E2%80%94%20Build%20%26%20Test/badge.svg)
![Release](https://github.com/MirceaOvidiu/Axon/workflows/CD%20%E2%80%94%20Release%20Build/badge.svg)

---

## 🆘 Troubleshooting

### "GOOGLE_SERVICES_JSON secret is not set"
→ Add the secret in repository Settings → Secrets and variables → Actions

### "Signing failed"
→ Verify all 4 signing secrets are correct (see [SECRETS_MANAGEMENT.md](../docs/SECRETS_MANAGEMENT.md))

### "Invalid base64"
→ Ensure no line breaks. Use `-w 0` flag on Linux/macOS or verify ASCII encoding on Windows

---

**Questions?** Check the [full documentation](../docs/CI_CD_SETUP.md) or open an issue.
