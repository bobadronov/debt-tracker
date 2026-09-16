package org.bigblackowl.debttracker.ui.components.text

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

/**
 * The app's text taxonomy — five semantic roles instead of ad hoc `Text(style = ...)` calls, so
 * a single typography change applies everywhere at once. Pick by what the text *is*, not by how
 * big it should look:
 * - [HeadingText] — screen/section heading
 * - [TitleText] — a card/row/dialog's own title (contact name, amount headline)
 * - [BodyText] — ordinary copy (the default for text with no special role)
 * - [LabelText] — small labels, section captions
 * - [CaptionText] — de-emphasized secondary/hint text (subtitle under a title, helper text)
 *
 * Semantic coloring (debt/repay accent, error, etc.) is passed via [color] on the call site —
 * it stays a parameter, not a separate component.
 */

@Composable
fun HeadingText(
    text: String,
    modifier: Modifier = Modifier.Companion,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.titleLarge,
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
private fun HeadingTextLightPreview() = DebtTrackerPreview(darkTheme = false) { HeadingText("Heading text") }

@Preview
@Composable
private fun HeadingTextDarkPreview() = DebtTrackerPreview(darkTheme = true) { HeadingText("Heading text") }