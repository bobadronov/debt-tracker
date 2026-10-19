package org.bigblackowl.debttracker

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph
import org.bigblackowl.debttracker.theme.AppTheme
import org.bigblackowl.debttracker.ui.components.update.InAppUpdateBanner
import org.bigblackowl.debttracker.ui.components.update.UpdateBanner

/**
 * Composable root of the app, shared across every platform entry point
 * (`AppActivity`, `main()` on Desktop/JVM, `MainViewController` on iOS).
 * Wraps [DebtTrackerNavGraph] in [AppTheme]; [onThemeChanged] lets a platform
 * shell react to the resolved light/dark mode (e.g. to tint the system bars).
 * [UpdateBanner] (Desktop) and [InAppUpdateBanner] (Android/Play) each overlay
 * their own self-update prompt, and are no-ops everywhere else.
 *
 * `testTagsAsResourceId = true` exposes every [androidx.compose.ui.platform.testTag] below as a
 * UiAutomator/`adb` resource-id, so UI automation (manual QA scripts, `adb shell uiautomator dump`)
 * can address elements by a stable id instead of screen-reading pixel coordinates.
 */
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        DebtTrackerNavGraph()
        UpdateBanner()
        InAppUpdateBanner()
    }
}
