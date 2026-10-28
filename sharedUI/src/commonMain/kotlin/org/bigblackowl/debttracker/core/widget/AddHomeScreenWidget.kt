package org.bigblackowl.debttracker.core.widget

import androidx.compose.runtime.Composable

/**
 * Asks the launcher to pin the debt-summary widget to the home screen (Android launchers that
 * support pinning). Returns null where there is nothing to pin, so Settings hides the row.
 */
@Composable
expect fun rememberAddHomeScreenWidget(): (() -> Unit)?
