package org.bigblackowl.debttracker.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

const val PIN_LENGTH = 4

/**
 * 4-значний PIN-код у вигляді окремих квадратів [_][_][_][_] замість звичайного текстового поля.
 * Shared by [PinSetupDialog] and [org.bigblackowl.debttracker.ui.screens.authgate.AuthGateScreen] so every
 * PIN entry point in the app looks and behaves the same. [imeAction]/[keyboardActions] let each
 * caller wire up its own Enter/Done key behavior (e.g. advance to the next field, or submit).
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
    /**
     * When false the hidden text field is omitted entirely — the dots are display-only and input
     * comes from elsewhere (e.g. [NumericKeypad]). Used on mobile so the system keyboard never
     * appears; desktop keeps it true for physical-keyboard typing.
     */
    acceptTextInput: Boolean = true,
    fillColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
) {
    val strings = LocalStrings.current

    var isFocused by remember { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space16),
                verticalAlignment = Alignment.CenterVertically
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
            if (acceptTextInput) {
                BasicTextField(
                    value = value,
                    onValueChange = { new ->
                        if (new.length <= length && new.all(Char::isDigit)) onValueChange(new)
                    },
                    modifier = Modifier.matchParentSize().alpha(0f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
                    keyboardActions = keyboardActions,
                )
            }
        }
        TextButton(onClick = { pinVisible = !pinVisible }) {
            Text(if (pinVisible) strings.hidePin else strings.showPin)
            Checkbox(pinVisible, onCheckedChange = { pinVisible = !pinVisible }, modifier = Modifier.clip(RoundedCornerShape(Dimens.space3)))
        }
    }
}

/**
 * Індикатор одного розряду PIN — незаповнене коло-контур, заповнене суцільним кольором (Android-style lock dots).
 * [fillColor], [borderColor] і [selectedColor] розведені по окремих параметрах, щоб їх можна було
 * незалежно кастомізувати (border — контур у стані спокою, selectedColor — контур сфокусованого/наступного розряду).
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
        modifier = Modifier
            .size(Dimens.space40)
            .clip(CircleShape)
            .background(animatedFillColor)
            .border(
                width = if (highlighted) Dimens.space3 else Dimens.space1,
                color = animatedBorderColor,
                shape = CircleShape
            )
            .padding(if (filled) Dimens.space3 else Dimens.space1),
        contentAlignment = Alignment.Center,
    ) {
        if (visible && filled) {
            Text(char.toString(), style = MaterialTheme.typography.titleMedium)
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
