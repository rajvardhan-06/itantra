package com.itantra.app.domain.repository

import com.itantra.app.domain.model.SupportedLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting and observing the user's active language selection.
 */
interface LanguageRepository {
    /** Stream of the currently active language, updated on every change. */
    fun observeActiveLanguage(): Flow<SupportedLanguage>

    /** Persist the user's language choice. */
    suspend fun setActiveLanguage(language: SupportedLanguage)
}
