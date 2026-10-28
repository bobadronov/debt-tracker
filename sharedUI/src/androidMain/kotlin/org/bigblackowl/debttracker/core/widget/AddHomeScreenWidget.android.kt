package org.bigblackowl.debttracker.core.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// The receiver lives in :androidApp, which sharedUI can't reference — hence the name by string.
private const val WIDGET_RECEIVER = "org.bigblackowl.debttracker.androidApp.widget.DebtSummaryWidgetReceiver"

@Composable
actual fun rememberAddHomeScreenWidget(): (() -> Unit)? {
    val context = LocalContext.current
    return remember(context) {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) {
            null
        } else {
            { manager.requestPinAppWidget(ComponentName(context.packageName, WIDGET_RECEIVER), null, null); Unit }
        }
    }
}
