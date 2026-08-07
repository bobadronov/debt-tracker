package org.bigblackowl.debttracker.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/**
 * Kotlin/Native forbids mixing Kotlin supertypes with Objective-C ones on the same class, so the
 * `NSObject`/delegate-protocol conformance lives here, separate from the plain-Kotlin [ImagePicker].
 */
@OptIn(ExperimentalForeignApi::class)
private class PickerDelegate : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    var callback: ((PickedImage?) -> Unit)? = null

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val data = image?.let { UIImageJPEGRepresentation(it, 0.85) }
        val result = data?.toByteArray()?.let { PickedImage(it, "jpg") }
        callback?.invoke(result)
        callback = null
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        callback?.invoke(null)
        callback = null
    }
}

/** Wraps `UIImagePickerController` (photo library) — simpler Kotlin/Native interop than `PHPickerViewController`. */
@OptIn(ExperimentalForeignApi::class)
private class IosImagePicker : ImagePicker {

    private val delegate = PickerDelegate()

    override fun pickImage(onPicked: (PickedImage?) -> Unit) {
        delegate.callback = onPicked
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.delegate = delegate
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, length)
    }
    return out
}

@Composable
actual fun rememberImagePicker(): ImagePicker = remember { IosImagePicker() }
