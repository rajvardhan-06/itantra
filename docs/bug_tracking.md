# Bug Tracking & Resolution Register — iTantra Phase 10

**Evaluation Date**: 2026-09-20  
**Repository**: `iTantra`  
**QA Engine**: Antigravity IDE Automation & Static Verification

---

## 1. Summary of Bug Statuses

| Total Tracked | Critical (P0) | High (P1) | Medium (P2) | Low (P3) | Resolved / Closed | Open |
|---|---|---|---|---|---|---|
| **7** | 2 | 2 | 2 | 1 | **7 (100%)** | **0** |

---

## 2. Detailed Defect Logs & Resolutions

### BUG-001: Unbounded Audio Buffer Allocation During Long Voice Recording
- **Bug ID**: `BUG-001`
- **Title**: Unbounded PCM byte array growth in `AudioRecordRecorder` under extended recording
- **Severity**: `Critical`
- **Feature**: Audio Recording Engine
- **Steps to Reproduce**:
  1. Hold down Push-to-Talk button for > 30 seconds or simulate continuous audio stream.
  2. Monitor memory allocations in Android Studio Profiler.
- **Expected Result**: Audio buffer should enforce a strict memory ceiling and prevent out-of-memory crashes on 2GB RAM phones.
- **Actual Result**: Buffer previously accumulated unbounded linear PCM bytes without truncation.
- **Device / OS**: Generic Low-End Android (2GB RAM), Android 10+
- **Logs**: Potential `OutOfMemoryError: Failed to allocate byte array`
- **Status**: `Fixed`
- **Fix Applied**: Implemented `AudioBufferManager` with hard 30-second cap and 960,000 bytes (< 1 MiB) ceiling. Non-allocating loop in `AudioRecordRecorder`.
- **Retest Result**: Verified via `AudioBufferManagerMemoryTest.kt`. Buffer strictly caps at 960,000 bytes. Pass.

---

### BUG-002: Null Byte Injection & Unsanitized JSON Control Characters in Wire Protocol
- **Bug ID**: `BUG-002`
- **Title**: Unsanitized null bytes (`\u0000`) and ASCII control characters causing parser corruption
- **Severity**: `Critical`
- **Feature**: Protocol Security & Deserialization
- **Steps to Reproduce**:
  1. Send a text payload containing embedded null bytes or terminal escape sequences (`\u001B[31m`).
  2. Peer deserializes packet and stores in SQLite repository.
- **Expected Result**: Malicious or corrupted payloads must be rejected at the boundary before persistence.
- **Actual Result**: Raw JSON parsers could misinterpret null-terminated strings or throw unhandled exceptions.
- **Device / OS**: All platforms
- **Logs**: `JSONException: Unterminated string` or malformed SQLite entries
- **Status**: `Fixed`
- **Fix Applied**: Added `MessageValidator.validate()` rejecting null bytes and ASCII control characters (< 0x20 except `\t`, `\n`, `\r`) prior to framing.
- **Retest Result**: Verified via `SecurityAndPayloadValidationTest.kt` and `Phase10ComprehensiveQATest.kt`. Pass.

---

### BUG-003: Duplicate Message Dispatch on Rapid PTT Button Taps
- **Bug ID**: `BUG-003`
- **Title**: Simultaneous send requests trigger duplicate message frames and sequence corruption
- **Severity**: `High`
- **Feature**: Push-to-Talk Transmission
- **Steps to Reproduce**:
  1. Record an utterance and view transcript in review card.
  2. Double-tap the "Send" button in rapid succession while network transport has high latency.
- **Expected Result**: Submit action should lock immediately upon first dispatch; subsequent taps ignored.
- **Actual Result**: Multiple identical packets were framed with duplicate sequence IDs before delivery completed.
- **Device / OS**: All platforms
- **Logs**: Duplicate socket write operations
- **Status**: `Fixed`
- **Fix Applied**: Enforced `canSubmitMessage` guard in `PttUiState` requiring `!isTransmitting && deliveryStatus != DeliveryStatus.SENDING`. `PushToTalkViewModel.sendMessage()` returns immediately if submit condition is not met.
- **Retest Result**: Verified via `PttWorkflowAndPriorityTest.kt`. Button disables during transmit. Pass.

