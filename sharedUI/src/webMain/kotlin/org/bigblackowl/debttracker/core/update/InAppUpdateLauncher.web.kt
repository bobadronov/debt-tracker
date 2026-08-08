package org.bigblackowl.debttracker.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual val inAppUpdateSupported: Boolean = false

@Composable
actual fun rememberInAppUpdateLauncher(): InAppUpdateLauncher = remember { NoopInAppUpdateLauncher }
