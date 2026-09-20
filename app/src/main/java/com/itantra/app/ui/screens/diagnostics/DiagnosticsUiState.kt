package com.itantra.app.ui.screens.diagnostics

import com.itantra.app.communication.CommunicationDiagnostics

/**
 * UI state representation for the Diagnostics Dashboard.
 */
data class DiagnosticsUiState(
    val diagnostics: CommunicationDiagnostics = CommunicationDiagnostics(),
    val isConnected: Boolean = false,
    val transportName: String = "None",
    val uptimeSeconds: Long = 0L,
    val isResetting: Boolean = false
)
