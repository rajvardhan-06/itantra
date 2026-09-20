package com.itantra.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.data.repository.LanguageRepositoryImpl
import com.itantra.app.data.repository.SettingsRepositoryImpl
import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.domain.model.ThemePreference
import com.itantra.app.domain.repository.LanguageRepository
import com.itantra.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(),
    private val languageRepository: LanguageRepository = LanguageRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                languageRepository.observeActiveLanguage(),
                settingsRepository.observeCommunicationMode(),
                settingsRepository.observeThemePreference(),
                settingsRepository.observeIncomingNotifications(),
                settingsRepository.observeTtsSpeechRate(),
                settingsRepository.observeTtsPitch(),
                settingsRepository.observeTtsAutoPlay()
            ) { args: Array<Any> ->
                SettingsUiState(
                    activeLanguage        = args[0] as SupportedLanguage,
                    communicationMode     = args[1] as CommunicationMode,
                    themePreference       = args[2] as ThemePreference,
                    incomingNotifications = args[3] as Boolean,
                    ttsSpeechRate         = args[4] as Float,
                    ttsPitch              = args[5] as Float,
                    ttsAutoPlay           = args[6] as Boolean
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun setCommunicationMode(mode: CommunicationMode) {
        viewModelScope.launch { settingsRepository.setCommunicationMode(mode) }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { settingsRepository.setThemePreference(theme) }
    }

    fun setIncomingNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setIncomingNotifications(enabled) }
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch { settingsRepository.setTtsSpeechRate(rate) }
    }

    fun setTtsPitch(pitch: Float) {
        viewModelScope.launch { settingsRepository.setTtsPitch(pitch) }
    }

    fun setTtsAutoPlay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTtsAutoPlay(enabled) }
    }
}
