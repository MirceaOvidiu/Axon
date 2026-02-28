# GitHub Secrets Management Guide

This guide explains how to configure GitHub Actions secrets for the Axon project's CI/CD pipelines.

## 📋 Overview

Our CI/CD pipelines require several secrets to build and sign the Android application:

- **`GOOGLE_SERVICES_JSON`**: Firebase configuration (required for all builds)
- **`KEYSTORE_FILE`**: Release signing keystore (required for release builds only)
- **`STORE_PASSWORD`**: Keystore password (required for release builds only)
- **`KEY_ALIAS`**: Key alias in keystore (required for release builds only)
- **`KEY_PASSWORD`**: Key password (required for release builds only)

---

## 🔐 Setting Up Secrets

### Step 1: Navigate to Repository Settings

1. Go to your GitHub repository: `https://github.com/MirceaOvidiu/Axon`
2. Click on **Settings** (top-right tab)
3. In the left sidebar, expand **Secrets and variables**
4. Click **Actions**

### Step 2: Add Required Secrets

Click **New repository secret** for each secret below.

---

## 📦 Secret Details

### 1. GOOGLE_SERVICES_JSON (Required for CI & CD)

**Description**: Base64-encoded Firebase configuration file

**How to generate**:

```bash
# On Linux/macOS:
base64 -w 0 mobile/google-services.json > google-services-b64.txt

# On Windows PowerShell:
[Convert]::ToBase64String([IO.File]::ReadAllBytes("mobile\google-services.json")) | Out-File -Encoding ASCII google-services-b64.txt
```

