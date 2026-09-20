package com.itantra.app.ui.screens.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.data.repository.ConnectionRepositoryImpl
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType
import com.itantra.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val connectionRepository: ConnectionRepository = ConnectionRepositoryImpl(),
    private val appContainer: com.itantra.app.di.AppContainer? = null,
    private val demoSimulationManager: com.itantra.app.demo.DemoSimulationManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConnectionUiState(
            isDemoMode = appContainer?.isDemoMockMode?.value ?: (demoSimulationManager != null)
        )
    )
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                connectionRepository.observeConnectionState(),
                connectionRepository.observeDiscoveredDevices()
            ) { state, devices ->
                state to devices
            }.collect { (state, devices) ->
                _uiState.update { current ->
                    current.copy(
                        connectionState   = state,
                        discoveredDevices = devices,
                        isScanning        = state is ConnectionState.Scanning,
                        errorMessage      = (state as? ConnectionState.Error)?.reason
                    )
                }
            }
        }

        demoSimulationManager?.let { demo ->
            viewModelScope.launch {
                demo.lastSimulatedAction.collect { action ->
                    _uiState.update { it.copy(lastDemoAction = action) }
                }
            }
            viewModelScope.launch {
                demo.isFaultInjected.collect { faulted ->
                    _uiState.update { it.copy(isFaultInjected = faulted) }
                }
            }
        }
    }

    fun selectTransport(transport: TransportType) {
        _uiState.update { it.copy(selectedTransport = transport) }
        if (!_uiState.value.isDemoMode) {
            appContainer?.activateHardwareTransport(transport)
        }
    }

    fun setDemoMode(enable: Boolean) {
        if (enable) {
            appContainer?.activateDemoMode()
        } else {
            appContainer?.activateHardwareTransport(_uiState.value.selectedTransport)
        }
        _uiState.update { it.copy(isDemoMode = enable) }
    }

    fun injectCorruptedPacket() {
        viewModelScope.launch {
            demoSimulationManager?.injectCorruptedPacket()
        }
    }

    fun toggleFaultInjection() {
        val next = !_uiState.value.isFaultInjected
        demoSimulationManager?.toggleSimulatedTransportFailure(next)
        _uiState.update { it.copy(isFaultInjected = next) }
    }

    fun resetDemoSession() {
        viewModelScope.launch {
            demoSimulationManager?.resetDemoSession()
        }
    }

    fun startScan() {
        viewModelScope.launch {
            connectionRepository.startScan(_uiState.value.selectedTransport)
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            connectionRepository.stopScan()
        }
    }

    fun connectToDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            connectionRepository.connectToDevice(device)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionRepository.disconnect()
        }
    }

    fun toggleHostMode() {
        _uiState.update { it.copy(isHostMode = !it.isHostMode) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
