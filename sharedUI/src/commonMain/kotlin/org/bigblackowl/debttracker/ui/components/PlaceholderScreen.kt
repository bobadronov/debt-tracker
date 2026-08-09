package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Спільний каркас для екранів-заглушок Фази 1 (справжня верстка з'являється
 * в наступних фазах, п.6 спека). [content] за замовчуванням просто показує
 * назву — екрани, яким потрібні кнопки навігації, передають свій [content].
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {
        CircularWavyProgressIndicator(
            modifier = Modifier.size(Dimens.space60)
        )
    }
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        BackButton(onClick = onBack)
                    }
                },
                actions = actions
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
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
