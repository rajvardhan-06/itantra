package com.itantra.app.data.repository

import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.domain.model.ThemePreference
import com.itantra.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of [SettingsRepository].
 * Replace with a DataStore-backed implementation for persistence across app restarts.
 */
class SettingsRepositoryImpl : SettingsRepository {

    private val _communicationMode = MutableStateFlow(CommunicationMode.PUSH_TO_TALK)
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    private val _incomingNotifications = MutableStateFlow(true)
    private val _ttsSpeechRate = MutableStateFlow(1.0f)
    private val _ttsPitch = MutableStateFlow(1.0f)
    private val _ttsAutoPlay = MutableStateFlow(false)

    override fun observeCommunicationMode(): Flow<CommunicationMode> = _communicationMode.asStateFlow()
    override suspend fun setCommunicationMode(mode: CommunicationMode) {
        _communicationMode.value = mode
    }

    override fun observeThemePreference(): Flow<ThemePreference> = _themePreference.asStateFlow()
    override suspend fun setThemePreference(theme: ThemePreference) {
        _themePreference.value = theme
    }

    override fun observeIncomingNotifications(): Flow<Boolean> = _incomingNotifications.asStateFlow()
    override suspend fun setIncomingNotifications(enabled: Boolean) {
        _incomingNotifications.value = enabled
    }

    override fun observeTtsSpeechRate(): Flow<Float> = _ttsSpeechRate.asStateFlow()
    override suspend fun setTtsSpeechRate(rate: Float) {
        _ttsSpeechRate.value = rate
    }

    override fun observeTtsPitch(): Flow<Float> = _ttsPitch.asStateFlow()
    override suspend fun setTtsPitch(pitch: Float) {
        _ttsPitch.value = pitch
    }

    override fun observeTtsAutoPlay(): Flow<Boolean> = _ttsAutoPlay.asStateFlow()
    override suspend fun setTtsAutoPlay(enabled: Boolean) {
        _ttsAutoPlay.value = enabled
    }

    private val _onboardingCompleted = MutableStateFlow(false)
    override fun observeOnboardingCompleted(): Flow<Boolean> = _onboardingCompleted.asStateFlow()
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
    }
}
