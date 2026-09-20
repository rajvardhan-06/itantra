package com.itantra.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines the bottom navigation bar items.
 * Five tabs: Home, Communication (Talk), History, Models, Settings
 */
data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label
)

val bottomNavItems = listOf(
    NavItem(Screen.Home,       "Home",     Icons.Filled.Home,                    "Home dashboard"),
    NavItem(Screen.PushToTalk, "Talk",     Icons.Filled.Mic,                     "Push to talk communication"),
    NavItem(Screen.History,    "History",  Icons.AutoMirrored.Filled.Chat,       "Message history"),
    NavItem(Screen.Language,   "Models",   Icons.Filled.Language,                "Language and model management"),
    NavItem(Screen.Settings,   "Settings", Icons.Filled.Settings,                "Application settings"),
)
