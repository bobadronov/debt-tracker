package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.theme.Dimens

const val PIN_LENGTH = 4

/**
 * 4-значний PIN-код у вигляді окремих квадратів [_][_][_][_] замість звичайного текстового поля.
 * Shared by [PinSetupDialog] and [org.bigblackowl.debttracker.ui.screens.AuthGateScreen] so every
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
) {
    val strings = LocalStrings.current

    var isFocused by remember { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }

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
                )
            }
            IconButton(onClick = { pinVisible = !pinVisible }) {
                Icon(
                    if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (pinVisible) strings.hidePin else strings.showPin,
                )
            }
        }
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

/** Індикатор одного розряду PIN — незаповнене коло-контур, заповнене суцільним кольором (Android-style lock dots). */
@Composable
private fun PinDot(char: Char?, highlighted: Boolean, visible: Boolean) {
    val filled = char != null
    val borderColor by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "pin-dot-border",
    )
    val fillColor by animateColorAsState(
        if (filled && !visible) MaterialTheme.colorScheme.secondary else Color.Transparent,
        label = "pin-dot-fill",
    )

    Box(
        modifier = Modifier
            .size(Dimens.space40)
            .clip(CircleShape)
            .background(fillColor)
            .border(
                width = if (highlighted) Dimens.space3 else Dimens.space1,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (visible && filled) {
            Text(char.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}
