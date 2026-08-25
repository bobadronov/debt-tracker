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

/** Байти обраного зображення + розширення файлу (без крапки, напр. "jpg"), для завантаження в Storage. */
data class PickedImage(val bytes: ByteArray, val fileExtension: String)

/** Відкриває системний вибір фото з галереї/файлової системи. */
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
