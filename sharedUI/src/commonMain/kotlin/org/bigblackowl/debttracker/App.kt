package org.bigblackowl.debttracker

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph
import org.bigblackowl.debttracker.theme.AppTheme
import org.bigblackowl.debttracker.ui.components.InAppUpdateBanner
import org.bigblackowl.debttracker.ui.components.UpdateBanner

/**
 * Composable root of the app, shared across every platform entry point
 * (`AppActivity`, `main()` on Desktop/JVM, `MainViewController` on iOS).
 * Wraps [DebtTrackerNavGraph] in [AppTheme]; [onThemeChanged] lets a platform
 * shell react to the resolved light/dark mode (e.g. to tint the system bars).
 * [UpdateBanner] (Desktop) and [InAppUpdateBanner] (Android/Play) each overlay
 * their own self-update prompt, and are no-ops everywhere else.
 */
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    Box {
        DebtTrackerNavGraph()
        UpdateBanner()
        InAppUpdateBanner()
    }
}
