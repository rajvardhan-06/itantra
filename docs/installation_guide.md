# iTantra — Android Installation & Sideloading Guide

This guide provides step-by-step instructions for sideloading the **iTantra Android APK** (`iTantra-v1.0.0.apk`) onto physical Android devices, along with an end-to-end device validation checklist.

---

## 1. Quick Installation (For Non-Technical Users)

### Step 1: Download the APK
1. Open the official iTantra website on your Android phone (e.g. `https://itantra.vercel.app`).
2. Tap the **"Download APK v1.0.0"** button.
3. If Chrome or Samsung Internet shows a prompt:
   > *"File might be harmful. Do you want to download iTantra-v1.0.0.apk anyway?"*
   
   Tap **"Download anyway"**.
   *(This is standard Android security behavior for all applications downloaded directly via a web browser outside the Google Play Store).*

### Step 2: Open and Allow Unknown Apps
1. Once download finishes, tap **"Open"** from the browser download bar or find `iTantra-v1.0.0.apk` in your phone's **Files / Downloads** app.
2. If Android displays:
   > *"For your security, your phone is currently not allowed to install unknown apps from this source."*
3. Tap **"Settings"**.
4. Toggle on **"Allow from this source"** (e.g. allow Chrome or Files).
5. Tap the **Back** arrow.

### Step 3: Complete Installation
1. Tap **"Install"** on the system package installer prompt.
2. If Google Play Protect displays a scan prompt:
   > *"Unrecognized app details. Send app for scanning?"*
   
   Tap **"Don't send"** or **"Install anyway"**.
3. Tap **"Open"** once installation completes.

### Step 4: Grant Permissions
When iTantra launches for the first time:
1. **Microphone (`RECORD_AUDIO`):** Tap **"While using the app"** to allow Push-to-Talk voice transcription.
2. **Nearby Devices / Bluetooth:** Tap **"Allow"** to enable peer-to-peer discovery and message exchange without cellular internet.
3. **Notifications (Android 13+):** Tap **"Allow"** to keep communication background services active.

---

## 2. Real-Device Testing & Validation Protocol (QA Checklist)

For engineers and testers validating release candidate builds on physical hardware:

### Test Environment Requirements
- **Device A:** Physical Android smartphone (Android 10 - Android 14) with Wi-Fi and Bluetooth enabled.
- **Device B:** Second physical Android smartphone or Android Emulator for peer-to-peer testing.
- **Connectivity:** Cellular data and external internet disabled (Airplane mode with Wi-Fi and Bluetooth re-enabled) to verify pure offline capability.

### Validation Matrix

| Phase | Test Item | Verification Procedure | Expected Result | Verified Status |
|---|---|---|---|---|
| **Installation** | Fresh APK Install | Install via `adb install dist\iTantra-v1.0.0.apk` or sideload from browser | Clean installation without signature mismatch | Verified (Emulator & Signed APK) |
| **Upgrade** | Over-the-air Overwrite | Install v1.0.0 over older debug/dev build | Version upgraded, local SQLite Room history preserved | Requires device test |
| **Launch** | Cold Boot & Splash | Launch app from home launcher | Splash screen displays cleanly, routes to Home Screen within 2 seconds | Verified |
| **Permissions** | Runtime Prompts | Navigate to Push-to-Talk and tap microphone | Dialog requests `RECORD_AUDIO` with explanation; handles Deny and Allow gracefully | Verified |
| **STT Engine** | Hindi Voice Input | Hold PTT button, speak clear Hindi phrase | Real-time waveform responds; transcribes speech to text offline | Verified (Inference engine active) |
| **TTS Engine** | Voice Playback | Tap speaker icon next to transcribed text | System TTS engine synthesizes audio in selected target language | Verified (Android TTS active) |
| **Local P2P** | Direct Transport | Connect Device A and Device B via Wi-Fi Direct or Bluetooth LE | Devices discover each other, negotiate socket, and exchange text message | Tested via loopback / Requires 2 physical devices |
| **Persistence** | Message History | Send 3 messages, kill app process, relaunch app | All 3 messages remain visible with timestamps and delivery flags | Verified |
| **Uninstall** | Clean Removal | Long-press app icon &rarr; Uninstall | App uninstalls completely, local database and cache purged | Verified |

---

## 3. Known Limitations & Scope Disclosures

To ensure transparency with end users and evaluators:
1. **Physical P2P Range:** Direct Wi-Fi Direct communication range is typically 30–70 meters line-of-sight; Bluetooth LE is 10–25 meters. Neither replaces cellular infrastructure over multi-kilometer distances unless connected via intermediate mesh relays.
2. **Language Model Tiers:** Hindi, Bengali, and English feature full voice pipeline support; Gujarati, Marathi, Tamil, Telugu, Kannada, Malayalam, and Odia user interfaces are ready, while high-accuracy acoustic models are actively being trained.
3. **Low-RAM Devices:** On devices with less than 2 GB RAM, high-memory neural inference models are automatically throttled to preserve system stability.
