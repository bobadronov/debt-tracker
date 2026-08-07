package org.bigblackowl.debttracker.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private class DesktopImagePicker : ImagePicker {
    override fun pickImage(onPicked: (PickedImage?) -> Unit) {
        val chooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            onPicked(null)
            return
        }
        val file = chooser.selectedFile
        val bytes = runCatching { file.readBytes() }.getOrNull()
        val extension = file.extension.ifBlank { "jpg" }
        onPicked(bytes?.let { PickedImage(it, extension) })
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker = remember { DesktopImagePicker() }
