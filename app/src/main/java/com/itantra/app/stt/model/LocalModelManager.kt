package com.itantra.app.stt.model

import android.content.Context
import com.itantra.app.stt.SttError
import com.itantra.app.stt.SttLanguage
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Local filesystem-backed implementation of [ModelManager].
 *
 * Stores model packages under the application's internal files directory:
 * `<filesDir>/models/stt/<language_code>/`
 *
 * Enforces pre-installation storage checks, duplicate prevention, and clean uninstallation.
 */
import kotlinx.coroutines.CoroutineDispatcher

open class LocalModelManager(
    private val modelsDir: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ModelManager {

    constructor(context: Context) : this(File(context.filesDir, "models/stt"))

    private val _installedLanguages = MutableStateFlow<Set<SttLanguage>>(emptySet())
    override val installedLanguages: StateFlow<Set<SttLanguage>> = _installedLanguages.asStateFlow()

    init {
        modelsDir.mkdirs()
        refreshInstalledModels()
    }

    private fun refreshInstalledModels() {
        val installed = mutableSetOf<SttLanguage>()
        SttLanguage.entries.forEach { lang ->
            val langDir = File(modelsDir, lang.code)
            val marker = File(langDir, "model.info")
            if (langDir.exists() && marker.exists()) {
                installed.add(lang)
            }
        }
        _installedLanguages.value = installed
    }

    override fun isModelInstalled(language: SttLanguage): Boolean {
        return _installedLanguages.value.contains(language)
    }

    override fun isModelAvailable(language: SttLanguage): Boolean {
        return language.isModelAvailable
    }

    override fun getModelDirectory(language: SttLanguage): File? {
        if (!isModelInstalled(language)) return null
        val dir = File(modelsDir, language.code)
        return if (dir.exists()) dir else null
    }

    override suspend fun installModel(language: SttLanguage): Result<Unit> = withContext(dispatcher) {
        if (!language.isModelAvailable) {
            return@withContext Result.failure(
                IllegalArgumentException("Model for ${language.displayName} is not available for download.")
            )
        }

        if (isModelInstalled(language)) {
            // Prevent duplicate installations
            return@withContext Result.success(Unit)
        }

        val requiredBytes = language.modelSizeBytes
        val availableBytes = getAvailableStorageBytes()
        if (availableBytes < requiredBytes) {
            return@withContext Result.failure(
                IOException("Insufficient storage. Required: ${language.formattedModelSize}, Available: ${availableBytes / (1024 * 1024)} MB")
            )
        }

        try {
            val langDir = File(modelsDir, language.code)
            langDir.mkdirs()

            // Write model manifest descriptor
            val marker = File(langDir, "model.info")
            marker.writeText("language=${language.code}\nversion=1.0.0\nsize=${language.modelSizeBytes}")

            refreshInstalledModels()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeModel(language: SttLanguage): Result<Unit> = withContext(dispatcher) {
        try {
            val langDir = File(modelsDir, language.code)
            if (langDir.exists()) {
                langDir.deleteRecursively()
            }
            refreshInstalledModels()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAvailableStorageBytes(): Long {
        return modelsDir.usableSpace.coerceAtLeast(0L)
    }
}
