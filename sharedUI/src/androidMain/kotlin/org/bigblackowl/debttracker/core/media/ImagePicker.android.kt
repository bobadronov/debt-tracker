package org.bigblackowl.debttracker.core.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePicker(): ImagePicker {
    val context = LocalContext.current
    var pendingCallback by remember { mutableStateOf<((PickedImage?) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val callback = pendingCallback
        pendingCallback = null
        if (uri == null) {
            callback?.invoke(null)
            return@rememberLauncherForActivityResult
        }
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        val extension = context.contentResolver.getType(uri)?.substringAfter('/')?.ifBlank { null } ?: "jpg"
        callback?.invoke(bytes?.let { PickedImage(it, extension) })
    }

    return remember(launcher) {
        object : ImagePicker {
            override fun pickImage(onPicked: (PickedImage?) -> Unit) {
                pendingCallback = onPicked
                launcher.launch("image/*")
            }
        }
    }
}
