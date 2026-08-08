package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.media.rememberImagePicker
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.security.BiometricResult
import org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.update.AppUpdateInfo
import org.bigblackowl.debttracker.core.update.appUpdateSupported
import org.bigblackowl.debttracker.core.update.rememberAppUpdateChecker
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.usecase.DeleteAllDataUseCase
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.PinSetupDialog
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.koinInject

/**
 * SettingsScreen: захист/біометрія (спек §6, п.7), тема, звук, експорт,
 * видалення всіх даних (подвійне підтвердження, спек §9.2), Local-only/
 * Account+Sync (спек §1.1, Фаза 6).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onOpenAuth: () -> Unit,
    onEditAccount: () -> Unit,
) {
    val settings = koinInject<AppSettings>()
    val deleteAllData = koinInject<DeleteAllDataUseCase>()
    val authRepository = koinInject<AuthRepository>()
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val imagePicker = rememberImagePicker()
    val scope = rememberCoroutineScope()
    val isAuthenticated by authRepository.isAuthenticated.collectAsState()
    val avatarUrl by authRepository.avatarUrl.collectAsState()
    val accountEmail by authRepository.email.collectAsState()
    val accountName by authRepository.displayName.collectAsState()
    val accountPhone by authRepository.phone.collectAsState()

    var biometricHardwareAvailable by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var protectionConfirmError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm1 by remember { mutableStateOf(false) }
    var showDeleteConfirm2 by remember { mutableStateOf(false) }
    var deleteDone by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        biometricHardwareAvailable = biometricAuthenticator.isAvailable()
    }

    val showProtectionRow = when (currentPlatform) {
        // Web: захисту немає взагалі — вхід уже вимагає email/пароль (Account+Sync-only на Web).
        AppPlatform.WEB -> false
        // Мобільні платформи: лише біометрія, без PIN-фолбеку — перемикач доступний,
        // тільки якщо на пристрої справді є зареєстрована біометрія.
        AppPlatform.ANDROID, AppPlatform.IOS -> biometricHardwareAvailable
        // Desktop: немає нативної біометрії — лише PIN.
        AppPlatform.DESKTOP -> true
    }
    val protectionIcon =
        if (currentPlatform == AppPlatform.DESKTOP) Icons.Filled.Password else Icons.Filled.Fingerprint

    // Тільки Android/iOS мають реальний віброзвінок під керуванням LocalHapticFeedback —
    // на Desktop/Web це або no-op, або взагалі не підтримується, тож перемикач там ховаємо.
    // Only Android/iOS have a real vibration motor behind LocalHapticFeedback — on Desktop/Web
    // it's either a no-op or unsupported, so the toggle is hidden there.
    val showHapticRow = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS

    val updateChecker = rememberAppUpdateChecker()
    var updateState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }

    fun startUpdateCheck() {
        scope.launch {
            updateState = UpdateCheckState.Checking
            updateState = updateChecker.checkForUpdate()
                ?.let { UpdateCheckState.Available(it) }
                ?: UpdateCheckState.UpToDate
        }
    }

    fun startUpdateDownload(info: AppUpdateInfo) {
        scope.launch {
            updateState = UpdateCheckState.Downloading(info)
            runCatching {
                updateChecker.download(info) { progress -> updateState = UpdateCheckState.Downloading(info, progress) }
            }.onSuccess { path ->
                updateChecker.installAndExit(path)
            }.onFailure {
                updateState = UpdateCheckState.Failed(info)
            }
        }
    }

    PlaceholderScreen(title = strings.settingsTitle, onBack = onBack) {

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space24),
            ) {
                // --- Account ---
                SettingsSection(strings.settingsAccount) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccountAvatar(
                            avatarUrl = avatarUrl,
                            isUploading = isUploadingAvatar,
                            onEditClick = {
                                imagePicker.pickImage { picked ->
                                    if (picked == null) return@pickImage
                                    scope.launch {
                                        isUploadingAvatar = true
                                        avatarError = null
                                        authRepository.updateAvatar(
                                            picked.bytes,
                                            picked.fileExtension
                                        )
                                            .onFailure {
                                                avatarError = strings.settingsAvatarUploadError
                                            }
                                        isUploadingAvatar = false
                                    }
                                }
                            },
                        )
                        Spacer(Modifier.width(Dimens.space16))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isAuthenticated) strings.settingsAccountSynced else strings.settingsLocalOnly,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            avatarError?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.debtAccentColors.debt
                                )
                            }
                        }
                    }
                    if (isAuthenticated) {
                        accountName?.takeIf(String::isNotBlank)?.let {
                            SettingsRowDivider()
                            SettingsRow(icon = Icons.Default.Person, title = strings.fullName, subtitle = it)
                        }
                        accountEmail?.takeIf(String::isNotBlank)?.let {
                            SettingsRowDivider()
                            SettingsRow(icon = Icons.Default.Email, title = strings.email, subtitle = it)
                        }
                        accountPhone?.takeIf(String::isNotBlank)?.let {
                            SettingsRowDivider()
                            SettingsRow(icon = Icons.Default.Phone, title = strings.phone, subtitle = it)
                        }
                        SettingsRowDivider()
                        SettingsRow(
                            icon = Icons.Default.Edit,
                            title = strings.settingsEditAccount,
                            onClick = onEditAccount,
                        )
                        SettingsRowDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = strings.settingsSignOut,
                            onClick = { scope.launch { authRepository.signOut() } },
                        )
                    } else {
                        SettingsRowDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.Login,
                            title = strings.settingsSignIn,
                            onClick = onOpenAuth,
                        )
                    }
                }

                // --- Preferences ---
                SettingsSection(strings.settingsPreferences) {
                    if (showProtectionRow) {
                        SettingsRow(
                            icon = protectionIcon,
                            title = strings.settingsProtection,
                            subtitle = protectionConfirmError,
                            trailing = {
                                Switch(
                                    checked = settings.protectionEnabled,
                                    onCheckedChange = { checked ->
                                        protectionConfirmError = null
                                        when (currentPlatform) {
                                            AppPlatform.DESKTOP -> if (checked && settings.pinCode == null) {
                                                showPinSetupDialog = true
                                            } else {
                                                settings.protectionEnabled = checked
                                            }

                                            // Мобільні платформи: увімкнення захисту потребує підтвердження
                                            // відбитком/обличчям одразу — інакше можна ввімкнути перемикач,
                                            // маючи чужий палець на сканері, і сам захист виявиться фікцією.
                                            else -> if (checked) {
                                                scope.launch {
                                                    when (biometricAuthenticator.authenticate(strings.biometricEnableReason)) {
                                                        BiometricResult.SUCCESS -> {
                                                            settings.protectionEnabled = true
                                                            settings.biometricEnabled = true
                                                        }
                                                        else -> protectionConfirmError = strings.settingsProtectionConfirmFailed
                                                    }
                                                }
                                            } else {
                                                settings.protectionEnabled = false
                                                settings.biometricEnabled = false
                                            }
                                        }
                                    },
                                )
                            },
                        )
                        SettingsRowDivider()
                    }

                    SettingsRow(
                        icon = if (settings.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        title = strings.settingsSound,
                        trailing = {
                            Switch(
                                checked = settings.soundEnabled,
                                onCheckedChange = { settings.soundEnabled = it })
                        },
                    )
                    SettingsRowDivider()

                    if (showHapticRow) {
                        SettingsRow(
                            icon = Icons.Filled.Vibration,
                            title = strings.settingsHaptic,
                            trailing = {
                                Switch(
                                    checked = settings.hapticEnabled,
                                    onCheckedChange = { settings.hapticEnabled = it })
                            },
                        )
                        SettingsRowDivider()
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(Dimens.space16)) {
                        Text(strings.settingsTheme, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(Dimens.space12))
                        val themeOptions = listOf(
                            "system" to strings.settingsThemeSystem,
                            "light" to strings.settingsThemeLight,
                            "dark" to strings.settingsThemeDark,
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themeOptions.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = settings.theme == value,
                                    onClick = { settings.theme = value },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = themeOptions.size
                                    ),
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                    SettingsRowDivider()

                    Column(modifier = Modifier.fillMaxWidth().padding(Dimens.space16)) {
                        Text(strings.settingsLanguage, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(Dimens.space12))
                        val languageOptions = listOf(
                            "system" to strings.settingsLanguageSystem,
                            "uk" to "Українська",
                            "en" to "English",
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            languageOptions.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = settings.locale == value,
                                    onClick = { settings.locale = value },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = languageOptions.size
                                    ),
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }

                // --- Data ---
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
                    SettingsSection(strings.settingsData) {
                        SettingsRow(
                            icon = Icons.Filled.Download,
                            title = strings.settingsExportData,
                            onClick = onExport,
                        )
                        SettingsRowDivider()
                        SettingsRow(
                            icon = Icons.Filled.DeleteForever,
                            title = strings.settingsDeleteAllData,
                            titleColor = MaterialTheme.debtAccentColors.debt,
                            iconTint = MaterialTheme.debtAccentColors.debt,
                            iconContainerColor = MaterialTheme.debtAccentColors.debt.copy(alpha = 0.12f),
                            onClick = { showDeleteConfirm1 = true },
                        )
                    }
                    if (deleteDone) {
                        Text(
                            strings.settingsDeleteAllDataDone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.debtAccentColors.repay,
                            modifier = Modifier.padding(start = Dimens.space8),
                        )
                    }
                }

                // --- About ---
                SettingsSection(strings.settingsAbout) {
                    val versionLine = "${BuildConfig.APP_VERSION} (${BuildConfig.APP_VERSION_CODE})"
                    val versionSubtitle = when (val s = updateState) {
                        UpdateCheckState.Idle -> versionLine
                        UpdateCheckState.Checking -> "$versionLine · ${strings.settingsCheckingForUpdates}"
                        UpdateCheckState.UpToDate -> "$versionLine · ${strings.settingsUpToDate}"
                        is UpdateCheckState.Available -> strings.updateAvailableMessage(s.info.version)
                        is UpdateCheckState.Downloading -> strings.updateDownloading
                        is UpdateCheckState.Failed -> strings.updateFailed
                    }
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        title = strings.settingsAboutVersion,
                        subtitle = versionSubtitle,
                        trailing = if (appUpdateSupported) {
                            {
                                when (val s = updateState) {
                                    UpdateCheckState.Checking, is UpdateCheckState.Downloading ->
                                        CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))

                                    is UpdateCheckState.Available -> IconButton(onClick = { startUpdateDownload(s.info) }) {
                                        Icon(Icons.Filled.Download, contentDescription = strings.updateDownloadInstall)
                                    }

                                    is UpdateCheckState.Failed -> IconButton(onClick = { startUpdateDownload(s.info) }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = strings.updateRetry)
                                    }

                                    UpdateCheckState.Idle, UpdateCheckState.UpToDate -> IconButton(onClick = { startUpdateCheck() }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = strings.settingsCheckForUpdates)
                                    }
                                }
                            }
                        } else null,
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = strings.settingsAboutAuthor,
                        subtitle = BuildConfig.APP_AUTHOR,
                    )
                }
            }
        }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                settings.pinCode = pin
                settings.protectionEnabled = true
                showPinSetupDialog = false
            },
        )
    }

    if (showDeleteConfirm1) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm1 = false },
            title = { Text(strings.settingsDeleteConfirm1Title) },
            text = { Text(strings.settingsDeleteConfirm1Text) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm1 = false; showDeleteConfirm2 = true
                }) { Text(strings.continueLabel) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm1 = false
                }) { Text(strings.cancel) }
            },
        )
    }

    if (showDeleteConfirm2) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm2 = false },
            title = { Text(strings.settingsDeleteConfirm2Title) },
            text = { Text(strings.settingsDeleteConfirm2Text) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm2 = false
                    scope.launch {
                        deleteAllData()
                        deleteDone = true
                    }
                }) { Text(strings.deleteForever) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm2 = false
                }) { Text(strings.cancel) }
            },
        )
    }
}

/** Settings' own on-demand update check — independent of [org.bigblackowl.debttracker.ui.components.UpdateBanner]'s automatic on-launch check. */
private sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val info: AppUpdateInfo) : UpdateCheckState
    data class Downloading(val info: AppUpdateInfo, val progress: Float? = null) : UpdateCheckState
    data class Failed(val info: AppUpdateInfo) : UpdateCheckState
}

@Preview
@Composable
private fun SettingsScreenPreview() = DebtTrackerPreview {
    SettingsScreen(onBack = {}, onExport = {}, onOpenAuth = {}, onEditAccount = {})
}

@Preview(device = DESKTOP)
@Composable
private fun SettingsScreenPreview2() = DebtTrackerPreview {
    SettingsScreen(onBack = {}, onExport = {}, onOpenAuth = {}, onEditAccount = {})
}

/** Кругле фото акаунта (ініціали-заглушка через [Icons.Default.Person] поки фото немає) з кнопкою редагування. */
@Composable
private fun AccountAvatar(avatarUrl: String?, isUploading: Boolean, onEditClick: () -> Unit) {
    Box(modifier = Modifier.size(Dimens.space72)) {
        Box(
            modifier = Modifier
                .size(Dimens.space72)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.space72).clip(CircleShape),
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.space40),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isUploading) {
                CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space72))
            }
        }
        FilledIconButton(
            onClick = onEditClick,
            enabled = !isUploading,
            modifier = Modifier.align(Alignment.BottomEnd).size(Dimens.space28),
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(Dimens.space16)
            )
        }
    }
}

