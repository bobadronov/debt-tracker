package org.bigblackowl.debttracker.core.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import org.bigblackowl.debttracker.domain.model.ScannedContact

/**
 * `ACTION_PICK` on phone numbers: the system picker returns a URI with a one-shot read grant for the
 * chosen row, so the lookup below works without `READ_CONTACTS`.
 */
@Composable
actual fun rememberDevicePhoneContactPicker(onPicked: (ScannedContact) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri -> readContact(context, uri)?.let(currentOnPicked) }
    }
    return remember(launcher) { { launcher.launch(Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)) } }
}

private fun readContact(context: Context, uri: Uri): ScannedContact? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val name = cursor.getString(0)?.trim().orEmpty()
            if (name.isBlank()) return@use null
            ScannedContact(fullName = name, phone = cursor.getString(1)?.takeIf { it.isNotBlank() }, email = null)
        }
    }.getOrNull()
