package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import debt_tracker.sharedui.generated.resources.Res
import debt_tracker.sharedui.generated.resources.ic_app_logo
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.jetbrains.compose.resources.painterResource

/**
 * Branded, centered shell for the app-lock screens (`AuthGateScreen`, `ProtectionOnboardingScreen`):
 * app logo + title/subtitle over the themed background, content column below capped at
 * [Dimens.contentMaxWidth]. Replaces the bare `PlaceholderScreen` + `Text` those screens used, which
 * the user flagged as looking unfinished.
 */
@Composable
fun UnlockScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.space24, vertical = Dimens.space24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(Dimens.space72).clip(RoundedCornerShape(Dimens.space20)),
            )
            Spacer(Modifier.height(Dimens.space24))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            subtitle?.let {
                Spacer(Modifier.height(Dimens.space8))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(Dimens.space40))
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Preview
@Composable
private fun UnlockScaffoldLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    UnlockScaffold(title = "DebtTracker", subtitle = "Enter your PIN code") { Text("content") }
}

@Preview(device = DESKTOP)
@Composable
private fun UnlockScaffoldDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    UnlockScaffold(title = "DebtTracker", subtitle = "Enter your PIN code") { Text("content") }
}