**Steps**:
1. Download `google-services.json` from [Firebase Console](https://console.firebase.google.com/)
   - Go to Project Settings → General → Your apps
   - Download the `google-services.json` for your Android app
2. Encode it to base64 using the command above
3. Copy the entire content of `google-services-b64.txt`
4. In GitHub Secrets, create a secret named `GOOGLE_SERVICES_JSON`
5. Paste the base64 content as the value

**⚠️ Important**: The decoded file should look like this:
```json
{
  "project_info": {
    "project_number": "...",
    "project_id": "...",
    ...
  },
  "client": [ ... ]
}
```

---

### 2. KEYSTORE_FILE (Required for Release Builds)

**Description**: Base64-encoded release signing keystore

**How to generate**:

If you don't have a keystore yet, create one:

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias axon-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

Then encode it to base64:

```bash
# On Linux/macOS:
base64 -w 0 release.keystore > keystore-b64.txt

# On Windows PowerShell:
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -Encoding ASCII keystore-b64.txt
```

**Steps**:
1. Encode your keystore file using the command above
2. Copy the content of `keystore-b64.txt`
3. In GitHub Secrets, create a secret named `KEYSTORE_FILE`
4. Paste the base64 content as the value

---

### 3. STORE_PASSWORD (Required for Release Builds)

**Description**: Password for the keystore file

**Value**: The password you used when creating the keystore (e.g., `YOUR_STORE_PASSWORD`)

**Steps**:
1. In GitHub Secrets, create a secret named `STORE_PASSWORD`
2. Enter your keystore password as the value

---

### 4. KEY_ALIAS (Required for Release Builds)

**Description**: Alias of the key inside the keystore

**Value**: The alias you used when creating the keystore (e.g., `axon-release`)

**Steps**:
1. In GitHub Secrets, create a secret named `KEY_ALIAS`
2. Enter your key alias as the value

---

### 5. KEY_PASSWORD (Required for Release Builds)

**Description**: Password for the specific key in the keystore

**Value**: The key password you used when creating the keystore (e.g., `YOUR_KEY_PASSWORD`)

**Steps**:
1. In GitHub Secrets, create a secret named `KEY_PASSWORD`
2. Enter your key password as the value

---

## 🔍 Verifying Secrets

After adding all secrets, you should see them listed in **Settings → Secrets and variables → Actions**:

- ✅ `GOOGLE_SERVICES_JSON`
- ✅ `KEYSTORE_FILE` (for releases)
- ✅ `STORE_PASSWORD` (for releases)
- ✅ `KEY_ALIAS` (for releases)
- ✅ `KEY_PASSWORD` (for releases)

---

## 🚀 CI/CD Workflows

### CI Workflow (`ci.yml`)

**Triggers on**: 
- Push to `master`, `main`, or `develop` branches
- Pull requests to these branches
- Manual trigger via GitHub Actions UI

**Requires**: 
- `GOOGLE_SERVICES_JSON` only

**Builds**: 
- Debug APKs for mobile and wear
- Runs unit tests
- Runs lint checks

### Release Workflow (`release.yml`)

**Triggers on**: 
- Git tags matching `v*.*.*` (e.g., `v1.0.0`)
- Manual trigger via GitHub Actions UI

**Requires**: 
- `GOOGLE_SERVICES_JSON`
- `KEYSTORE_FILE`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

**Builds**: 
- Release APKs (signed) for mobile and wear
- Release AAB (Android App Bundle) for Play Store
- Creates GitHub Release with artifacts

---

## 🛠️ Local Development

For local development, you need to create a `keystore.properties` file in the project root:

```bash
# Copy the template
cp keystore.properties.template keystore.properties

# Edit keystore.properties with your local values
```

Example `keystore.properties`:
```properties
storeFile=/path/to/your/release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=axon-release
keyPassword=YOUR_KEY_PASSWORD
```

**⚠️ Never commit `keystore.properties` or `.keystore` files to Git!** They are in `.gitignore`.

---

## 📝 Environment Variables vs Secrets

### When to Use GitHub Secrets:
- ✅ Passwords, API keys, tokens
- ✅ Signing credentials
- ✅ Firebase configuration (contains API keys)
- ✅ Any sensitive data that should never be public

### When to Use Environment Variables (not secrets):
- Build configurations (debug/release)
- Version numbers
- Build timestamps
- Non-sensitive build flags

---

## 🔒 Security Best Practices

1. **Never log secrets**: GitHub Actions automatically masks secrets in logs, but be careful with custom scripts
2. **Use separate keys**: Use different keystores for debug and release builds
3. **Rotate regularly**: Change passwords and regenerate keystores periodically
4. **Limit access**: Only give repository access to people who need it
5. **Backup securely**: Keep encrypted backups of keystores and `keystore.properties`
6. **Use environments**: For production apps, consider using GitHub Environments for additional protection

---

## 🐛 Troubleshooting

### CI workflow fails with "GOOGLE_SERVICES_JSON secret is not set!"

**Solution**: Add the `GOOGLE_SERVICES_JSON` secret following Step 2 section "1. GOOGLE_SERVICES_JSON"

### Release build fails with signing errors

**Solution**: Verify all 4 signing secrets are correct:
- `KEYSTORE_FILE` - Must be base64-encoded keystore
- `STORE_PASSWORD` - Must match your keystore password
- `KEY_ALIAS` - Must match the alias in your keystore
- `KEY_PASSWORD` - Must match your key password

### How to verify keystore details?

```bash
# List all aliases in keystore
keytool -list -v -keystore release.keystore -storepass YOUR_STORE_PASSWORD
```

### Invalid base64 encoding errors

**Solution**: 
- Ensure no line breaks in the base64 string
- Use `-w 0` flag on Linux/macOS
- On Windows, verify the file is ASCII encoded, not UTF-8 with BOM

---

## 📚 Additional Resources

- [GitHub Actions Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Android App Signing Guide](https://developer.android.com/studio/publish/app-signing)
- [Firebase Console](https://console.firebase.google.com/)
- [Gradle Signing Configuration](https://developer.android.com/studio/build/gradle-tips#sign-your-app)

---

## 🆘 Need Help?

If you encounter issues:
1. Check the workflow logs in GitHub Actions
2. Verify all required secrets are set correctly
3. Ensure base64 encoding is correct (no line breaks)
4. Verify keystore passwords match

For Firebase issues, check the [Firebase documentation](https://firebase.google.com/docs/android/setup).
