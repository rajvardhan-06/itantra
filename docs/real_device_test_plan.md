# Real-Device Testing & Evaluation Plan — iTantra Phase 10

**Document Purpose**: Standardized testing protocol and field evaluation checklist for validating iTantra on physical Android hardware.  
**Hardware Profile Requirements**:
- Device 1: Low-End Smartphone (e.g. Redmi 9A / Realme C-series, 2GB–3GB RAM, Android 10–12, Cortex-A53).
- Device 2: Mid-Range Smartphone (e.g. Pixel 6a / Samsung Galaxy A-series, 6GB RAM, Android 13–15).

---

## 1. Device Hardware Information Template

| Field | Device A (Host / Low-End) | Device B (Client / Mid-Range) |
|---|---|---|
| **Manufacturer & Model** | *(e.g. Xiaomi Redmi 9A)* | *(e.g. Google Pixel 6a)* |
| **Android OS Version** | *(e.g. Android 10 / API 29)* | *(e.g. Android 14 / API 34)* |
| **SoC / Processor** | *(e.g. MediaTek Helio G25, 8x Cortex-A53)* | *(e.g. Google Tensor G1, 2+2+4)* |
| **RAM** | *(e.g. 2 GB / 3 GB)* | *(e.g. 6 GB)* |
| **Available Storage** | *(e.g. 8.4 GB free)* | *(e.g. 45.2 GB free)* |
| **Active Network** | Local Ad-Hoc Hotspot / Wi-Fi Direct | Local Ad-Hoc Hotspot / Wi-Fi Direct |
| **Bluetooth Chipset** | Bluetooth 5.0 (BR/EDR/LE) | Bluetooth 5.2 (BR/EDR/LE) |

---

## 2. Fifteen Standard Real-Device Test Scenarios

### Test 1: Install APK via ADB or Package Installer
- **Action**: Install `app-debug.apk` using `adb install -r app-debug.apk` or direct file transfer.
- **Expected Result**: Package installs cleanly without manifest signature or parsing errors.
- **Verification Criteria**: App icon and name ("iTantra") appears in launcher.

### Test 2: Cold Application Launch
- **Action**: Launch iTantra from the home screen launcher.
- **Expected Result**: App initializes within < 1.5s on mid-range, < 2.5s on low-end. No blank screens or ANR warnings.
- **Verification Criteria**: First launch displays Onboarding flow; subsequent launches display Home dashboard.

### Test 3: Runtime Permissions (Audio, Bluetooth, Wi-Fi)
- **Action**:
  - Test Denial: Deny microphone permission when prompted. Verify error banner displays "Microphone permission required" with button to open settings.
  - Test Grant: Open app settings, grant permission, return to app. Verify banner dismisses immediately.
- **Verification Criteria**: No crash occurs during denial; permission request prompt re-checks status cleanly.

### Test 4: Audio Recording via Physical Microphone
- **Action**: Press and hold circular Push-to-Talk button for 5 seconds while speaking.
- **Expected Result**: Live audio waveform visually responds to voice dynamics; millisecond counter increments smoothly; haptic click fires on press.
- **Verification Criteria**: Release stops recording cleanly; audio buffer stays well within 1 MiB ceiling.

### Test 5: Offline STT Conversion
- **Action**: Release PTT button after speaking a test sentence ("Water supply needed at sector 2").
- **Expected Result**: Audio transitions through VAD speech-detection, enters STT processing, and outputs transcript into review box.
- **Verification Criteria**: Processing duration is under 500ms; error is reported cleanly if silence was recorded.

### Test 6: Text Review and Interactive Editing
- **Action**: Tap on the editable transcript field in the PTT review card. Edit text using on-screen virtual keyboard.
- **Expected Result**: Virtual keyboard shifts UI without clipping Send/Cancel buttons or obscuring text cursor.
- **Verification Criteria**: User edits persist into the outgoing `TextMessage.text` payload.

### Test 7: Outgoing Message Transmission (Wi-Fi Socket / Bluetooth)
- **Action**: Select Priority (`NORMAL`, `HIGH`, or `EMERGENCY`). Tap "Send Transmission".
- **Expected Result**: Status transitions from `PENDING` -> `SENDING`. Frame header (4 bytes) and CRC32 payload traverse the radio transport.
- **Verification Criteria**: If `EMERGENCY` is tapped, modal confirmation dialog appears requiring explicit confirmation.

### Test 8: Remote Message Reception
- **Action**: Receive packet on peer device.
- **Expected Result**: Receiving device detects 4-byte frame header, validates CRC32 integrity, emits `ACK:<id>` packet back to sender, and saves message.
- **Verification Criteria**: Sender status updates to `DELIVERED` within < 100ms over Wi-Fi.

### Test 9: Offline Text-to-Speech (TTS) Announcement
- **Action**: Observe receiving device behavior upon message arrival.
- **Expected Result**: Receiving device automatically speaks the incoming message using local offline TTS engine.
- **Verification Criteria**: Urgent/Alert messages play immediately with audio ducking or high queue priority.

### Test 10: Message History & Local Audit Log
- **Action**: Navigate to **History** tab on both devices.
- **Expected Result**: All sent and received messages appear in reverse chronological order with priority badges, timestamps, and status chips.
- **Verification Criteria**: Tapping the speaker icon replays the message audio via TTS.

### Test 11: Language Switching & Model Availability
- **Action**: Navigate to **Language** tab. Switch active language from English to Hindi.
- **Expected Result**: Active language updates in Home and PTT headers. Model availability status is displayed honestly.
- **Verification Criteria**: Transcriptions and TTS synthesize in selected language script.

### Test 12: Network Disconnection & Out-of-Range Handling
- **Action**: Turn off Wi-Fi on Host phone while Client phone sends a message.
- **Expected Result**: Message transitions to `SENDING`, attempts 3 exponential backoff retries, and transitions to `FAILED`.
- **Verification Criteria**: No application crash occurs; Retry button becomes visible.

### Test 13: Transport Recovery & Reconnection
- **Action**: Turn Wi-Fi back on. Tap "Retry" on the failed message.
- **Expected Result**: Message resends, arrives at peer, receives ACK, and transitions to `DELIVERED`.
- **Verification Criteria**: Diagnostics screen registers delivery success and recalculates RTT latency.

### Test 14: Dark / Light Mode Theme Switching
- **Action**: In Settings, toggle theme between System, Light, and Dark.
- **Expected Result**: Theme transitions instantly without activity recreation or UI flickering. Text contrast remains accessible.
- **Verification Criteria**: Color tokens adhere to Material 3 dark/light palettes.

### Test 15: Application Process Termination & State Restoration
- **Action**: Background the app, kill the process via Android task manager, and relaunch.
- **Expected Result**: App restarts cleanly. History messages, active language, and settings remain intact.
- **Verification Criteria**: DataStore preferences and local message storage load immediately.
