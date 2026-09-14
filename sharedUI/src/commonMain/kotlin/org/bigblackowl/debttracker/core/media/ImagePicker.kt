package org.bigblackowl.debttracker.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

/** Bytes of the picked image + file extension (no dot, e.g. "jpg"), for uploading to Storage. */
data class PickedImage(val bytes: ByteArray, val fileExtension: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PickedImage

        if (!bytes.contentEquals(other.bytes)) return false
        if (fileExtension != other.fileExtension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + fileExtension.hashCode()
        return result
    }
}

/** Opens the system photo picker from the gallery/file system. */
interface ImagePicker {
    fun pickImage(onPicked: (PickedImage?) -> Unit)
}

/**
 * FileKit's gallery picker (`rememberFilePickerLauncher`) works identically on Android, iOS,
 * Desktop and Web, so — unlike [org.bigblackowl.debttracker.core.export.rememberFileExporter] —
 * this needs no expect/actual: one Composable covers every platform.
 */
@Composable
fun rememberImagePicker(): ImagePicker {
    val scope = rememberCoroutineScope()
    var pendingCallback by remember { mutableStateOf<((PickedImage?) -> Unit)?>(null) }

    val launcher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onError = { failure ->
            Napier.w(tag = "ImagePicker", throwable = failure) { "pickImage failed" }
            val callback = pendingCallback
            pendingCallback = null
            callback?.invoke(null)
        },
        onResult = { file ->
            val callback = pendingCallback
            pendingCallback = null
            if (file == null) {
                callback?.invoke(null)
            } else {
                scope.launch {
                    val bytes = runCatching { file.readBytes() }.getOrNull()
                    callback?.invoke(bytes?.let { PickedImage(it, file.extension.ifBlank { "jpg" }) })
                }
            }
        },
    )

    return remember(launcher) {
        object : ImagePicker {
            override fun pickImage(onPicked: (PickedImage?) -> Unit) {
                pendingCallback = onPicked
                launcher.launch()
            }
        }
    }
}
