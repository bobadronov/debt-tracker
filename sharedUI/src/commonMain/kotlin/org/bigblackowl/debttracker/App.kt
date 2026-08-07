package org.bigblackowl.debttracker

import androidx.compose.runtime.Composable
import org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph
import org.bigblackowl.debttracker.theme.AppTheme

/**
 * Composable root of the app, shared across every platform entry point
 * (`AppActivity`, `main()` on Desktop/JVM, `MainViewController` on iOS).
 * Wraps [DebtTrackerNavGraph] in [AppTheme]; [onThemeChanged] lets a platform
 * shell react to the resolved light/dark mode (e.g. to tint the system bars).
 */
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    DebtTrackerNavGraph()
}
