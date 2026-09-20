package com.itantra.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itantra.app.ui.components.ItantraPrimaryButton
import com.itantra.app.ui.theme.ItantraDimens
import com.itantra.app.ui.theme.OnboardingGradientEnd
import com.itantra.app.ui.theme.OnboardingGradientStart
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentColor: Color = OnboardingGradientStart
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.WifiOff,
        title = "No Internet Required",
        description = "iTantra works completely offline. Speak in your language, transmit text over local connections, and hear responses — all without cellular or internet connectivity."
    ),
    OnboardingPage(
        icon = Icons.Filled.Translate,
        title = "10 Indian Languages",
        description = "Speak in Hindi, Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali, or English. On-device AI models process your speech locally."
    ),
    OnboardingPage(
        icon = Icons.Filled.Mic,
        title = "Speak · Transmit · Listen",
        description = "Press and hold to speak. Your voice is transcribed into text, transmitted to nearby devices over Wi-Fi or Bluetooth, and read aloud on the other side.",
        accentColor = OnboardingGradientEnd
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ItantraDimens.SpacingXxl)
        ) {
            // ── Skip button ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ItantraDimens.SpacingXxl),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(
                    visible = !isLastPage,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Pager ──────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(onboardingPages[page])
            }

            // ── Page Indicators ────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = ItantraDimens.SpacingLg)
                    .semantics { contentDescription = "Page ${pagerState.currentPage + 1} of ${onboardingPages.size}" }
            ) {
                onboardingPages.forEachIndexed { index, _ ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // ── Bottom Actions ─────────────────────────────────────────
            Spacer(Modifier.height(ItantraDimens.SpacingMd))

            if (isLastPage) {
                ItantraPrimaryButton(
                    text = "Get Started",
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItantraDimens.LargeButtonHeight)
                )
            } else {
                ItantraPrimaryButton(
                    text = "Continue",
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItantraDimens.LargeButtonHeight)
                )
            }

            Spacer(Modifier.height(ItantraDimens.SpacingHuge))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ItantraDimens.SpacingLg)
    ) {
        // Icon container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(ItantraDimens.RadiusXl))
                .background(page.accentColor.copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.accentColor,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(ItantraDimens.SpacingHuge))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(ItantraDimens.SpacingLg))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}
