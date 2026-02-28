# CI/CD Pipeline Setup Guide

This document explains the CI/CD pipeline for the Axon Android project.

## 🏗️ Pipeline Architecture

The Axon project uses **GitHub Actions** for continuous integration and deployment with two main workflows:

```
┌─────────────────────────────────────────────────────────────┐
│                     GitHub Actions                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  CI Workflow (ci.yml)          Release Workflow (release.yml)│
│  ├─ Triggered on:              ├─ Triggered on:             │
│  │  • Push to main/master      │  • Version tags (v*.*.*)   │
│  │  • Pull requests            │  • Manual dispatch         │
│  │                             │                             │
│  ├─ Actions:                   ├─ Actions:                  │
│  │  • Validate code            │  • Run release tests       │
│  │  • Run unit tests           │  • Build signed APKs       │
│  │  • Build debug APKs         │  • Build AAB for Play Store│
│  │  • Run lint checks          │  • Create GitHub Release   │
│  │                             │                             │
│  └─ Output:                    └─ Output:                   │
│     • Test reports             │   • Signed release APK     │
│     • Debug APKs               │   • App Bundle (AAB)       │
│     • Lint reports             │   • GitHub Release         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 CI Workflow (ci.yml)

### Purpose
Validates code quality and builds debug versions for every code change.

### When It Runs
- ✅ Push to `master`, `main`, or `develop` branches
- ✅ Pull requests targeting these branches
- ✅ Manual trigger from GitHub Actions UI

### What It Does

1. **Environment Setup**
   - Checks out code
   - Configures JDK 17
   - Validates Gradle wrapper
   - Decodes Firebase `google-services.json`

2. **Quality Checks**
   - Runs unit tests for mobile module
   - Runs unit tests for wear module
   - Executes lint analysis

3. **Build Artifacts**
   - Assembles debug APK for mobile
   - Assembles debug APK for wear

4. **Upload Results**
   - Test reports (available for 14 days)
   - Debug APKs (available for 14 days)
   - Lint reports (available for 14 days)

### Required Secrets
- `GOOGLE_SERVICES_JSON` - Base64-encoded Firebase config

### Running Manually
```
1. Go to Actions tab
2. Select "CI — Build & Test"
3. Click "Run workflow"
4. Choose branch
5. Click "Run workflow"
```

---

## 🚀 Release Workflow (release.yml)

### Purpose
Builds production-ready signed releases for distribution.

### When It Runs
- ✅ Git tags matching `v*.*.*` pattern (e.g., `v1.0.0`, `v2.1.3`)
- ✅ Manual trigger from GitHub Actions UI

### What It Does

1. **Environment Setup**
   - Checks out code
   - Configures JDK 17
   - Validates Gradle wrapper
   - Decodes Firebase `google-services.json`
   - Decodes and configures signing keystore

2. **Quality Gates**
   - Runs release-variant unit tests
   - Validates code before building

3. **Build Production Artifacts**
   - Assembles signed release APK (mobile)
   - Assembles release APK (wear)
   - Bundles AAB for Google Play Store

4. **Publish**
   - Creates GitHub Release with artifacts
   - Attaches APKs and AAB files
   - Auto-generates release notes

### Required Secrets
- `GOOGLE_SERVICES_JSON` - Base64-encoded Firebase config
- `KEYSTORE_FILE` - Base64-encoded release keystore
- `STORE_PASSWORD` - Keystore password
- `KEY_ALIAS` - Key alias in keystore
- `KEY_PASSWORD` - Key password

### Creating a Release

#### Option 1: Using Git Tags (Recommended)
```bash
# Create and push a version tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

#### Option 2: Manual Dispatch
```
1. Go to Actions tab
2. Select "CD — Release Build"
3. Click "Run workflow"
4. Enter version name (e.g., 1.0.0)
5. Enter version code (e.g., 1)
6. Click "Run workflow"
```

---

## 🔑 Secrets Configuration

All secrets are managed in **Settings → Secrets and variables → Actions**.

### CI Workflow Secrets (1 required)
| Secret Name | Description | Used In |
|------------|-------------|---------|
| `GOOGLE_SERVICES_JSON` | Base64 Firebase config | CI & Release |

### Release Workflow Secrets (4 additional)
| Secret Name | Description | Used In |
|------------|-------------|---------|
| `KEYSTORE_FILE` | Base64 encoded keystore | Release only |
| `STORE_PASSWORD` | Keystore password | Release only |
| `KEY_ALIAS` | Key alias | Release only |
| `KEY_PASSWORD` | Key password | Release only |

**📖 For detailed setup instructions, see [SECRETS_MANAGEMENT.md](./SECRETS_MANAGEMENT.md)**

---

## 🛠️ Workflow Features

