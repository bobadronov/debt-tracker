package org.bigblackowl.debttracker.ui.components.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/** Defaults to [androidx.compose.material3.MaterialTheme.typography.bodyLarge] — Material 3's own ambient default for a style-less `Text`, so this matches what every "no special role" call site already rendered as. */
@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier.Companion,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight? = null,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        style = style,
        fontWeight = fontWeight,
        textDecoration = textDecoration,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Preview
@Composable
private fun BodyTextLightPreview() = DebtTrackerPreview(darkTheme = false) { BodyText("Body text") }

@Preview
@Composable
private fun BodyTextDarkPreview() = DebtTrackerPreview(darkTheme = true) { BodyText("Body text") }

/** All five text roles together — for comparing size/weight/color at a glance. */
@Composable
private fun TypographySample() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.xs)) {
        LabelText("LabelText")
        BodyText("BodyText")
        CaptionText("CaptionText")
        TitleText("TitleText")
        HeadingText("HeadingText")
    }
}

@Preview
@Composable
private fun TypographyLightPreview() = DebtTrackerPreview(darkTheme = false) { TypographySample() }

@Preview
@Composable
private fun TypographyDarkPreview() = DebtTrackerPreview(darkTheme = true) { TypographySample() }