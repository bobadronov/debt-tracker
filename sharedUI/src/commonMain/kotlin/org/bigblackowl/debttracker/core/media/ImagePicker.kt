package org.bigblackowl.debttracker.core.media

import androidx.compose.runtime.Composable

/** Байти обраного зображення + розширення файлу (без крапки, напр. "jpg"), для завантаження в Storage. */
data class PickedImage(val bytes: ByteArray, val fileExtension: String)

/** Відкриває системний вибір фото з галереї/файлової системи. */
interface ImagePicker {
    fun pickImage(onPicked: (PickedImage?) -> Unit)
}

/**
 * Фабрика замість expect-класу — Android потребує `ActivityResultLauncher`, який можна
 * зареєструвати лише в Composable-контексті (той самий підхід, що й [org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator]).
 */
@Composable
expect fun rememberImagePicker(): ImagePicker
