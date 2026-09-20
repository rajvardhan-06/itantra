package com.itantra.app.ui.screens.settings

import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.domain.model.ThemePreference

data class SettingsUiState(
    val activeLanguage: SupportedLanguage = SupportedLanguage.DEFAULT,
    val communicationMode: CommunicationMode = CommunicationMode.PUSH_TO_TALK,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val incomingNotifications: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsAutoPlay: Boolean = false
)