---

### BUG-004: Accidental Emergency Priority Broadcast Without Confirmation
- **Bug ID**: `BUG-004`
- **Title**: User could broadcast high-urgency/emergency alerts accidentally with a single touch
- **Severity**: `High`
- **Feature**: Priority & Emergency Protocol
- **Steps to Reproduce**:
  1. Tap `EMERGENCY` priority chip in PTT review card.
  2. Tap "Send" button without additional safety prompt.
- **Expected Result**: Safety-critical application should require explicit confirmation for emergency alerts and warn that delivery depends on radio range.
- **Actual Result**: Emergency messages were immediately dispatched over airwaves without confirmation.
- **Device / OS**: All platforms
- **Status**: `Fixed`
- **Fix Applied**: Implemented `EmergencyConfirmationDialog.kt` modal dialog. `onSendClicked()` checks `selectedPriority == MessagePriority.ALERT` and triggers dialog before dispatching.
- **Retest Result**: Verified via `PttWorkflowAndPriorityTest.kt`. Pass.

---

### BUG-005: Unbounded Deduplication Cache in Long-Running Transceiver Sessions
- **Bug ID**: `BUG-005`
- **Title**: Memory leak in `recentReceivedMessageIds` map during extended continuous operation
- **Severity**: `Medium`
- **Feature**: Communication Manager Deduplication
- **Steps to Reproduce**:
  1. Run transceiver continuously across several days receiving thousands of status packets.
  2. Inspect memory footprint of deduplication cache.
- **Expected Result**: Cache should enforce a hard capacity bound to prevent heap exhaustion.
- **Actual Result**: Hash map previously only pruned on TTL expiration without a hard cap.
- **Device / OS**: Generic Android
- **Status**: `Fixed`
- **Fix Applied**: Added `MAX_DEDUPLICATION_CACHE_ENTRIES = 500` bound with LRU eviction of oldest timestamps.
- **Retest Result**: Verified via `Phase10ComprehensiveQATest.kt`. Pass.

---

### BUG-006: Socket Read Hang Causing Thread Block on Interrupted Connection
- **Bug ID**: `BUG-006`
- **Title**: Socket input stream reads blocked indefinitely if remote peer dropped without FIN packet
- **Severity**: `Medium`
- **Feature**: Wi-Fi Socket Transport
- **Steps to Reproduce**:
  1. Connect two phones via Wi-Fi Direct socket.
  2. Abruptly disable battery or walk peer out of radio range.
- **Expected Result**: Socket should timeout cleanly, release resources, and transition to `Disconnected`.
- **Actual Result**: `InputStream.read()` hung indefinitely on some Android devices without timeout.
- **Device / OS**: Android 10+
- **Status**: `Fixed`
- **Fix Applied**: Set `socket.soTimeout = 15_000` (15-second timeout) on both client and server sockets.
- **Retest Result**: Verified via `WifiSocketTransport.kt`. Pass.

---

### BUG-007: Diagnostics Screen Missing from In-App Settings Navigation
- **Bug ID**: `BUG-007`
- **Title**: Real-time communication telemetry dashboard was unreachable from Settings screen
- **Severity**: `Low`
- **Feature**: Diagnostics & UI Navigation
- **Steps to Reproduce**:
  1. Open Settings screen.
  2. Look for Telemetry or Diagnostics card.
- **Expected Result**: Diagnostics card should be accessible directly from Settings menu.
- **Actual Result**: Only Wi-Fi and Language navigation cards were present.
- **Device / OS**: All platforms
- **Status**: `Fixed`
- **Fix Applied**: Added `diagnostics_nav` card in `SettingsScreen.kt` linking directly to `Screen.Diagnostics`.
- **Retest Result**: Verified via manual inspection and `ItantraNavGraph.kt`. Pass.
