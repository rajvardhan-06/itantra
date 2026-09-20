package com.itantra.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.data.repository.ConnectionRepositoryImpl
import com.itantra.app.data.repository.LanguageRepositoryImpl
import com.itantra.app.data.repository.MessageRepositoryImpl
import com.itantra.app.domain.repository.ConnectionRepository
import com.itantra.app.domain.repository.LanguageRepository
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.demo.DemoSimulationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val messageRepository: MessageRepository = MessageRepositoryImpl(),
    private val languageRepository: LanguageRepository = LanguageRepositoryImpl(),
    private val connectionRepository: ConnectionRepository = ConnectionRepositoryImpl(),
    private val demoSimulationManager: DemoSimulationManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isDemoMode = demoSimulationManager != null))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeState()
        observeDemoManager()
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                languageRepository.observeActiveLanguage(),
                connectionRepository.observeConnectionState(),
                messageRepository.observeMessages()
            ) { language, connection, messages ->
                Triple(language, connection, messages)
            }.collect { (language, connection, messages) ->
                _uiState.update { current ->
                    current.copy(
                        activeLanguage    = language,
                        connectionState   = connection,
                        recentMessages    = messages.takeLast(3),
                        totalMessageCount = messages.size
                    )
                }
            }
        }
    }

    private fun observeDemoManager() {
        demoSimulationManager?.let { demo ->
            viewModelScope.launch {
                demo.lastSimulatedAction.collect { action ->
                    _uiState.update { it.copy(lastDemoAction = action) }
                }
            }
        }
    }

    fun simulateIncomingMessage(presetIndex: Int = 0) {
        viewModelScope.launch {
            demoSimulationManager?.simulateIncomingPeerMessage(presetIndex)
        }
    }

    fun resetDemoSession() {
        viewModelScope.launch {
            demoSimulationManager?.resetDemoSession()
        }
    }
}
