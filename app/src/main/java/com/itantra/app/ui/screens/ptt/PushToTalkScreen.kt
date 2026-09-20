package com.itantra.app.ui.screens.ptt

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import com.itantra.app.communication.DeliveryStatus
import com.itantra.app.communication.MessagePriority
import com.itantra.app.ui.theme.StatusWarning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.itantra.app.R
import com.itantra.app.audio.RecordingState
import com.itantra.app.audio.vad.VadState
import com.itantra.app.stt.SttLanguage
import com.itantra.app.stt.SttState
import com.itantra.app.ui.components.ConnectionStatusBadge
import com.itantra.app.ui.components.PttButton
import com.itantra.app.ui.theme.StatusError
import com.itantra.app.ui.theme.StatusOnline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushToTalkScreen(viewModel: PushToTalkViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // ── Permission Request Launcher ──────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val permanentlyDenied = if (!isGranted && activity != null) {
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        } else {
            false
        }
        viewModel.onPermissionResult(isGranted, permanentlyDenied)
    }

    // Check initial permission state on launch
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.ptt_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.activeLanguage.nativeName}  ·  ${uiState.activeLanguage.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        if (uiState.isDemoMode) {
                            com.itantra.app.ui.components.DemoModeBadge()
                        }
                        ConnectionStatusBadge(
                            connectionState = uiState.connectionState
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

            // ── SIH Demo Simulation Bar (Single-device loopback testing) ──────
            if (uiState.isDemoMode) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "SIH Demo: Simulate Remote Peer",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Loopback",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.simulateIncomingPeerMessage(0) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Hindi", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { viewModel.simulateIncomingPeerMessage(1) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Bengali", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { viewModel.simulateIncomingPeerMessage(2) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("English", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // ── Permission Warning Banner (if denied) ─────────────────────────
            if (!uiState.hasMicPermission) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusError.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MicOff,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isMicPermanentlyDenied) {
                                    stringResource(R.string.ptt_permission_permanently_denied)
                                } else {
                                    stringResource(R.string.ptt_permission_required)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            if (uiState.isMicPermanentlyDenied) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.ptt_permission_settings), style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.onRequestingPermission()
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.ptt_permission_grant), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // ── Model Missing Warning Banner (if model not installed) ─────────
            val activeSttLang = SttLanguage.fromSupportedLanguage(uiState.activeLanguage)
            if (uiState.sttState is SttState.ModelNotInstalled) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.stt_model_missing, uiState.activeLanguage.displayName),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Model package: ${activeSttLang.formattedModelSize}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.installActiveModel() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.stt_model_install), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // ── Status / Instruction Text (VAD & STT-Integrated) ───────────────
            AnimatedContent(
                targetState = Triple(uiState.recordingState, uiState.vadState, uiState.sttState),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ptt_status_text"
            ) { (recState, vadState, sttState) ->
                val statusText = when {
                    sttState is SttState.Recognizing -> stringResource(R.string.stt_recognizing)
                    sttState is SttState.ResultAvailable -> "Speech Transcribed"
                    sttState is SttState.NoSpeechDetected -> stringResource(R.string.stt_no_speech)
                    sttState is SttState.ModelNotInstalled -> stringResource(R.string.stt_model_missing, uiState.activeLanguage.displayName)
                    sttState is SttState.UnsupportedLanguage -> stringResource(R.string.stt_model_unsupported, uiState.activeLanguage.displayName)
                    recState is RecordingState.Recording -> when (vadState) {
                        VadState.IDLE -> stringResource(R.string.ptt_idle_hint)
                        VadState.POSSIBLE_SPEECH -> stringResource(R.string.vad_listening)
                        VadState.SPEECH_DETECTED -> stringResource(R.string.vad_speech_detected)
                        VadState.POSSIBLE_SILENCE -> stringResource(R.string.vad_possible_silence)
                        VadState.SILENCE_DETECTED -> stringResource(R.string.vad_silence_detected)
                        VadState.COMPLETED -> stringResource(R.string.vad_speech_captured)
                        VadState.ERROR -> stringResource(R.string.vad_error)
                    }
                    recState is RecordingState.RequestingPermission -> "Requesting microphone permission…"
                    recState is RecordingState.Ready -> stringResource(R.string.ptt_idle_hint)
                    recState is RecordingState.Stopping -> stringResource(R.string.ptt_stopping)
                    recState is RecordingState.Completed -> stringResource(R.string.vad_speech_captured)
                    recState is RecordingState.Cancelled -> "Recording cancelled"
                    recState is RecordingState.Error -> recState.message.ifBlank { stringResource(R.string.ptt_recording_failed) }
                    else -> stringResource(R.string.ptt_idle_hint)
                }

                val statusColor = when {
                    recState is RecordingState.Error || sttState is SttState.Error -> StatusError
                    sttState is SttState.ResultAvailable || recState is RecordingState.Completed -> StatusOnline
                    recState is RecordingState.Recording && vadState == VadState.SPEECH_DETECTED -> StatusOnline
                    recState is RecordingState.Recording || sttState is SttState.Recognizing -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = statusColor
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Duration / Waveform / Speech Indicator Section ────────────────
            when (val state = uiState.recordingState) {
                is RecordingState.Recording -> {
                    val durationSeconds = state.durationMs / 1000
                    val durationMsFraction = (state.durationMs % 1000) / 100

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format("%02d.%d s / 30s", durationSeconds, durationMsFraction),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        // Speech Activity Badge
                        val isSpeaking = uiState.vadState == VadState.SPEECH_DETECTED
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSpeaking) StatusOnline.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = if (isSpeaking) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeaking) "Speech Active" else "Listening",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSpeaking) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.audioLevel },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (uiState.vadState == VadState.SPEECH_DETECTED) StatusOnline else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                is RecordingState.Stopping -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> Spacer(Modifier.height(28.dp))
            }

            Spacer(Modifier.weight(1f))

            // ── Main PTT Microphone Button ────────────────────────────────────
            val isActivelyRecording = uiState.recordingState is RecordingState.Recording
            val isRecognizing = uiState.sttState is SttState.Recognizing
            val buttonEnabled = uiState.recordingState !is RecordingState.Stopping && !isRecognizing

            PttButton(
                isRecording = isActivelyRecording,
                onPress = {
                    if (uiState.hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        viewModel.onRequestingPermission()
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onRelease = {
                    if (isActivelyRecording) {
                        viewModel.stopRecording()
                    }
                },
                size = 140.dp,
                enabled = buttonEnabled
            )

            Spacer(Modifier.height(16.dp))

            // ── Context Label Under PTT Button ────────────────────────────────
            Text(
                text = if (isActivelyRecording) {
                    stringResource(R.string.ptt_tap_to_stop)
                } else {
                    stringResource(R.string.ptt_tap_to_record)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Cancel Action Button (Visible while recording) ────────────────
            AnimatedVisibility(
                visible = isActivelyRecording,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelRecording() },
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.ptt_cancel_recording), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.weight(1f))

            // ── STT Transcription / Audio Confirmation Card ───────────────────
            val capturedResult = uiState.capturedAudioResult
            val sttResult = uiState.sttResult

            when {
                // 1. In-progress Offline Speech-to-Text inference
                uiState.sttState is SttState.Recognizing -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.stt_recognizing),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Offline Neural Acoustic Model · ${activeSttLang.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Transcription result available (Speech recognized!)
                uiState.sttState is SttState.ResultAvailable || (uiState.recordingState is RecordingState.Completed && uiState.editableTranscription.isNotBlank()) -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusOnline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.stt_transcription_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOnline
                                )
                                Spacer(Modifier.weight(1f))
                                if (sttResult?.confidence != null) {
                                    val confPercent = (sttResult.confidence * 100).toInt()
                                    Text(
                                        text = stringResource(R.string.stt_confidence, confPercent),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "16kHz · Mono · Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.editableTranscription,
                                onValueChange = { viewModel.onTranscriptionEdited(it) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyLarge,
                                supportingText = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Tap to edit transcribed text")
                                        Text("${uiState.editableTranscription.length} chars")
                                    }
                                }
                            )

                            if (capturedResult != null) {
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Audio: ${capturedResult.formattedDuration} · ${capturedResult.formattedSize}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (sttResult != null) {
                                        Text(
                                            text = stringResource(R.string.stt_latency, sttResult.durationMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            PrioritySelectorRow(
                                selectedPriority = uiState.selectedPriority,
                                onPrioritySelected = { viewModel.selectPriority(it) }
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.clearRecording() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.ptt_clear))
                                }
                                Button(
                                    onClick = { viewModel.onSendClicked() },
                                    modifier = Modifier.weight(1f),
                                    enabled = uiState.canSubmitMessage
                                ) {
                                    if (uiState.isTransmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Sending…")
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.ptt_send))
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. No speech detected in audio capture
                uiState.sttState is SttState.NoSpeechDetected -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "No Speech Detected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.stt_no_speech),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.clearRecording() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.ptt_clear))
                            }
                        }
                    }
                }

                // 4. Fallback completed audio card (if STT state is idle or uninitialized)
                uiState.recordingState is RecordingState.Completed && capturedResult != null -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusOnline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.ptt_audio_captured),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOnline
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "16kHz · Mono · 16-bit PCM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Duration: ${capturedResult.formattedDuration} (${capturedResult.durationSeconds}s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Payload: ${capturedResult.formattedSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            PrioritySelectorRow(
                                selectedPriority = uiState.selectedPriority,
                                onPrioritySelected = { viewModel.selectPriority(it) }
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.clearRecording() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.ptt_clear))
                                }
                                Button(
                                    onClick = { viewModel.onSendClicked() },
                                    modifier = Modifier.weight(1f),
                                    enabled = uiState.canSubmitMessage
                                ) {
                                    if (uiState.isTransmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Sending…")
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.ptt_send))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Phase 5: Transmission Delivery Status Banner ─────────────────
            if (uiState.deliveryStatus != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (uiState.deliveryStatus) {
                            DeliveryStatus.DELIVERED -> StatusOnline.copy(alpha = 0.12f)
                            DeliveryStatus.FAILED -> StatusError.copy(alpha = 0.12f)
                            DeliveryStatus.SENDING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (uiState.deliveryStatus) {
                            DeliveryStatus.PENDING -> {
                                Icon(Icons.Filled.HourglassEmpty, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.delivery_status_pending), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            DeliveryStatus.SENDING -> {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.delivery_status_sending), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            DeliveryStatus.SENT -> {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.delivery_status_sent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            DeliveryStatus.DELIVERED -> {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusOnline, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.delivery_status_delivered), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = StatusOnline)
                            }
                            DeliveryStatus.FAILED -> {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.delivery_status_failed), style = MaterialTheme.typography.bodySmall, color = StatusError, modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { viewModel.retrySendMessage() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.comm_retry_transmission), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            null -> Unit
                        }
                    }
                }
            }

            // ── Error Banner & Dismiss Action ─────────────────────────────────
            if (uiState.recordingState is RecordingState.Error) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = StatusError)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = uiState.errorMessage ?: stringResource(R.string.ptt_recording_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { viewModel.dismissError() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        if (uiState.showEmergencyConfirmation) {
            com.itantra.app.ui.components.EmergencyConfirmationDialog(
                messageSnippet = uiState.editableTranscription.take(60),
                onConfirm = { viewModel.confirmEmergencySend() },
                onDismiss = { viewModel.dismissEmergencyConfirmation() }
            )
        }
    }
}

@Composable
private fun PrioritySelectorRow(
    selectedPriority: MessagePriority,
    onPrioritySelected: (MessagePriority) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.protocol_priority_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            MessagePriority.entries.forEach { priority ->
                val isSelected = selectedPriority == priority
                val (label, chipColor) = when (priority) {
                    MessagePriority.NORMAL -> "NORMAL" to MaterialTheme.colorScheme.primary
                    MessagePriority.IMPORTANT -> "HIGH" to StatusWarning
                    MessagePriority.ALERT -> "EMERGENCY" to StatusError
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) chipColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onPrioritySelected(priority) }
                        .semantics {
                            role = androidx.compose.ui.semantics.Role.RadioButton
                            this.selected = isSelected
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Note: Priority orders local dispatch queue & triggers recipient speech alert. Delivery is subject to peer radio range.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

