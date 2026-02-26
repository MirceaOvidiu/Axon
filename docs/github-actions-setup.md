# GitHub Actions CI/CD Setup Guide

## What's included

| Workflow | File | Trigger |
|---|---|---|
| CI — Build & Test | `.github/workflows/ci.yml` | Every push/PR to `main` or `develop` |
| CD — Release | `.github/workflows/cd.yml` | Every tag matching `v*.*.*`, or manual |

---

## Step 1 — Push the repo to GitHub

If not done yet:

```bash
git remote add origin https://github.com/YOUR_USERNAME/Axon.git
git branch -M main
git push -u origin main
```

---

## Step 2 — Encode your secrets locally

### 2a. Encode `google-services.json`

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("mobile\google-services.json")) | Set-Clipboard
```

**Mac/Linux:**
```bash
base64 -w 0 mobile/google-services.json | pbcopy   # Mac
base64 -w 0 mobile/google-services.json            # Linux — copy output
```

### 2b. Generate a release keystore (if you don't have one)

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias axon-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

> ⚠️ **Back this keystore up in a safe place. Losing it means you can never update your app on the Play Store.**

### 2c. Encode the keystore

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Clipboard
```

**Mac/Linux:**
```bash
base64 -w 0 release.keystore
```

---

## Step 3 — Add secrets to GitHub

Go to your repo → **Settings → Secrets and variables → Actions → New repository secret**

| Secret name | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | Base64 output from Step 2a |
| `KEYSTORE_BASE64` | Base64 output from Step 2c |
| `KEY_ALIAS` | The alias you used (e.g. `axon-key`) |
| `KEY_PASSWORD` | The key password you set |
| `STORE_PASSWORD` | The keystore password you set |

---

## Step 4 — Trigger the CI workflow

Push any commit to `main` or `develop`. You'll see it running under the **Actions** tab.

```bash
git add .
git commit -m "chore: add CI/CD"
git push
```

---

## Step 5 — Trigger a release

Tag a commit to trigger the CD workflow and produce a signed release APK:

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will:
1. Build a signed release APK
2. Create a GitHub Release with the APK attached
3. Auto-generate release notes from commit messages

---

## Local development signing

For signing locally (e.g. for manual testing), copy the template and fill it in:

```bash
cp keystore.properties.template keystore.properties
# then edit keystore.properties with your real values
```

This file is in `.gitignore` and will never be committed.

---

## Workflow summary

```
push to main/develop
       │
       ▼
  ci.yml runs
  ├── Unit tests (mobile + wear)
  ├── Debug APK build
  └── Uploads APK + test results as artifacts (14-day retention)

push tag v*.*.*  (or manual trigger)
       │
       ▼
  cd.yml runs
  ├── Release APK build (signed)
  ├── Uploads as 90-day artifact
  └── Creates GitHub Release with APK attached
```
