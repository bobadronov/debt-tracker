package org.bigblackowl.debttracker.ui.screens.settings.data

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings → Data (export, clear cache, delete all data) — split out from the former single
 * SettingsScreen.
 */
@Composable
fun SettingsDataScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    viewModel: SettingsDataViewModel = koinViewModel(),
) {
    val authRepository = koinInject<AuthRepository>()
    val isAuthenticated by authRepository.isAuthenticated.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsDataContent(
        state = state,
        isAuthenticated = isAuthenticated,
        onBack = onBack,
        onExport = onExport,
        onClearAppCache = { viewModel.onIntent(SettingsDataIntent.ClearAppCache) },
        onDeleteAllData = { viewModel.onIntent(SettingsDataIntent.DeleteAllData) },
    )
}

@Composable
private fun SettingsDataContent(
    state: SettingsDataState,
    isAuthenticated: Boolean,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onClearAppCache: () -> Unit,
    onDeleteAllData: () -> Unit,
) {
    val strings = LocalStrings.current

    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm1 by remember { mutableStateOf(false) }
    var showDeleteConfirm2 by remember { mutableStateOf(false) }

    PlaceholderScreen(title = strings.settings.data, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.sm),
            ) {
                SettingsSection(strings.settings.data) {
                    SettingsRow(
                        icon = Icons.Filled.Download,
                        title = strings.settings.exportData,
                        onClick = onExport,
                    )
                    // For local-only accounts the local cache is the sole copy of the data: clearing
                    // it without Supabase as a source of truth would be an inconspicuous
                    // DeleteAllDataUseCase without double confirmation, so the row is hidden
                    // (see ClearAppCacheUseCase).
                    if (isAuthenticated) {
                        SettingsRowDivider()
                        SettingsRow(
                            icon = Icons.Filled.CleaningServices,
                            title = strings.settings.clearCache.title,
                            onClick = { showClearCacheConfirm = true },
                        )
                    }
                    SettingsRowDivider()
                    SettingsRow(
                        icon = Icons.Filled.DeleteForever,
                        title = strings.settings.deleteAllData,
                        titleColor = MaterialTheme.debtAccentColors.debt,
                        iconTint = MaterialTheme.debtAccentColors.debt,
                        iconContainerColor = MaterialTheme.debtAccentColors.debt.copy(alpha = 0.12f),
                        onClick = { showDeleteConfirm1 = true },
                    )
                }
                DataResultLine(
                    visible = state.cacheCleared,
                    text = strings.settings.clearCache.done,
                    color = MaterialTheme.debtAccentColors.repay,
                )
                DataResultLine(
                    visible = state.cacheClearError,
                    text = strings.settings.clearCache.failed,
                    color = MaterialTheme.debtAccentColors.debt,
                )
                DataResultLine(
                    visible = state.deleteDone,
                    text = strings.settings.deleteAllDataDone,
                    color = MaterialTheme.debtAccentColors.repay,
                )
                DataResultLine(
                    visible = state.deleteError,
                    text = strings.settings.deleteAllDataFailed,
                    color = MaterialTheme.debtAccentColors.debt,
                )
            }
        }
    }

    if (showClearCacheConfirm) {
        ConfirmDialog(
            title = strings.settings.clearCache.confirmTitle,
            text = strings.settings.clearCache.confirmText,
            confirmLabel = strings.settings.clearCache.title,
            onConfirm = {
                showClearCacheConfirm = false
                onClearAppCache()
            },
            onDismiss = { showClearCacheConfirm = false },
        )
    }

    if (showDeleteConfirm1) {
        ConfirmDialog(
            title = strings.settings.deleteConfirm1Title,
            text = strings.settings.deleteConfirm1Text,
            confirmLabel = strings.continueLabel,
            onConfirm = {
                showDeleteConfirm1 = false; showDeleteConfirm2 = true
            },
            onDismiss = { showDeleteConfirm1 = false },
        )
    }

    if (showDeleteConfirm2) {
        ConfirmDialog(
            title = strings.settings.deleteConfirm2Title,
            text = strings.settings.deleteConfirm2Text,
            confirmLabel = strings.deleteForever,
            onConfirm = {
                showDeleteConfirm2 = false
                onDeleteAllData()
            },
            onDismiss = { showDeleteConfirm2 = false },
        )
    }
}

@Composable
private fun ColumnScope.DataResultLine(visible: Boolean, text: String, color: Color) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        BodyText(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(start = Dimens.Spacing.sm),
        )
    }
}

@Composable
private fun Preview(state: SettingsDataState, isAuthenticated: Boolean = true) = SettingsDataContent(
    state = state,
    isAuthenticated = isAuthenticated,
    onBack = {},
    onExport = {},
    onClearAppCache = {},
    onDeleteAllData = {},
)

@Preview
@Composable
private fun SettingsDataScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { Preview(SettingsDataState()) }

@Preview
@Composable
private fun SettingsDataScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { Preview(SettingsDataState()) }

@Preview(device = DESKTOP)
@Composable
private fun SettingsDataScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { Preview(SettingsDataState()) }

@Preview(device = DESKTOP)
@Composable
private fun SettingsDataScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { Preview(SettingsDataState()) }
