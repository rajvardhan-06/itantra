package com.itantra.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itantra.app.ui.theme.ItantraDimens
import com.itantra.app.ui.theme.StatusError

// ── Primary Action Button ──────────────────────────────────────────────────

/**
 * Standard primary action button used throughout the iTantra application.
 * Meets minimum 48dp touch target height.
 */
@Composable
fun ItantraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
        contentPadding = PaddingValues(
            horizontal = ItantraDimens.SpacingXl,
            vertical = ItantraDimens.SpacingMd
        ),
        modifier = modifier.height(ItantraDimens.MediumButtonHeight)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ItantraDimens.IconSm)
            )
            Spacer(Modifier.width(ItantraDimens.SpacingSm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Secondary / Outlined Button ────────────────────────────────────────────

@Composable
fun ItantraSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        contentPadding = PaddingValues(
            horizontal = ItantraDimens.SpacingXl,
            vertical = ItantraDimens.SpacingMd
        ),
        modifier = modifier.height(ItantraDimens.MediumButtonHeight)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ItantraDimens.IconSm)
            )
            Spacer(Modifier.width(ItantraDimens.SpacingSm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Standardised Card ──────────────────────────────────────────────────────

@Composable
fun ItantraCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(ItantraDimens.RadiusLarge),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(ItantraDimens.CardPadding)) {
            content()
        }
    }
}

// ── Section Header ─────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier.padding(start = ItantraDimens.SpacingXs, top = ItantraDimens.SpacingXs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Empty State ────────────────────────────────────────────────────────────

/**
 * Reusable empty-state placeholder with icon, title, description, and optional action.
 */
@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Filled.Inbox,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = onAction
) {
    val effectiveAction = onActionClick ?: onAction
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(ItantraDimens.SpacingHuge)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(ItantraDimens.IconHuge)
        )
        Spacer(Modifier.height(ItantraDimens.SpacingLg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(ItantraDimens.SpacingSm))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && effectiveAction != null) {
            Spacer(Modifier.height(ItantraDimens.SpacingXl))
            ItantraPrimaryButton(text = actionLabel, onClick = effectiveAction)
        }
    }
}

// ── Error State ────────────────────────────────────────────────────────────

/**
 * Error display with structured messaging:
 * 1. What happened (title)
 * 2. Why / details (description)
 * 3. A useful next action (actionLabel)
 */
@Composable
fun ErrorStateView(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = StatusError.copy(alpha = 0.1f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Error: $title" }
    ) {
        Row(
            modifier = Modifier.padding(ItantraDimens.CardPadding),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(ItantraDimens.IconMd)
            )
            Spacer(Modifier.width(ItantraDimens.SpacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(ItantraDimens.SpacingXs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.height(ItantraDimens.SpacingSm))
                    ItantraSecondaryButton(
                        text = actionLabel,
                        onClick = onAction,
                        contentColor = StatusError,
                        modifier = Modifier.height(ItantraDimens.SmallButtonHeight)
                    )
                }
            }
        }
    }
}

// ── Loading State ──────────────────────────────────────────────────────────

@Composable
fun LoadingStateView(
    label: String = "Loading…",
    message: String = label,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(ItantraDimens.SpacingHuge)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(ItantraDimens.IconLg),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(ItantraDimens.SpacingLg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
