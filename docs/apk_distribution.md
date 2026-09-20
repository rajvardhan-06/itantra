# iTantra — APK Distribution Strategy & Guide

This document outlines the hosting and distribution architecture for distributing the signed **iTantra Android APK** (`com.itantra.app`) to end users via web and direct channels.

---

## 1. Hosting Architecture Evaluation

We evaluated two primary distribution approaches for the iTantra Android application:

| Evaluation Dimension | Option A: GitHub Releases (Recommended) | Option B: Vercel Static Hosting (Direct) |
|---|---|---|
| **Mechanism** | GitHub Releases CDN storage (`github.com/.../releases/download/...`) | Static bundle file in `web/public/download/iTantra-v1.0.0.apk` |
| **Max File Size Limit** | Up to **2 GB** per release asset | **100 MB** max deployment asset size on Vercel Hobby tier |
| **Bandwidth Limits** | High CDN bandwidth included with GitHub | 100 GB/month included bandwidth on Vercel Hobby tier |
| **Release Management** | Semantic tags (`v1.0.0`), changelogs, separate git git-lfs/binaries | Git repo bloat if binaries committed to git history |
| **Update Process** | Upload new APK to GitHub Release tag; update env var `VITE_ANDROID_APK_URL` | Replace file in `web/public/download/`, rebuild & redeploy |
| **Download Reliability** | Global CDN optimized for large file downloads | High speed edge network, auto-compressed headers |

### Recommended Hybrid Strategy:
1. **Initial / Out-of-the-Box Setup:** The Vercel website ships with the signed release APK bundled inside `/download/iTantra-v1.0.0.apk` (11.9 MB), well below Vercel's 100 MB asset limit. It works immediately without any external dependency.
2. **Production / Scale Strategy:** For ongoing version bumps and public distribution, upload releases to **GitHub Releases** and configure the environment variable `VITE_ANDROID_APK_URL` on Vercel. This prevents git repository bloat and scales to tens of thousands of downloads without exhausting Vercel bandwidth.

---

## 2. Configurable APK URL

The website dynamically resolves the download link via Vite environment variables:

```javascript
// web/app.js
const CONFIG = {
  version: '1.0.0',
  apkSize: '11.9 MB',
  minSdk: 'Android 8.0 (API 26)+',
  apkUrl: import.meta.env?.VITE_ANDROID_APK_URL || '/download/iTantra-v1.0.0.apk'
};
```

### To Redirect to GitHub Releases on Vercel:
1. Go to your **Vercel Project Settings** → **Environment Variables**.
2. Add a new variable:
   - **Key:** `VITE_ANDROID_APK_URL`
   - **Value:** `https://github.com/rajvardhan-06/itantra/releases/download/v1.0.0/iTantra-v1.0.0.apk`
3. Trigger a redeploy. The Download buttons and dynamic QR code on the website will automatically point to the official GitHub Release CDN.

---

## 3. Version Update Procedure

When releasing a new version of iTantra (e.g., v1.1.0):

### Step 1: Bump Android Version
In `app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 2
    versionName = "1.1.0"
}
```

### Step 2: Build Signed Release APK
Run in project root:
```powershell
.\gradlew.bat assembleRelease
```
Verify the output at:
`app/build/outputs/apk/release/app-release.apk`

### Step 3: Publish GitHub Release
1. Create a git tag:
   ```bash
   git tag -a v1.1.0 -m "Release v1.1.0 - Improved STT accuracy and Bluetooth range"
   git push origin v1.1.0
   ```
2. On GitHub (`github.com/rajvardhan-06/itantra/releases/new`):
   - Select tag `v1.1.0`.
   - Title: `iTantra v1.1.0`
   - Attach `app/build/outputs/apk/release/app-release.apk` renamed to `iTantra-v1.1.0.apk`.
   - Publish the release.

### Step 4: Update Website Config
In Vercel or `web/.env`:
```ini
VITE_ANDROID_APK_URL=https://github.com/rajvardhan-06/itantra/releases/download/v1.1.0/iTantra-v1.1.0.apk
```

---

## 4. Release Security Checklist

- [x] **APK Signing:** Verified with Android V2 signing scheme (`apksigner verify -v`).
- [x] **Keystore Protection:** Keystore files (`.keystore`, `.jks`) and passwords are listed in `.gitignore` and never committed to version control.
- [x] **Integrity Verification:** Calculate SHA-256 checksum for each release and publish it in the release notes so users can verify authenticity:
  ```powershell
  Get-FileHash -Path "dist\iTantra-v1.0.0.apk" -Algorithm SHA256
  ```
- [x] **MIME Headers Configured:** `vercel.json` serves the APK with `Content-Type: application/vnd.android.package-archive` and `Content-Disposition: attachment` to prevent browser corruption.
- [x] **Safe Downloads Disclosure:** The website instructs users to only install APKs originating from the official domain or GitHub repository.
