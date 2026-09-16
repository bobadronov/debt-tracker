package org.bigblackowl.debttracker.core.qr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.button.TextButton
import org.bigblackowl.debttracker.ui.components.text.CaptionText

/** Platforms with a working camera-based QRKit scanner. Desktop/Web have none (QRKit's wasmJs
 * `QrScanner` actual is an empty no-op stub, and Desktop never gets the camera dependency at all —
 * see `sharedUI/build.gradle.kts`), so they open [ContactQrFilePickerContent] as their only way to
 * scan instead of losing the feature outright. On Android/iOS the file picker is additionally
 * offered as a secondary "select image" action next to the camera (see [ContactQrScanOverlay][org.bigblackowl.debttracker.ui.components.contact.ContactQrScanOverlay]),
 * since [decodeQrFromImage] has a real per-platform implementation everywhere now. Shared by every
 * screen that offers a QR-scan entry point (QrHubScreen, AddEditContactForm). */
val QR_SCAN_CAPABLE_PLATFORMS = setOf(AppPlatform.ANDROID, AppPlatform.IOS)

/**
 * A pill-shaped action floated over the live camera preview (bottom-center). [ContactQrScanner]'s
 * camera surface is a native view outside Compose's own drawing/compositing, not a regular
 * Compose-drawn sibling — a plain [TextButton] placed below it in a `Column` measures and lays out
 * correctly (confirmed via the accessibility tree — the clickable region is there, right size,
 * right place) but never actually paints: the camera surface's own compositing wins regardless of
 * Compose z-order, so a same-color-as-background text button in that spot is invisible and looks
 * unclickable even though it isn't. Wrapping it in an opaque [Surface] guarantees it paints on top
 * regardless of how the camera surface composites, and doubles as sane contrast against whatever
 * the camera happens to be pointed at.
 */
@Composable
fun BoxScope.ScanOverlayActionButton(text: String, errorText: String? = null, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Dimens.Spacing.lg),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = Dimens.Spacing.xs)) {
                Text(text)
            }
            errorText?.let {
                CaptionText(
                    it,
                    modifier = Modifier.padding(horizontal = Dimens.Spacing.md, vertical = Dimens.Spacing.xs / 2),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
