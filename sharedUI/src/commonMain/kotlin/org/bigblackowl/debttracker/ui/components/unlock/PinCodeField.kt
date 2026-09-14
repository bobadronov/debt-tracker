package org.bigblackowl.debttracker.ui.components.unlock

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.text.TitleText

const val PIN_LENGTH = 4

/**
 * A 4-digit PIN code shown as separate boxes [_][_][_][_] instead of a plain text field.
 * Shared by [PinSetupDialog] and [org.bigblackowl.debttracker.ui.screens.authgate.AuthGateScreen] so every
 * PIN entry point in the app looks and behaves the same. [imeAction]/[keyboardActions] let each
 * caller wire up its own Enter/Done key behavior (e.g. advance to the next field, or submit).
 *
 * Input goes through a transparent, full-bleed [BasicTextField] behind the dots — the system
 * numeric keyboard on mobile, the physical keyboard on desktop.
 */
@Composable
fun PinCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    length: Int = PIN_LENGTH,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    fillColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
) {
    val strings = LocalStrings.current

    var isFocused by remember { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.md)
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.lg), verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(length) { index ->
                    PinDot(
                        char = value.getOrNull(index),
                        highlighted = isFocused && index == value.length,
                        visible = pinVisible,
                        fillColor = fillColor,
                        borderColor = borderColor,
                        selectedColor = selectedColor,
                    )
                }
            }
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    if (new.length <= length && new.all(Char::isDigit)) onValueChange(new)
                },
                modifier = Modifier.matchParentSize().alpha(0f).focusRequester(focusRequester).onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
                keyboardActions = keyboardActions,
            )
        }

        ToggleButton(
            checked = pinVisible, onCheckedChange = {
                pinVisible = !pinVisible
            }) {
            Text(
                if (pinVisible) strings.authGate.hidePin else strings.authGate.showPin, modifier = Modifier.padding(end = Dimens.Spacing.xs)
            )
            Icon(
                imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null
            )

        }
    }
}

/**
 * Indicator for a single PIN digit — an empty outlined circle, filled with a solid color when set
 * (Android-style lock dots). [fillColor], [borderColor] and [selectedColor] are split into separate
 * parameters so they can be customized independently (border — outline at rest, selectedColor —
 * outline of the focused/next digit).
 */
@Composable
private fun PinDot(
    char: Char?,
    highlighted: Boolean,
    visible: Boolean,
    fillColor: Color = MaterialTheme.colorScheme.errorContainer,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
) {
    val filled = char != null
    val animatedBorderColor by animateColorAsState(
        if (highlighted) selectedColor else borderColor,
        label = "pin-dot-border",
    )
    val animatedFillColor by animateColorAsState(
        if (filled && !visible) fillColor else Color.Transparent,
        label = "pin-dot-fill",
    )

    Box(
        modifier = Modifier.size(Dimens.IconSize.md).clip(CircleShape).background(animatedFillColor).border(
            width = if (highlighted) Dimens.Border.thick else Dimens.Border.thin, color = animatedBorderColor, shape = CircleShape
        ).padding(if (filled) Dimens.Border.thick else Dimens.Border.thin),
        contentAlignment = Alignment.Center,
    ) {
        if (visible && filled) {
            TitleText(char.toString())
        }
    }
}

@Composable
private fun PinCodeFieldSample() {
    var pin by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    PinCodeField(value = pin, onValueChange = { pin = it }, focusRequester = focusRequester)
}

@Preview(device = DESKTOP)
@Composable
private fun PinCodeFieldLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { PinCodeFieldSample() }

@Preview(device = DESKTOP)
@Composable
private fun PinCodeFieldDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { PinCodeFieldSample() }
