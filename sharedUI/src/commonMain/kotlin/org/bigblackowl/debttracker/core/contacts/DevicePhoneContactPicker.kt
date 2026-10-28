package org.bigblackowl.debttracker.core.contacts

import androidx.compose.runtime.Composable
import org.bigblackowl.debttracker.domain.model.ScannedContact

/**
 * Opens the OS contact picker and hands back the chosen person's name/phone/email, to prefill the
 * Add record form. Uses the system picker, so no contacts permission is needed — the app only sees
 * the one contact the user taps. Returns null where the platform has no such picker (Desktop, Web,
 * iOS for now), so callers hide the entry point.
 */
@Composable
expect fun rememberDevicePhoneContactPicker(onPicked: (ScannedContact) -> Unit): (() -> Unit)?
