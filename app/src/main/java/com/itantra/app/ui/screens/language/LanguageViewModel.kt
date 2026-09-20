package com.itantra.app.ui.screens.language

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.data.repository.LanguageRepositoryImpl
import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.domain.repository.LanguageRepository
import com.itantra.app.stt.SttLanguage
import com.itantra.app.stt.model.LocalModelManager
import com.itantra.app.stt.model.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LanguageUiState(
    val languages: List<SupportedLanguage> = SupportedLanguage.values().toList(),
    val activeLanguage: SupportedLanguage = SupportedLanguage.DEFAULT,
    val installedModels: Set<SttLanguage> = emptySet(),
    val isInstalling: Map<SttLanguage, Boolean> = emptyMap(),
    val searchQuery: String = ""
) {
    val filteredLanguages: List<SupportedLanguage>
        get() = if (searchQuery.isBlank()) languages
        else languages.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.nativeName.contains(searchQuery, ignoreCase = true)
        }
}

class LanguageViewModel(
    application: Application,
    private val languageRepository: LanguageRepository = LanguageRepositoryImpl(),
    private val modelManager: ModelManager? = null
) : AndroidViewModel(application) {

    constructor(
        languageRepository: LanguageRepository = LanguageRepositoryImpl(),
        modelManager: ModelManager? = null
    ) : this(
        application = Application(),
        languageRepository = languageRepository,
        modelManager = modelManager
    )

    private val effectiveModelManager: ModelManager? = modelManager ?: runCatching {
        LocalModelManager(application.applicationContext)
    }.getOrNull()

    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            languageRepository.observeActiveLanguage().collect { language ->
                _uiState.update { it.copy(activeLanguage = language) }
            }
        }
        effectiveModelManager?.let { mm ->
            viewModelScope.launch {
                mm.installedLanguages.collect { installedSet ->
                    _uiState.update { it.copy(installedModels = installedSet) }
                }
            }
        }
    }

    fun selectLanguage(language: SupportedLanguage) {
        viewModelScope.launch {
            languageRepository.setActiveLanguage(language)
        }
    }

    fun installModel(language: SttLanguage) {
        effectiveModelManager?.let { mm ->
            viewModelScope.launch {
                _uiState.update { it.copy(isInstalling = it.isInstalling + (language to true)) }
                mm.installModel(language)
                _uiState.update { it.copy(isInstalling = it.isInstalling - language) }
            }
        }
    }

    fun removeModel(language: SttLanguage) {
        effectiveModelManager?.let { mm ->
            viewModelScope.launch {
                _uiState.update { it.copy(isInstalling = it.isInstalling + (language to true)) }
                mm.removeModel(language)
                _uiState.update { it.copy(isInstalling = it.isInstalling - language) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
