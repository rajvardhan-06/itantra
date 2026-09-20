package com.itantra.app.ui.navigation

/**
 * Type-safe navigation destinations for the iTantra application.
 * Each screen is a singleton object for destinations without arguments,
 * or a data class for destinations with route parameters.
 */
sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home       : Screen("home")
    data object PushToTalk : Screen("push_to_talk")
    data object Language   : Screen("language")
    data object Connection : Screen("connection")
    data object History    : Screen("history")
    data object Settings   : Screen("settings")
    data object Diagnostics: Screen("diagnostics")
}
