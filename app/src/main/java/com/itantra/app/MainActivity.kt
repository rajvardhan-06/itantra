package com.itantra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.itantra.app.data.repository.SettingsRepositoryImpl
import com.itantra.app.domain.model.ThemePreference
import com.itantra.app.domain.repository.SettingsRepository
import com.itantra.app.ui.navigation.ItantraNavGraph
import com.itantra.app.ui.navigation.NavItem
import com.itantra.app.ui.navigation.Screen
import com.itantra.app.ui.navigation.bottomNavItems
import com.itantra.app.ui.screens.connection.ConnectionViewModel
import com.itantra.app.ui.screens.diagnostics.DiagnosticsViewModel
import com.itantra.app.ui.screens.history.HistoryViewModel
import com.itantra.app.ui.screens.home.HomeViewModel
import com.itantra.app.ui.screens.language.LanguageViewModel
import com.itantra.app.ui.screens.ptt.PushToTalkViewModel
import com.itantra.app.ui.screens.settings.SettingsViewModel
import com.itantra.app.ui.theme.ItantraTheme
import kotlinx.coroutines.launch

/**
 * Single-Activity entry point for the iTantra application.
 * All navigation is handled by Compose Navigation within this activity.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ItantraApp()
        }
    }
}

/** Routes where the bottom navigation bar should be hidden. */
private val hideBottomNavRoutes = setOf(
    Screen.Onboarding.route,
    Screen.Diagnostics.route
)

@Composable
fun ItantraApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as? ItantraApplication
    val container = app?.container ?: com.itantra.app.di.AppContainer(context.applicationContext)
    val factory = remember { com.itantra.app.di.ItantraViewModelFactory(container) }

    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    // Onboarding state
    val settingsRepository = container.settingsRepository
    val onboardingCompleted by settingsRepository.observeOnboardingCompleted()
        .collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    ItantraTheme(themePreference = settingsUiState.themePreference) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Show bottom nav only on main screens, not during onboarding
        val showBottomBar = currentRoute != null && currentRoute !in hideBottomNavRoutes

        // Instantiate ViewModels with shared container singletons
        val homeViewModel: HomeViewModel = viewModel(factory = factory)
        val pttViewModel: PushToTalkViewModel = viewModel(factory = factory)
        val languageViewModel: LanguageViewModel = viewModel(factory = factory)
        val connectionViewModel: ConnectionViewModel = viewModel(factory = factory)
        val historyViewModel: HistoryViewModel = viewModel(factory = factory)
        val diagnosticsViewModel: DiagnosticsViewModel = viewModel(factory = factory)

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector        = item.icon,
                                        contentDescription = item.contentDescription
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            ItantraNavGraph(
                navController        = navController,
                homeViewModel        = homeViewModel,
                pttViewModel         = pttViewModel,
                languageViewModel    = languageViewModel,
                connectionViewModel  = connectionViewModel,
                historyViewModel     = historyViewModel,
                settingsViewModel    = settingsViewModel,
                diagnosticsViewModel = diagnosticsViewModel,
                showOnboarding       = !onboardingCompleted,
                onOnboardingComplete = {
                    scope.launch {
                        settingsRepository.setOnboardingCompleted(true)
                    }
                },
                modifier             = Modifier.padding(innerPadding)
            )
        }
    }

}
