package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.text.TitleText
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Round avatar for debtor/creditor list rows (spec: Debtor.avatarUrl doc):
 * the photo at [avatarUrl] if present, otherwise the first letter of [name] on a colored
 * background derived deterministically from [id], so the same debtor/creditor always gets the same color.
 */
@Composable
fun EntityAvatar(
    id: String,
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.IconSize.md,
) {
    // The initial letter is always drawn; the photo fades in over it once it has actually loaded
    // (and the letter fades out), so a slow or failed load never leaves an empty circle.
    // A photo that arrives almost immediately came from the memory cache — it is shown at once
    // (no fade), so rows scrolling back into view don't flash letter → photo.
    var photoLoaded by remember(avatarUrl) { mutableStateOf(false) }
    var cached by remember(avatarUrl) { mutableStateOf(false) }
    val requestedAt = remember(avatarUrl) { TimeSource.Monotonic.markNow() }
    val fadedAlpha by animateFloatAsState(
        targetValue = if (photoLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = PHOTO_FADE_MS),
        label = "avatarPhotoAlpha",
    )
    val photoAlpha = if (cached) 1f else fadedAlpha

    Box(
        modifier = modifier.size(size).clip(CircleShape).background(avatarColorFor(id)),
        contentAlignment = Alignment.Center,
    ) {
        TitleText(
            text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White,
            modifier = Modifier.alpha(1f - photoAlpha),
        )
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    val success = state is AsyncImagePainter.State.Success
                    if (success && !photoLoaded && requestedAt.elapsedNow() < CACHE_HIT_THRESHOLD) cached = true
                    photoLoaded = success
                },
                modifier = Modifier.size(size).clip(CircleShape).alpha(photoAlpha),
            )
        }
    }
}

private const val PHOTO_FADE_MS = 250

/** A load finishing faster than this after composition is treated as a memory-cache hit. */
private val CACHE_HIT_THRESHOLD = 50.milliseconds

private val AvatarPalette = listOf(
    Color(0xFF5E35B1), // deep purple
    Color(0xFF1E88E5), // blue
    Color(0xFF00897B), // teal
    Color(0xFF43A047), // green
    Color(0xFFF4511E), // deep orange
    Color(0xffea1b67), // pink
    Color(0xFF6D4C41), // brown
    Color(0xFF546E7A), // blue grey
)

private fun avatarColorFor(id: String): Color =
    AvatarPalette[(id.hashCode() and 0x7fffffff) % AvatarPalette.size]

@Composable
private fun EntityAvatarSample() {
    Row(
        modifier = Modifier.padding(Dimens.Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.md),
    ) {
        EntityAvatar(id = "1", name = "Олена Коваль", avatarUrl = null)
        EntityAvatar(id = "2", name = "Іван Петренко", avatarUrl = null)
        EntityAvatar(id = "3", name = "Марія Бондар", avatarUrl = null)
    }
}

@Preview
@Composable
private fun EntityAvatarLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { EntityAvatarSample() }

@Preview
@Composable
private fun EntityAvatarDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { EntityAvatarSample() }

@Preview(device = DESKTOP)
@Composable
private fun EntityAvatarLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { EntityAvatarSample() }

@Preview(device = DESKTOP)
@Composable
private fun EntityAvatarDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { EntityAvatarSample() }
