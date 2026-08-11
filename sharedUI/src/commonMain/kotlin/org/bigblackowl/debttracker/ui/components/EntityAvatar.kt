package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Кругла аватарка для рядків списку боржників/кредиторів (спек: Debtor.avatarUrl doc):
 * фото за [avatarUrl], якщо воно є, інакше — перша літера [name] на кольоровому фоні,
 * детермінованому від [id], щоб той самий боржник/кредитор завжди отримував той самий колір.
 */
@Composable
fun EntityAvatar(
    id: String,
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.space40,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(avatarColorFor(id)),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

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
