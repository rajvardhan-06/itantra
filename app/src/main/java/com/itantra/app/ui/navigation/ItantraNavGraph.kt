package com.itantra.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.itantra.app.ui.screens.connection.ConnectionScreen
import com.itantra.app.ui.screens.connection.ConnectionViewModel
import com.itantra.app.ui.screens.diagnostics.DiagnosticsScreen
import com.itantra.app.ui.screens.diagnostics.DiagnosticsViewModel
import com.itantra.app.ui.screens.history.HistoryScreen
import com.itantra.app.ui.screens.history.HistoryViewModel
import com.itantra.app.ui.screens.home.HomeScreen
import com.itantra.app.ui.screens.home.HomeViewModel
import com.itantra.app.ui.screens.language.LanguageScreen
import com.itantra.app.ui.screens.language.LanguageViewModel
import com.itantra.app.ui.screens.onboarding.OnboardingScreen
import com.itantra.app.ui.screens.ptt.PushToTalkScreen
import com.itantra.app.ui.screens.ptt.PushToTalkViewModel
import com.itantra.app.ui.screens.settings.SettingsScreen
import com.itantra.app.ui.screens.settings.SettingsViewModel

/**
 * Root navigation graph wiring all iTantra screens.
 * ViewModels are provided by the Compose ViewModel factory and share the
 * same ViewModel instances across recompositions thanks to [viewModel()].
 */
@Composable
fun ItantraNavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    pttViewModel: PushToTalkViewModel,
    languageViewModel: LanguageViewModel,
    connectionViewModel: ConnectionViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    showOnboarding: Boolean,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startDestination = if (showOnboarding) Screen.Onboarding.route else Screen.Home.route

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier,
        enterTransition  = { fadeIn() + slideInHorizontally(initialOffsetX = { it / 4 }) },
        exitTransition   = { fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 4 }) },
        popEnterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { -it / 4 }) },
        popExitTransition  = { fadeOut() + slideOutHorizontally(targetOffsetX = { it / 4 }) }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel    = homeViewModel,
                onNavigateTo = { navController.navigate(it.route) }
            )
        }
        composable(Screen.PushToTalk.route) {
            PushToTalkScreen(viewModel = pttViewModel)
        }
        composable(Screen.Language.route) {
            LanguageScreen(viewModel = languageViewModel)
        }
        composable(Screen.Connection.route) {
            ConnectionScreen(viewModel = connectionViewModel)
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateToComm = { navController.navigate(Screen.PushToTalk.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel    = settingsViewModel,
                onNavigateTo = { navController.navigate(it.route) }
            )
        }
        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(
                viewModel = diagnosticsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