### Security Features
- ✅ Secrets are masked in logs
- ✅ Keystore files are cleaned up after use
- ✅ Gradle wrapper validation
- ✅ Environment variable injection
- ✅ No secrets in source code

### Performance Optimizations
- ✅ Gradle build caching
- ✅ JDK caching via `setup-java`
- ✅ Parallel execution where possible
- ✅ Timeout limits to prevent hanging

### Quality Assurance
- ✅ Unit tests (debug and release variants)
- ✅ Lint analysis with reports
- ✅ Build validation before release
- ✅ Comprehensive error logging

---

## 📊 Monitoring & Artifacts

### Viewing Workflow Runs
1. Go to **Actions** tab in GitHub
2. Select a workflow from the list
3. Click on a specific run to see details

### Downloading Artifacts
1. Open a completed workflow run
2. Scroll to **Artifacts** section
3. Click to download:
   - APKs (mobile/wear)
   - Test reports
   - Lint reports

### Artifact Retention
- **CI artifacts**: 14 days
- **Release artifacts**: 90 days

---

## 🧪 Testing the Pipeline

### Test CI Workflow
```bash
# Make a change and push to trigger CI
git checkout -b test-ci
echo "# Test" >> README.md
git add README.md
git commit -m "test: trigger CI"
git push origin test-ci
```

### Test Release Workflow
```bash
# Create a test release tag
git tag v0.0.1-test
git push origin v0.0.1-test

# Delete test tag after verification
git tag -d v0.0.1-test
git push origin :refs/tags/v0.0.1-test
```

---

## 🐛 Troubleshooting

### Common Issues

#### ❌ "GOOGLE_SERVICES_JSON secret is not set"
**Cause**: Missing or incorrectly named secret  
**Solution**: Add the secret in repository settings (see [SECRETS_MANAGEMENT.md](./SECRETS_MANAGEMENT.md))

#### ❌ "Task 'assembleRelease' fails with signing error"
**Cause**: Incorrect signing secrets  
**Solution**: Verify all 4 signing secrets (`KEYSTORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)

#### ❌ "Tests fail in CI but pass locally"
**Cause**: Environment differences or missing test resources  
**Solution**: 
- Check test logs in workflow output
- Ensure Firebase config is correctly decoded
- Verify test dependencies in `build.gradle.kts`

#### ❌ "Workflow times out"
**Cause**: Long-running builds or hanging processes  
**Solution**: 
- Current timeout: 30 min (CI), 45 min (Release)
- Check for infinite loops or network issues
- Optimize build with `--parallel` flag if needed

### Getting Help

1. **Check workflow logs**: Click on failed step to see detailed errors
2. **Review documentation**: See [SECRETS_MANAGEMENT.md](./SECRETS_MANAGEMENT.md)
3. **Validate locally**: Run `./gradlew :mobile:assembleRelease` locally
4. **GitHub Actions status**: Check [GitHub Status](https://www.githubstatus.com/)

---

## 📋 Checklist: Setting Up CI/CD

- [ ] Fork/clone the repository
- [ ] Add `GOOGLE_SERVICES_JSON` secret (for CI)
- [ ] Create release keystore (for releases)
- [ ] Add signing secrets (for releases):
  - [ ] `KEYSTORE_FILE`
  - [ ] `STORE_PASSWORD`
  - [ ] `KEY_ALIAS`
  - [ ] `KEY_PASSWORD`
- [ ] Test CI workflow with a test commit
- [ ] Test release workflow with a test tag
- [ ] Review and download artifacts
- [ ] Configure branch protection rules (optional)

---

## 🚦 Workflow Status Badges

Add these to your main README.md:

```markdown
![CI](https://github.com/MirceaOvidiu/Axon/workflows/CI%20%E2%80%94%20Build%20%26%20Test/badge.svg)
![Release](https://github.com/MirceaOvidiu/Axon/workflows/CD%20%E2%80%94%20Release%20Build/badge.svg)
```

---

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI/CD Best Practices](https://developer.android.com/studio/build/building-cmdline)
- [Gradle Build Optimization](https://docs.gradle.org/current/userguide/performance.html)
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)

---

## 🎯 Next Steps

1. **Set up secrets** following [SECRETS_MANAGEMENT.md](./SECRETS_MANAGEMENT.md)
2. **Test the pipeline** by pushing a commit or creating a tag
3. **Configure Play Store deployment** (optional) using Fastlane or Play Console API
4. **Set up CD to Firebase App Distribution** (optional) for beta testing
5. **Add more quality checks** like code coverage, UI tests, or security scans

---

**Need help?** Check the [SECRETS_MANAGEMENT.md](./SECRETS_MANAGEMENT.md) guide or open an issue.
