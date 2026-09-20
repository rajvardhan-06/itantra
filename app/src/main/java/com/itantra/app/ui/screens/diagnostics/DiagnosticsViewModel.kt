package com.itantra.app.ui.screens.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.communication.CommunicationManager
import com.itantra.app.domain.model.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating real-time communication telemetry, latency measurements,
 * packet counters, and CRC32 verification stats.
 */
class DiagnosticsViewModel(
    private val communicationManager: CommunicationManager
) : ViewModel() {

    private val _isResetting = MutableStateFlow(false)
    private val _uptimeTicker = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uptimeTicker.value += 1L
            }
        }
    }

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        communicationManager.diagnostics,
        communicationManager.connectionState,
        _uptimeTicker,
        _isResetting
    ) { diag, connState, _, resetting ->
        val isConn = connState is ConnectionState.Connected
        val uptimeSec = (System.currentTimeMillis() - diag.sessionStartedTimestampMs) / 1000L
        DiagnosticsUiState(
            diagnostics = diag,
            isConnected = isConn,
            transportName = diag.activeTransportType,
            uptimeSeconds = if (uptimeSec >= 0) uptimeSec else 0L,
            isResetting = resetting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiagnosticsUiState()
    )

    fun resetDiagnostics() {
        viewModelScope.launch {
            _isResetting.value = true
            communicationManager.resetDiagnostics()
            delay(200L)
            _isResetting.value = false
        }
    }
}
