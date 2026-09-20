package com.itantra.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.itantra.app.ui.screens.connection.ConnectionViewModel
import com.itantra.app.ui.screens.diagnostics.DiagnosticsViewModel
import com.itantra.app.ui.screens.history.HistoryViewModel
import com.itantra.app.ui.screens.home.HomeViewModel
import com.itantra.app.ui.screens.language.LanguageViewModel
import com.itantra.app.ui.screens.ptt.PushToTalkViewModel
import com.itantra.app.ui.screens.settings.SettingsViewModel

/**
 * Custom [ViewModelProvider.Factory] injecting singletons from [AppContainer]
 * into all screen ViewModels.
 */
class ItantraViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    messageRepository = container.messageRepository,
                    languageRepository = container.languageRepository,
                    connectionRepository = container.connectionRepository,
                    demoSimulationManager = container.demoSimulationManager
                ) as T
            }
            modelClass.isAssignableFrom(PushToTalkViewModel::class.java) -> {
                PushToTalkViewModel(
                    audioRecorder = container.audioRecorder,
                    voiceActivityDetector = container.voiceActivityDetector,
                    sttEngine = container.sttEngine,
                    modelManager = container.modelManager,
                    messageRepository = container.messageRepository,
                    languageRepository = container.languageRepository,
                    connectionRepository = container.connectionRepository,
                    communicationManager = container.communicationManager,
                    demoSimulationManager = container.demoSimulationManager
                ) as T
            }
            modelClass.isAssignableFrom(LanguageViewModel::class.java) -> {
                LanguageViewModel(
                    languageRepository = container.languageRepository,
                    modelManager = container.modelManager
                ) as T
            }
            modelClass.isAssignableFrom(ConnectionViewModel::class.java) -> {
                ConnectionViewModel(
                    connectionRepository = container.connectionRepository,
                    appContainer = container,
                    demoSimulationManager = container.demoSimulationManager
                ) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(
                    messageRepository = container.messageRepository,
                    ttsEngine = container.ttsEngine
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    languageRepository = container.languageRepository
                ) as T
            }
            modelClass.isAssignableFrom(DiagnosticsViewModel::class.java) -> {
                DiagnosticsViewModel(
                    communicationManager = container.communicationManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
