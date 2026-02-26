# GitHub Secrets Configuration Guide

## Step-by-Step Instructions

### 1. Open GitHub Repository Secrets Page

Go to: **https://github.com/MirceaOvidiu/Axon/settings/secrets/actions**

(If the link doesn't open, navigate manually: Repository → Settings → Secrets and variables → Actions)

---

### 2. Add All 5 Secrets

Click **"New repository secret"** for each one:

#### Secret 1: `GOOGLE_SERVICES_JSON`

**Name:** `GOOGLE_SERVICES_JSON`

**Value:** Copy the ENTIRE contents from:
```
C:\Users\Mircea\AndroidStudioProjects\Axon\google-services-b64.txt
```

The value should start with: `ew0KICAicHJvamVjdF9pbmZvIjogew0KIC...` (1780 characters)

---

#### Secret 2: `KEYSTORE_BASE64`

**Name:** `KEYSTORE_BASE64`

**Value:** Copy the ENTIRE contents from:
```
C:\Users\Mircea\AndroidStudioProjects\Axon\keystore-b64.txt
```

The value should start with: `MIIKqAIBAzCCClIGCSqGSIb3DQEHAA...` (3644 characters)

---

#### Secret 3: `KEY_ALIAS`

**Name:** `KEY_ALIAS`

**Value:** 
```
axon-key
```

---

#### Secret 4: `KEY_PASSWORD`

**Name:** `KEY_PASSWORD`

**Value:**
```
axon2024release
```

---

#### Secret 5: `STORE_PASSWORD`

**Name:** `STORE_PASSWORD`

**Value:**
```
axon2024release
```

---

### 3. Verify All Secrets Are Added

After adding all 5, you should see them listed on the secrets page:
- ✓ GOOGLE_SERVICES_JSON
- ✓ KEYSTORE_BASE64
- ✓ KEY_ALIAS
- ✓ KEY_PASSWORD
- ✓ STORE_PASSWORD

---

### 4. Re-run the Failed CI Workflow

1. Go to: **https://github.com/MirceaOvidiu/Axon/actions**
2. Click on the failed workflow run
3. Click **"Re-run all jobs"** button in the top-right
4. Wait for the build to complete (~5-10 minutes)

---

### 5. Check Build Status

The CI workflow will:
- ✓ Decode google-services.json from the secret
- ✓ Build debug APK
- ✓ Run unit tests
- ✓ Upload APK artifact

If it passes, you'll see a green checkmark ✓

---

## What Each Secret Does

| Secret | Purpose | Where It's Used |
|--------|---------|----------------|
| `GOOGLE_SERVICES_JSON` | Firebase configuration | Decoded to `mobile/google-services.json` during build |
| `KEYSTORE_BASE64` | Release signing keystore | Decoded to `mobile/release.keystore` for CD builds |
| `KEY_ALIAS` | Keystore key alias | Used when signing release APK |
| `KEY_PASSWORD` | Key password | Used when signing release APK |
| `STORE_PASSWORD` | Keystore password | Used when signing release APK |

---

## Troubleshooting

### If CI still fails after adding secrets:

1. **Check secret names** - They must match exactly (case-sensitive)
2. **Check for extra spaces** - Copy the base64 strings without any line breaks or spaces
3. **View logs** - Click on the failed step in GitHub Actions to see the exact error

### If you need to regenerate the base64 files:

```powershell
# Google Services
[Convert]::ToBase64String([IO.File]::ReadAllBytes("mobile\google-services.json")) | Out-File -NoNewline "google-services-b64.txt"

# Keystore
[Convert]::ToBase64String([IO.File]::ReadAllBytes("axon-release.keystore")) | Out-File -NoNewline "keystore-b64.txt"
```

---

## Next Steps After CI Passes

Once CI is working, you can trigger a release build:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This will:
- Build a **signed release APK**
- Create a GitHub Release
- Attach the APK to the release
