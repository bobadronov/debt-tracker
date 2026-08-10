package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import org.bigblackowl.debttracker.theme.Dimens

/** Кругле фото акаунта (ініціали-заглушка через [Icons.Default.Person] поки фото немає) з кнопкою редагування. */
@Composable
fun AccountAvatar(avatarUrl: String?, isUploading: Boolean, onEditClick: () -> Unit) {
    Box(modifier = Modifier.size(Dimens.space120)) {
        Box(
            modifier = Modifier
                .size(Dimens.space120)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.space120).clip(CircleShape),
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.space60),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
