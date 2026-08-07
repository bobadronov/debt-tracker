package org.bigblackowl.debttracker.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Web ще не підключений до бізнес-логіки застосунку (README) — пікер завжди повертає null. */
private class NoOpImagePicker : ImagePicker {
    override fun pickImage(onPicked: (PickedImage?) -> Unit) = onPicked(null)
}

@Composable
actual fun rememberImagePicker(): ImagePicker = remember { NoOpImagePicker() }
