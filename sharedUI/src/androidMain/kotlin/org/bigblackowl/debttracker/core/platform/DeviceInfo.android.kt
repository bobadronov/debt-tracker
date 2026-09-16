package org.bigblackowl.debttracker.core.platform

import android.os.Build

actual fun deviceDisplayName(): String {
    val model = Build.MODEL
    val manufacturer = Build.MANUFACTURER
    return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
}

actual fun systemInfo(): String =
    "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}); ${deviceDisplayName()}; ${Build.SUPPORTED_ABIS.firstOrNull()}"
