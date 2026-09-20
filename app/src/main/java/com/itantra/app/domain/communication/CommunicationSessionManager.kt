package com.itantra.app.domain.communication

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Thread-safe singleton and factory manager for the application's [SpeechCommunicationCoordinator].
 *
 * Provides a central point of access for UI layers (e.g. ViewModels) and background services
 * to interact with the end-to-end communication pipeline.
 */
object CommunicationSessionManager {

    @Volatile
    private var instance: SpeechCommunicationCoordinator? = null
    private val lock = Any()

    /**
     * Retrieves the current [SpeechCommunicationCoordinator] or creates a default instance if none exists.
     */
    fun getInstance(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        scope: CoroutineScope? = null
    ): SpeechCommunicationCoordinator {
        return instance ?: synchronized(lock) {
            instance ?: SpeechCommunicationCoordinator(
                dispatcher = dispatcher,
                externalScope = scope
            ).also { instance = it }
        }
    }

    /**
     * Initializes the manager with a pre-configured coordinator (useful for custom transports or DI).
     */
    suspend fun initialize(coordinator: SpeechCommunicationCoordinator) {
        val old = synchronized(lock) {
            val prev = instance
            instance = coordinator
            prev
        }
        old?.release()
    }

    /**
     * Checks whether an active coordinator instance is initialized.
     */
    fun isInitialized(): Boolean = instance != null

    /**
     * Releases the active coordinator instance and clears the reference.
     */
    suspend fun release() {
        val old = synchronized(lock) {
            val prev = instance
            instance = null
            prev
        }
        old?.release()
    }

    @VisibleForTesting
    suspend fun resetForTesting() {
        release()
    }
}
