package com.itantra.app.data.repository

import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of [LanguageRepository].
 * Replace with a DataStore-backed implementation for persistence across app restarts.
 */
class LanguageRepositoryImpl : LanguageRepository {

    private val _activeLanguage = MutableStateFlow(SupportedLanguage.DEFAULT)

    override fun observeActiveLanguage(): Flow<SupportedLanguage> = _activeLanguage.asStateFlow()

    override suspend fun setActiveLanguage(language: SupportedLanguage) {
        _activeLanguage.value = language
    }
}
