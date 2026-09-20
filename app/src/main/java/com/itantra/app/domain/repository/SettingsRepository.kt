package com.itantra.app.domain.repository

import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting and observing user application settings.
 */
interface SettingsRepository {
    fun observeCommunicationMode(): Flow<CommunicationMode>
    suspend fun setCommunicationMode(mode: CommunicationMode)

    fun observeThemePreference(): Flow<ThemePreference>
    suspend fun setThemePreference(theme: ThemePreference)

    fun observeIncomingNotifications(): Flow<Boolean>
    suspend fun setIncomingNotifications(enabled: Boolean)

    fun observeTtsSpeechRate(): Flow<Float>
    suspend fun setTtsSpeechRate(rate: Float)

    fun observeTtsPitch(): Flow<Float>
    suspend fun setTtsPitch(pitch: Float)

    fun observeTtsAutoPlay(): Flow<Boolean>
    suspend fun setTtsAutoPlay(enabled: Boolean)

    fun observeOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
}
