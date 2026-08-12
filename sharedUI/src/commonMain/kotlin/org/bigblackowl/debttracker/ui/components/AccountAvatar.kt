package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Кругле фото акаунта (ініціали-заглушка через [Icons.Default.Person] поки фото немає) з кнопкою редагування.
 * [localImageBytes] renders a not-yet-uploaded picked image (sign-up, before an account/user id exists);
 * otherwise falls back to the already-uploaded [avatarUrl].
 */
@Composable
fun AccountAvatar(
    avatarUrl: String? = null,
    localImageBytes: ByteArray? = null,
    isUploading: Boolean = false,
    onEditClick: () -> Unit,
) {
    Box(modifier = Modifier.size(Dimens.space120)) {
        Box(
            modifier = Modifier
                .size(Dimens.space120)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            when {
                localImageBytes != null -> {
                    val bitmap = remember(localImageBytes) { localImageBytes.decodeToImageBitmap() }
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(Dimens.space120).clip(CircleShape),
                    )
                }
                avatarUrl != null -> {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.space120).clip(CircleShape),
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.space60),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isUploading) {
                CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space72))
            }
        }
        FilledIconButton(
            onClick = onEditClick,
            enabled = !isUploading,
            modifier = Modifier.align(Alignment.BottomEnd).size(Dimens.space28),
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(Dimens.space16)
            )
        }
    }
}

@Composable
private fun AccountAvatarSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space24)) {
        AccountAvatar(onEditClick = {})
        AccountAvatar(isUploading = true, onEditClick = {})
    }
}

@Preview
@Composable
private fun AccountAvatarLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { AccountAvatarSample() }

@Preview
@Composable
private fun AccountAvatarDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { AccountAvatarSample() }

@Preview(device = DESKTOP)
@Composable
private fun AccountAvatarLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { AccountAvatarSample() }

@Preview(device = DESKTOP)
@Composable
private fun AccountAvatarDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { AccountAvatarSample() }
