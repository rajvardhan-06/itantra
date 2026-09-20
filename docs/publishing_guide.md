# iTantra — Production Publishing & Release Guide

This guide covers everything required to publish the **iTantra** offline multilingual communication application to the **Google Play Store** or distribute it directly via **GitHub Releases**.

---

## 1. Distribution Options Overview

| Distribution Channel | Target Audience | Format | Cost | Approval Time |
|---|---|---|---|---|
| **Google Play Store** | General public & enterprise | `.aab` (Android App Bundle) | $25 one-time developer fee | 1–7 days review |
| **GitHub Releases** | Hackathon judges, open-source testers | `.apk` (Direct install) | Free | Instant |
| **Internal / Closed Testing** | Beta testers, hackathon team | `.aab` / `.apk` via Play Console | Included in Play Console | Hours |

---

## 2. Step 1: Create a Release Keystore

To publish to Google Play or install release builds on real devices, the app must be cryptographically signed.

Run the following command in PowerShell using `keytool` (shipped with the Java JDK / Android Studio):

```powershell
keytool -genkey -v -keystore "d:\itantra\itantra-release-key.jks" -alias itantra-release -keyalg RSA -keysize 2048 -validity 10000
```

You will be prompted to:
1. Enter a strong keystore password (keep this safe!).
2. Fill in your Name / Organization details (e.g., `iTantra Team`).
3. Confirm with `yes`.

---

## 3. Step 2: Build Release Bundle or APK

### Option A: Via Android Studio GUI (Easiest)
1. Open the project in Android Studio.
2. Go to **Build** → **Generate Signed Bundle / APK...**
3. Select **Android App Bundle** (for Google Play) or **APK** (for direct distribution).
4. Choose the keystore generated above (`d:\itantra\itantra-release-key.jks`).
5. Enter your keystore password, alias (`itantra-release`), and key password.
6. Select destination folder and build variant **release**.
7. Click **Create**.

### Option B: Via Command Line (Gradle)
```powershell
# For Google Play Store (.aab):
.\gradlew bundleRelease

# For direct APK installation (.apk):
.\gradlew assembleRelease
```

Generated outputs will be placed at:
- **App Bundle**: `app/build/outputs/bundle/release/app-release.aab`
- **APK**: `app/build/outputs/apk/release/app-release.apk`

---

## 4. Step 3: Google Play Store Checklist

To publish on Google Play, follow these steps in the [Google Play Console](https://play.google.com/console):

### 1. Account Setup
- Create a Google Play Developer Account (one-time $25 USD registration fee).
- Complete identity verification.

### 2. Store Listing Assets
The Play Console requires the following graphical and textual assets:

| Asset | Specifications | Status |
|---|---|---|
| **App Icon** | 512 x 512 px PNG (32-bit color) | Included in app resources |
| **Feature Graphic** | 1024 x 500 px JPG or 24-bit PNG | Create promotional banner |
| **Screenshots** | Min 2, max 8 phone screenshots (16:9 or 18:9) | Generated in `screenshots/` |
| **Short Description** | Up to 80 characters | Ready (see below) |
| **Full Description** | Up to 4000 characters | Ready (see below) |

#### Suggested Store Listing Copy:
- **App Name**: `iTantra - Offline PTT & Voice`
- **Short Description**: `Offline multilingual push-to-talk speech communication over Wi-Fi and Bluetooth.`
- **Full Description**:
  ```text
  iTantra is an offline multilingual communication application designed for low-bandwidth, disconnected, and disaster-recovery environments.

  KEY CAPABILITIES:
  • 100% Offline Speech Processing: On-device Speech-to-Text and Text-to-Speech without internet access or cloud dependencies.
  • Multilingual Indian Languages: Support for Hindi, Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali, and English.
  • Zero-Cloud Local Mesh: Transmit structured voice-to-text micro-payloads over Wi-Fi Direct and Bluetooth Low Energy.
  • Low Bandwidth & High Reliability: 99%+ bandwidth reduction compared to raw streaming, backed by CRC32 error detection and automatic retransmissions.
  • Privacy First: Zero telemetry collection; all audio data remains securely isolated on your hardware.
  ```

---

## 5. Step 4: Mandatory Policies & Declarations

Because iTantra accesses hardware features, Google Play requires explicit disclosures:

### 1. Privacy Policy (Mandatory)
- Required due to `RECORD_AUDIO`, `NEARBY_WIFI_DEVICES`, and `BLUETOOTH_CONNECT`.
- **Policy Statement**: The app processes audio strictly on-device in local memory; no audio recordings, transcripts, or personal identifiers are uploaded to any external server or third party.
- Host the policy on a public URL (e.g., GitHub Pages: `https://yourusername.github.io/itantra/privacy-policy.html`).

### 2. App Permissions Declaration
When asked in the Play Console:
- **Microphone (`RECORD_AUDIO`)**: "Used solely for real-time speech transcription when the user holds the Push-to-Talk button. Audio is not saved or transmitted off-device."
- **Nearby Devices / Wi-Fi (`NEARBY_WIFI_DEVICES`, `BLUETOOTH_CONNECT`)**: "Used to discover and establish direct peer-to-peer communication with nearby smartphones without internet infrastructure."

### 3. Data Safety Section
In the Data Safety questionnaire:
- Data collected: **No data collected**.
- Data shared: **No data shared with third parties**.
- Data encrypted in transit: **Yes (Local peer frames verified with CRC32)**.
- Can users request data deletion: **Yes (History can be cleared at any time via the trash icon)**.

### 4. 14-Day Testing Requirement (New Personal Accounts)
Google requires new personal developer accounts to run a **Closed Test** with at least **12-20 opt-in testers** for a continuous period of **14 days** before applying for production release.

---

## 6. Alternative: Instant Distribution via GitHub Releases

If you are presenting for a hackathon, submitting to evaluators, or sharing with team members immediately without waiting for Play Store review:

1. Build the release APK:
   ```powershell
   .\gradlew assembleRelease
   ```
2. Navigate to your GitHub repository:
   - Click **Releases** → **Draft a new release**.
   - Set tag: `v1.0.0`.
   - Title: `iTantra v1.0.0 (SIH Demonstration Release)`.
   - Upload `app/build/outputs/apk/release/app-release.apk` (or the debug APK `app-debug.apk`).
3. Anyone can download the APK directly onto their Android phone, tap to install, and test immediately.
