package com.itantra.app.ui.screens.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itantra.app.R
import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.stt.SttLanguage
import com.itantra.app.ui.components.EmptyStateView
import com.itantra.app.ui.components.LanguageCard
import com.itantra.app.ui.components.SectionHeader
import com.itantra.app.ui.theme.ItantraDimens
import com.itantra.app.ui.theme.StatusOnline
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(viewModel: LanguageViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Offline Models",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${uiState.languages.size} Languages",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + ItantraDimens.SpacingSm,
                bottom = paddingValues.calculateBottomPadding() + ItantraDimens.SpacingXl,
                start = ItantraDimens.SpacingMd,
                end = ItantraDimens.SpacingMd
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Informational offline badge card
            item(key = "info_header") {
                Card(
                    shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Select your communication language and manage offline speech models. All processing stays strictly on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search bar
            item(key = "search_field") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Search by language or script…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Active Language Section
            if (uiState.searchQuery.isBlank()) {
                item(key = "active_section_header") {
                    SectionHeader(
                        title = "Active Language",
                        subtitle = "Currently used for speech-to-text input"
                    )
                }
                item(key = "active_card_${uiState.activeLanguage.code}") {
                    val sttLang = SttLanguage.fromSupportedLanguage(uiState.activeLanguage)
                    LanguageCard(
                        language = uiState.activeLanguage,
                        isSelected = true,
                        isModelInstalled = uiState.installedModels.contains(sttLang),
                        isInstalling = uiState.isInstalling[sttLang] ?: false,
                        onInstallClick = {
                            viewModel.installModel(sttLang)
                            scope.launch {
                                snackbarHostState.showSnackbar("Downloading ${uiState.activeLanguage.displayName} model…")
                            }
                        },
                        onRemoveClick = {
                            viewModel.removeModel(sttLang)
                            scope.launch {
                                snackbarHostState.showSnackbar("Removed ${uiState.activeLanguage.displayName} model")
                            }
                        },
                        onClick = { /* Already active */ }
                    )
                }

                item(key = "all_languages_header") {
                    Spacer(Modifier.height(6.dp))
                    SectionHeader(
                        title = "All Supported Languages",
                        subtitle = "Tap to switch active communication language"
                    )
                }
            }

            // If empty search results
            if (uiState.filteredLanguages.isEmpty()) {
                item(key = "empty_search") {
                    EmptyStateView(
                        icon = Icons.Filled.Search,
                        title = "No languages found",
                        description = "No language matches \"${uiState.searchQuery}\"",
                        actionLabel = "Clear search",
                        onActionClick = { viewModel.onSearchQueryChanged("") },
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            } else {
                items(
                    items = uiState.filteredLanguages,
                    key = { it.code }
                ) { language ->
                    val sttLang = SttLanguage.fromSupportedLanguage(language)
                    val isInstalled = uiState.installedModels.contains(sttLang)
                    val isInstalling = uiState.isInstalling[sttLang] ?: false

                    LanguageCard(
                        language = language,
                        isSelected = language == uiState.activeLanguage,
                        isModelInstalled = isInstalled,
                        isInstalling = isInstalling,
                        onInstallClick = {
                            viewModel.installModel(sttLang)
                            scope.launch {
                                snackbarHostState.showSnackbar("Installing offline model for ${language.displayName}…")
                            }
                        },
                        onRemoveClick = {
                            viewModel.removeModel(sttLang)
                            scope.launch {
                                snackbarHostState.showSnackbar("Removed model for ${language.displayName}")
                            }
                        },
                        onClick = {
                            viewModel.selectLanguage(language)
                            scope.launch {
                                snackbarHostState.showSnackbar("${language.nativeName} (${language.displayName}) selected")
                            }
                        }
                    )
                }
            }
        }
    }
}
