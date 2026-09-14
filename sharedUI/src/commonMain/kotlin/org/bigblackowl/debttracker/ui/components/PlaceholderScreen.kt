package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.appbar.BackTopAppBar

/**
 * Shared scaffold for Phase 1 placeholder screens (real layouts arrive in
 * later phases, spec item 6). By default [content] just shows the
 * title — screens that need navigation buttons pass their own [content].
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable ColumnScope.() -> Unit = { FullScreenLoadingIndicator() }
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = title, onBack = onBack, actions = actions) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

@Preview
@Composable
private fun PlaceholderScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    PlaceholderScreen(title = "DebtTracker")
}

@Preview
@Composable
private fun PlaceholderScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    PlaceholderScreen(title = "DebtTracker")
}

@Preview(device = DESKTOP)
@Composable
private fun PlaceholderScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    PlaceholderScreen(title = "DebtTracker")
}

@Preview(device = DESKTOP)
@Composable
private fun PlaceholderScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    PlaceholderScreen(title = "DebtTracker")
}
