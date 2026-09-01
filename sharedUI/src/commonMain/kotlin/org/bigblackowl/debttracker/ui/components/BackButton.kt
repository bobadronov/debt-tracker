package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.navigation.LocalNavPane
import org.bigblackowl.debttracker.navigation.NavPane
import org.bigblackowl.debttracker.preview.DebtTrackerPreview


@Composable
fun BackButton(onClick: () -> Unit = {}) =
    IconButton(onClick = onClick) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            null
        )
    }

/**
 * [TopAppBar] with a title and an optional [BackButton] — the shape repeated across nearly every
 * detail/form screen. On desktop ([DesktopTitleBar] claimed) it draws nothing and instead feeds its
 * title / back / actions into the native OS title bar (`main.kt`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    if (DesktopTitleBar.claimed) {
        if (LocalNavPane.current == NavPane.List) {
            // List pane of a two-pane ListDetailScene: keep an in-pane bar for the title/actions,
            // but no back arrow — it's the root of the split (back collapses the detail instead).
            TopAppBar(title = { Text(title) }, actions = actions, modifier = modifier)
        } else {
            // Full window, or the detail pane → route into the native OS title bar (`main.kt`).
            // SideEffect re-asserts every recomposition so an async title (a loaded contact name)
            // stays in sync; `owner` lets the incoming screen win a nav transition without the
            // outgoing screen's teardown wiping the bar.
            val owner = remember { Any() }
            SideEffect { DesktopTitleBar.set(owner, title, onBack, actions) }
            DisposableEffect(Unit) { onDispose { DesktopTitleBar.release(owner) } }
        }
        return
    }
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { onBack?.let { BackButton(onClick = it) } },
        actions = actions,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun BackButtonLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    BackButton{}
}

@Preview
@Composable
private fun BackButtonDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    BackButton{}
}

@Preview(device = DESKTOP)
@Composable
private fun BackButtonLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    BackButton{}
}

@Preview(device = DESKTOP)
@Composable
private fun BackButtonDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    BackButton{}
}