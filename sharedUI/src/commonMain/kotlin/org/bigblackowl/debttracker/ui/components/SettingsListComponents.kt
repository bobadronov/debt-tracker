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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.appbar.BackTopAppBar
import org.bigblackowl.debttracker.ui.components.text.CaptionText
import org.bigblackowl.debttracker.ui.components.text.LabelText
import org.bigblackowl.debttracker.ui.components.text.TitleText

/**
 * Screen shell shared by AccountInfoScreen/EditAccountScreen/ActiveSessionsScreen: back-button
 * top bar over a centered, width-capped, vertically-scrollable column — the same "Settings detail
 * page" shape each of those screens otherwise reimplemented on its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    useImePadding: Boolean = false,
    verticalSpacing: Dp = Dimens.Spacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = title, onBack = onBack) },
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .let { if (useImePadding) it.imePadding() else it }
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth).padding(Dimens.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

/**
 * Settings group header (Material You: a caption above a tonal card) + the card itself.
 * Shared style for screens like Settings/Export — a list of tonal cards instead of a flat Column.
 */
@Composable
fun SettingsSection(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.sm)) {
        if (title != null) {
            LabelText(
                title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = Dimens.Spacing.sm),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.Radius.lg),
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

/** One settings row: icon in a tonal circle, title (+optional subtitle), optional trailing control. */
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
    onSubtitleClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (onClick != null) it.clickablePressScale(onClick) else it
            }
            .padding(horizontal = Dimens.Spacing.lg, vertical = Dimens.Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(Dimens.IconSize.md).clip(CircleShape).background(iconContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            if (leadingContent != null) {
                leadingContent()
            } else {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(Dimens.IconSize.sm))
            }
        }
        Spacer(Modifier.width(Dimens.Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            TitleText(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            AnimatedVisibility(visible = subtitle != null, enter = fadeIn(), exit = fadeOut()) {
                CaptionText(
                    subtitle.orEmpty(),
                    textDecoration = if (onSubtitleClick != null) TextDecoration.Underline else null,
                    modifier = if (onSubtitleClick != null) Modifier.clickable(onClick = onSubtitleClick) else Modifier,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Dimens.Spacing.sm))
            trailing()
        }
    }
}

/**
 * [SettingsRow] whose trailing control is a [Switch] — the shape every on/off preference
 * (protection, sound, haptic, run-in-background…) otherwise repeats inline.
 */
@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSubtitleClick: (() -> Unit)? = null,
) {
    SettingsRow(
        icon = icon,
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        onSubtitleClick = onSubtitleClick,
        trailing = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = Dimens.IconSize.xl, end = Dimens.Spacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SettingsSectionSample() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    Column(modifier = Modifier.padding(Dimens.Spacing.lg)) {
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
