package org.bigblackowl.debttracker.core.contacts

import androidx.compose.runtime.Composable
import org.bigblackowl.debttracker.domain.model.ScannedContact

@Composable
actual fun rememberDevicePhoneContactPicker(onPicked: (ScannedContact) -> Unit): (() -> Unit)? = null
