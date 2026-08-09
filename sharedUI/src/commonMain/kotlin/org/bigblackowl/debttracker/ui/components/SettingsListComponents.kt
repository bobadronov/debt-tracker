package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Заголовок групи налаштувань (Material You: підпис над tonal-карткою) + сама картка.
 * Спільний стиль для екранів на кшталт Settings/Export — список tonal-карток замість плаского Column.
 */
@Composable
fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.space8),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.space20),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(content = content)
        }
    }
}

/** Clickable + a subtle press-down scale, skipping the interaction-source/animation setup entirely for rows without a click handler. */
private fun Modifier.clickablePressScale(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "settingsRowPressScale")
    scale(pressScale).clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
}

/** Один рядок налаштувань: іконка в tonal-колі, заголовок (+опційний підзаголовок), опційний trailing-контрол. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (onClick != null) it.clickablePressScale(onClick) else it
            }
            .padding(horizontal = Dimens.space16, vertical = Dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(Dimens.space40).clip(CircleShape).background(iconContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(Dimens.space20))
        }
        Spacer(Modifier.width(Dimens.space16))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            AnimatedVisibility(visible = subtitle != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    subtitle.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Dimens.space8))
            trailing()
        }
    }
}

@Composable
fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = Dimens.space72, end = Dimens.space16),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SettingsSectionSample() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    Column(modifier = Modifier.padding(Dimens.space16)) {
        SettingsSection(title = "Preferences") {
            SettingsRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = if (notificationsEnabled) "Enabled" else "Disabled",
                trailing = {
                    Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.Notifications,
                title = "Row without a subtitle",
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun SettingsSectionLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { SettingsSectionSample() }

@Preview
@Composable
private fun SettingsSectionDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { SettingsSectionSample() }

@Preview(device = DESKTOP)
@Composable
private fun SettingsSectionLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { SettingsSectionSample() }

@Preview(device = DESKTOP)
@Composable
private fun SettingsSectionDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { SettingsSectionSample() }
