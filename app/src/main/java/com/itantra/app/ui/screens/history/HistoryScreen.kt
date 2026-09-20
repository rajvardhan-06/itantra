package com.itantra.app.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itantra.app.R
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.ui.components.EmptyStateView
import com.itantra.app.ui.components.LoadingStateView
import com.itantra.app.ui.components.MessageItemCard
import com.itantra.app.ui.theme.ItantraDimens

private enum class MessageFilter { ALL, SENT, RECEIVED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToComm: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(MessageFilter.ALL) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Filter messages based on search query and direction filter
    val filteredMessages = remember(uiState.messages, searchQuery, selectedFilter) {
        uiState.messages.filter { msg ->
            val matchesFilter = when (selectedFilter) {
                MessageFilter.ALL -> true
                MessageFilter.SENT -> msg.direction == MessageDirection.SENT
                MessageFilter.RECEIVED -> msg.direction == MessageDirection.RECEIVED
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                msg.content.contains(searchQuery, ignoreCase = true) ||
                msg.language.displayName.contains(searchQuery, ignoreCase = true) ||
                msg.language.nativeName.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Message History?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all ${uiState.messages.size} sent and received messages from this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(
                        text = "Clear All",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.messages.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${uiState.messages.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.semantics {
                                role = Role.Button
                                contentDescription = "Clear all message history"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingStateView(
                    message = "Loading messages…",
                    modifier = Modifier.weight(1f)
                )
            } else if (uiState.messages.isEmpty()) {
                EmptyStateView(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = stringResource(R.string.history_empty_title),
                    description = stringResource(R.string.history_empty_desc),
                    actionLabel = "Start Communication",
                    onActionClick = onNavigateToComm,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search messages…",
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
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ItantraDimens.SpacingMd, vertical = ItantraDimens.SpacingXs)
                )

                // Direction filter chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ItantraDimens.SpacingMd, vertical = ItantraDimens.SpacingXs)
                ) {
                    FilterChip(
                        selected = selectedFilter == MessageFilter.ALL,
                        onClick = { selectedFilter = MessageFilter.ALL },
                        label = { Text("All (${uiState.messages.size})") },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                    val sentCount = remember(uiState.messages) {
                        uiState.messages.count { it.direction == MessageDirection.SENT }
                    }
                    FilterChip(
                        selected = selectedFilter == MessageFilter.SENT,
                        onClick = { selectedFilter = MessageFilter.SENT },
                        label = { Text("Sent ($sentCount)") },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                    val receivedCount = remember(uiState.messages) {
                        uiState.messages.count { it.direction == MessageDirection.RECEIVED }
                    }
                    FilterChip(
                        selected = selectedFilter == MessageFilter.RECEIVED,
                        onClick = { selectedFilter = MessageFilter.RECEIVED },
                        label = { Text("Received ($receivedCount)") },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }

                // Message list
                if (filteredMessages.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(ItantraDimens.SpacingLg)
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No messages match \"$searchQuery\""
                                   else "No messages in this filter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = ItantraDimens.SpacingSm,
                            end = ItantraDimens.SpacingSm,
                            top = ItantraDimens.SpacingSm,
                            bottom = ItantraDimens.SpacingXl
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = filteredMessages,
                            key = { it.id }
                        ) { message ->
                            MessageItemCard(
                                message = message,
                                isPlaying = uiState.currentlyPlayingMessageId == message.id,
                                ttsState = uiState.ttsState,
                                onPlayTts = { viewModel.speakMessage(it) },
                                onStopTts = { viewModel.stopSpeech() }
                            )
                        }
                    }
                }
            }
        }
    }
}
