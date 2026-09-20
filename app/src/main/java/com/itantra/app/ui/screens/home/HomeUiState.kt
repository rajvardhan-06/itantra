package com.itantra.app.ui.screens.home

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.SupportedLanguage

data class HomeUiState(
    val activeLanguage: SupportedLanguage = SupportedLanguage.DEFAULT,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val recentMessages: List<ItantraMessage> = emptyList(),
    val isLoading: Boolean = false,
    val totalMessageCount: Int = 0,
    val isDemoMode: Boolean = true,
    val lastDemoAction: String? = null
)
