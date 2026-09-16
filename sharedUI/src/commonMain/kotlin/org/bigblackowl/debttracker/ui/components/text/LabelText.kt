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

@Composable
fun LabelText(
    text: String,
    modifier: Modifier = Modifier.Companion,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.labelMedium,
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
private fun LabelTextLightPreview() = DebtTrackerPreview(darkTheme = false) { LabelText("Label text") }

@Preview
@Composable
private fun LabelTextDarkPreview() = DebtTrackerPreview(darkTheme = true) { LabelText("Label text") }