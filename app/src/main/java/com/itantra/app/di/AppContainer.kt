package com.itantra.app.di

import android.content.Context
import com.itantra.app.audio.AudioRecordRecorder
import com.itantra.app.audio.AudioRecorder
import com.itantra.app.audio.vad.EnergyBasedVoiceActivityDetector
import com.itantra.app.audio.vad.VoiceActivityDetector
import com.itantra.app.communication.BluetoothTransport
import com.itantra.app.communication.CommunicationManager
import com.itantra.app.communication.CommunicationTransport
import com.itantra.app.communication.MockCommunicationTransport
import com.itantra.app.communication.WifiSocketTransport
import com.itantra.app.data.repository.ConnectionRepositoryImpl
import com.itantra.app.data.repository.LanguageRepositoryImpl
import com.itantra.app.data.repository.MessageRepositoryImpl
import com.itantra.app.data.repository.SettingsRepositoryImpl
import com.itantra.app.domain.model.TransportType
import com.itantra.app.domain.repository.ConnectionRepository
import com.itantra.app.domain.repository.LanguageRepository
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.domain.repository.SettingsRepository
import com.itantra.app.stt.MockSpeechToTextEngine
import com.itantra.app.stt.SpeechToTextEngine
import com.itantra.app.stt.model.LocalModelManager
import com.itantra.app.stt.model.ModelManager
import com.itantra.app.tts.AndroidTextToSpeechEngine
import com.itantra.app.tts.TextToSpeechEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dependency Injection Container for the iTantra application.
 *
 * Provides shared singleton instances for repositories, engines, and communication
 * transports across all Compose screens and ViewModels.
 *
 * This architecture guarantees:
 * 1. Single source of truth for message history, active language, and settings.
 * 2. Synchronized connection state between Home, Connection, and PushToTalk screens.
 * 3. Transparent switching between simulated mock demo mode and live Wi-Fi / Bluetooth transports.
 */
class AppContainer(val context: Context) {

    // Repositories
    val messageRepository: MessageRepository by lazy { MessageRepositoryImpl() }
    val languageRepository: LanguageRepository by lazy { LanguageRepositoryImpl() }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl() }

    // Transports
    val mockTransport: MockCommunicationTransport by lazy { MockCommunicationTransport(autoEchoAck = true) }
    val wifiTransport: WifiSocketTransport by lazy { WifiSocketTransport() }
    val bluetoothTransport: BluetoothTransport by lazy { BluetoothTransport(context) }

    // Active Transport State
    private val _activeTransportType = MutableStateFlow(TransportType.WIFI_DIRECT)
    val activeTransportType: StateFlow<TransportType> = _activeTransportType.asStateFlow()

    private val _isDemoMockMode = MutableStateFlow(true)
    val isDemoMockMode: StateFlow<Boolean> = _isDemoMockMode.asStateFlow()

    // Communication Coordinator
    val communicationManager: CommunicationManager by lazy {
        CommunicationManager(
            transport = mockTransport,
            messageRepository = messageRepository
        )
    }

    // Connection Repository linked directly to the CommunicationManager
    val connectionRepository: ConnectionRepository by lazy {
        ConnectionRepositoryImpl(communicationManager = communicationManager)
    }

    // Audio & Offline AI Engines
    val modelManager: ModelManager by lazy { LocalModelManager(context) }
    val audioRecorder: AudioRecorder by lazy { AudioRecordRecorder(context) }
    val voiceActivityDetector: VoiceActivityDetector by lazy { EnergyBasedVoiceActivityDetector() }
    val sttEngine: SpeechToTextEngine by lazy { MockSpeechToTextEngine() }
    val ttsEngine: TextToSpeechEngine by lazy { AndroidTextToSpeechEngine(context) }

    // SIH Demonstration & Fault Simulation Manager
    val demoSimulationManager: com.itantra.app.demo.DemoSimulationManager by lazy {
        com.itantra.app.demo.DemoSimulationManager(
            communicationManager = communicationManager,
            mockTransport = mockTransport,
            messageRepository = messageRepository
        )
    }

    /**
     * Activates Demo Loopback mode using [MockCommunicationTransport].
     */
    fun activateDemoMode() {
        communicationManager.setTransport(mockTransport)
        _isDemoMockMode.value = true
    }

    /**
     * Activates Live Hardware Transport (Wi-Fi Direct TCP Sockets or Bluetooth SPP).
     */
    fun activateHardwareTransport(type: TransportType) {
        _activeTransportType.value = type
        val targetTransport: CommunicationTransport = when (type) {
            TransportType.WIFI_DIRECT -> wifiTransport
            TransportType.BLUETOOTH -> bluetoothTransport
        }
        communicationManager.setTransport(targetTransport)
        _isDemoMockMode.value = false
    }
}
