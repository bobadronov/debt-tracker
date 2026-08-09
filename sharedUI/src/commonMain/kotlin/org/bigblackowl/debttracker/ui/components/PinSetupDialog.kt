package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors

private const val PIN_LENGTH = 4

private const val NEW_PIN_PAGE = 0
private const val CONFIRM_PIN_PAGE = 1

/**
 * PIN setup dialog shared by SettingsScreen (manual enable) and the first-launch protection
 * onboarding screen. New PIN and Confirm PIN are two [HorizontalPager] pages instead of stacked
 * fields — entering a full PIN swipes to the next page automatically, and Back returns to fix it.
 */
@Composable
fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val strings = LocalStrings.current
    val pinFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    fun trySubmit() {
        when {
            pin.length < PIN_LENGTH -> error = strings.settingsPinTooShort
            pin != confirmPin -> error = strings.settingsPinMismatch
            else -> onConfirm(pin)
        }
    }

    // Google-style OTP/PIN entry: focus is on the first field the moment the dialog opens, so
    // typing can start immediately without a tap.
    LaunchedEffect(Unit) { pinFocusRequester.requestFocus() }
    // Whichever page becomes current — auto-advance, manual swipe, or Back — gets focus.
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == CONFIRM_PIN_PAGE) confirmFocusRequester.requestFocus()
        else pinFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Password, contentDescription = null) },
        title = { Text(strings.settingsPinSetupTitle) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    if (pagerState.currentPage == CONFIRM_PIN_PAGE) strings.settingsPinSetupConfirm else strings.settingsPinSetupNew,
                    style = MaterialTheme.typography.labelLarge,
                )

                Spacer(Modifier.height(Dimens.space4))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(Dimens.space60)
                ) { page ->
                    when (page) {
                        NEW_PIN_PAGE -> PinCodeField(
                            value = pin,
                            onValueChange = { new ->
                                pin = new
                                error = null
                                // Auto-advance to "Confirm" as soon as the PIN is complete — no manual tap needed.
                                if (new.length == PIN_LENGTH) scope.launch {
                                    pagerState.animateScrollToPage(
                                        CONFIRM_PIN_PAGE
                                    )
                                }
                            },
                            focusRequester = pinFocusRequester,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        else -> PinCodeField(
                            value = confirmPin,
                            onValueChange = { new ->
                                confirmPin = new
                                error = null
                                // Auto-submit once both PINs are complete, same as Google's OTP entry.
                                if (new.length == PIN_LENGTH && pin.length == PIN_LENGTH) trySubmit()
                            },
                            focusRequester = confirmFocusRequester,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                error?.let {
                    Spacer(Modifier.height(Dimens.space8))
                    Text(it, color = MaterialTheme.debtAccentColors.debt)
                }
            }
        },
        confirmButton = {
            if (pagerState.currentPage == CONFIRM_PIN_PAGE) {
                TextButton(
                    onClick = { trySubmit() },
                    enabled = confirmPin.length == PIN_LENGTH
                ) { Text(strings.save) }
            } else {
                TextButton(onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            CONFIRM_PIN_PAGE
                        )
                    }
                }, enabled = pin.length == PIN_LENGTH) { Text(strings.continueLabel) }
            }
        },
        dismissButton = {
            if (pagerState.currentPage == CONFIRM_PIN_PAGE) {
                TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(NEW_PIN_PAGE) } }) {
                    Text(
                        strings.back
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        },
    )
}

/** 4-значний PIN-код у вигляді окремих квадратів [_][_][_][_] замість звичайного текстового поля. */
@Composable
private fun PinCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    length: Int = PIN_LENGTH,
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
                if (new.length <= length && new.all(Char::isDigit)) onValueChange(
                    new
                )
            },
            modifier = Modifier.matchParentSize().alpha(0f)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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

@Preview
@Composable
private fun PinSetupDialogLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Scaffold {
        PinSetupDialog({}, {})
    }
}

@Preview
@Composable
private fun PinSetupDialogDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Scaffold {
        PinSetupDialog({}, {})
    }
}

@Preview(device = DESKTOP)
@Composable
private fun PinSetupDialogLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Scaffold {
        PinSetupDialog({}, {})
    }
}

@Preview(device = DESKTOP)
@Composable
private fun PinSetupDialogDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Scaffold {
        PinSetupDialog({}, {})
    }
}
