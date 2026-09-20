package com.itantra.app.stt.model

import com.itantra.app.stt.SttLanguage
import java.io.File
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for managing local offline STT model packages.
 */
interface ModelManager {

    /** Observes installed model languages. */
    val installedLanguages: StateFlow<Set<SttLanguage>>

    /**
     * Checks whether an offline model package is installed locally on the device.
     */
    fun isModelInstalled(language: SttLanguage): Boolean

    /**
     * Checks whether a model is available in the repository for download/installation.
     */
    fun isModelAvailable(language: SttLanguage): Boolean

    /**
     * Gets the local file directory for an installed model, or null if not installed.
     */
    fun getModelDirectory(language: SttLanguage): File?

    /**
     * Installs an offline model for the specified language with explicit user intent.
     * Checks available disk space before installation.
     */
    suspend fun installModel(language: SttLanguage): Result<Unit>

    /**
     * Removes an installed model from local storage to reclaim disk space.
     */
    suspend fun removeModel(language: SttLanguage): Result<Unit>

    /**
     * Gets the available free disk storage space in bytes.
     */
    fun getAvailableStorageBytes(): Long
}
