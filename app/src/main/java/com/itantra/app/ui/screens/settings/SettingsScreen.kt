package com.itantra.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itantra.app.BuildConfig
import com.itantra.app.R
import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.domain.model.ThemePreference
import com.itantra.app.ui.components.SectionHeader
import com.itantra.app.ui.navigation.Screen
import com.itantra.app.ui.theme.ItantraDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateTo: (Screen) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + ItantraDimens.SpacingSm,
                bottom = paddingValues.calculateBottomPadding() + ItantraDimens.SpacingXl,
                start = ItantraDimens.SpacingMd,
                end = ItantraDimens.SpacingMd
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── General & Language ──────────────────────────────────────────
            item(key = "general_header") {
                SectionHeader(
                    title = "General",
                    subtitle = "Language selection and appearance"
                )
            }
            item(key = "language_nav") {
                SettingsNavigationCard(
                    icon = Icons.Filled.Language,
                    title = "Active Language: ${uiState.activeLanguage.nativeName}",
                    subtitle = "${uiState.activeLanguage.displayName} (${uiState.activeLanguage.script})",
                    onClick = { onNavigateTo(Screen.Language) }
                )
            }
            item(key = "theme_card") {
                SettingsCard {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Column(modifier = Modifier.selectableGroup()) {
                        RadioSettingsRow(
                            icon = Icons.Filled.BatteryFull,
                            label = stringResource(R.string.settings_theme_system),
                            selected = uiState.themePreference == ThemePreference.SYSTEM,
                            onClick = { viewModel.setThemePreference(ThemePreference.SYSTEM) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        RadioSettingsRow(
                            icon = Icons.Filled.WbSunny,
                            label = stringResource(R.string.settings_theme_light),
                            selected = uiState.themePreference == ThemePreference.LIGHT,
                            onClick = { viewModel.setThemePreference(ThemePreference.LIGHT) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        RadioSettingsRow(
                            icon = Icons.Filled.DarkMode,
                            label = stringResource(R.string.settings_theme_dark),
                            selected = uiState.themePreference == ThemePreference.DARK,
                            onClick = { viewModel.setThemePreference(ThemePreference.DARK) }
                        )
                    }
                }
            }

            // ── Speech & Voice Synthesis ────────────────────────────────────
            item(key = "speech_header") {
                SectionHeader(
                    title = "Speech & Voice (TTS)",
                    subtitle = "Offline voice playback configuration"
                )
            }
            item(key = "speech_card") {
                SettingsCard {
                    SwitchSettingsRow(
                        icon = Icons.Filled.RecordVoiceOver,
                        title = stringResource(R.string.tts_autoplay_label),
                        subtitle = "Automatically speak incoming messages",
                        checked = uiState.ttsAutoPlay,
                        onToggle = { viewModel.setTtsAutoPlay(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Speech Rate",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1fx", uiState.ttsSpeechRate),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = uiState.ttsSpeechRate,
                            onValueChange = { viewModel.setTtsSpeechRate(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Voice Pitch",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1fx", uiState.ttsPitch),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = uiState.ttsPitch,
                            onValueChange = { viewModel.setTtsPitch(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // ── Communication & Protocol ────────────────────────────────────
            item(key = "comm_header") {
                SectionHeader(
                    title = "Communication & Protocol",
                    subtitle = "Transceiver behavior and wireless discovery"
                )
            }
            item(key = "comm_mode_card") {
                Column {
                    Text(
                        text = "Transceiver Operating Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    com.itantra.app.ui.components.CommunicationModeSelector(
                        selectedMode = uiState.communicationMode,
                        onModeSelected = { viewModel.setCommunicationMode(it) }
                    )
                }
            }
            item(key = "connection_nav") {
                SettingsNavigationCard(
                    icon = Icons.Filled.Wifi,
                    title = "Wireless Connection Settings",
                    subtitle = "Wi-Fi Direct, Bluetooth LE, and mesh transport",
                    onClick = { onNavigateTo(Screen.Connection) }
                )
            }
            item(key = "diagnostics_nav") {
                SettingsNavigationCard(
                    icon = Icons.Filled.Speed,
                    title = "Diagnostics & Telemetry",
                    subtitle = "Packet delivery ratio, latency RTT, and CRC32 verification",
                    onClick = { onNavigateTo(Screen.Diagnostics) }
                )
            }
            item(key = "notification_switch") {
                SettingsCard {
                    SwitchSettingsRow(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.settings_notifications_incoming),
                        subtitle = "Alert on received audio or text packets",
                        checked = uiState.incomingNotifications,
                        onToggle = { viewModel.setIncomingNotifications(it) }
                    )
                }
            }

            // ── Offline AI Models ───────────────────────────────────────────
            item(key = "models_header") {
                SectionHeader(
                    title = "Offline AI Models",
                    subtitle = "On-device Speech-to-Text neural models"
                )
            }
            item(key = "models_nav") {
                SettingsNavigationCard(
                    icon = Icons.Filled.ModelTraining,
                    title = "Manage Offline Speech Models",
                    subtitle = "View and download on-device STT acoustic models",
                    onClick = { onNavigateTo(Screen.Language) }
                )
            }

            // ── Privacy & Architecture ──────────────────────────────────────
            item(key = "privacy_header") {
                SectionHeader(
                    title = "Privacy & Data Protection",
                    subtitle = "Offline-first security guarantees"
                )
            }
            item(key = "privacy_card") {
                SettingsCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        Column {
                            Text(
                                text = "Zero Cloud Dependency · Local Sandbox",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Audio recordings and transcriptions are processed strictly on-device. No telemetry, third-party analytics, or external cloud servers are contacted.\n\nTransport Note: Local Wi-Fi Direct and Bluetooth SPP packets currently enforce binary framing and CRC32 payload integrity checks. Cryptographic payload encryption (e.g., AES-GCM) is planned for production deployment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── About ───────────────────────────────────────────────────────
            item(key = "about_header") {
                SectionHeader(title = "About iTantra")
            }
            item(key = "about_card") {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Text(
                                text = "iTantra · v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_about_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Clean Architecture · Jetpack Compose · Material 3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun SettingsNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.padding(horizontal = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun RadioSettingsRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun SwitchSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .semantics {
                role = Role.Switch
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
