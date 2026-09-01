package org.bigblackowl.debttracker.core.i18n

/**
 * Every user-facing string in the app, in one place, so [AppSettings.locale][org.bigblackowl.debttracker.core.settings.AppSettings.locale]
 * can switch the whole UI at runtime without depending on the OS locale.
 * Fields that need to interpolate a value are functions instead of plain strings.
 *
 * The flat constructor is close to the JVM 255-parameter method limit, so newer feature clusters
 * are grouped into nested holders (e.g. [dueReminder]) rather than adding many top-level fields.
 */
/** [org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen] — назви джерел (ПриватБанк/НБУ/Monobank) не тут: це власні назви, спільні для всіх локалей ([org.bigblackowl.debttracker.domain.model.RateSource]). */
data class ExchangeRatesStrings(
    /** ⋮ меню + заголовок екрана. */
    val menuTitle: String,
    val sourceLabel: String,
    /** Підзаголовок під джерелом: у якій валюті котирування ([base] — напр. "UAH ₴"). */
    val quotedIn: (base: String) -> String,
    val updated: (date: String) -> String,
    val buy: String,
    val sell: String,
    /** Підпис єдиного курсу (НБУ — без купівлі/продажу). */
    val official: String,
    val refresh: String,
    val error: String,
    /** Банер, коли оновлення впало, але лишився попередній зріз. */
    val stale: String,
)

data class DueReminderStrings(
    /** Form field label + the notification title. */
    val label: String,
    val notSet: String,
    /** Accessibility label for the clear (X) button. */
    val clear: String,
    /** "On the day" chip — always selected (the on-the-day reminder can't be turned off). */
    val leadOnDay: String,
    val lead1Day: String,
    val lead2Days: String,
    /** [whenText] is [whenToday]'s output for the on-the-day reminder, or a plain "DD.MM.YYYY, HH:MM" for the lead ones. */
    val debtorBody: (name: String, whenText: String) -> String,
    val creditorBody: (name: String, whenText: String) -> String,
    val whenToday: (time: String) -> String,
)

data class Strings(
    // common
    val cancel: String,
    val save: String,
    val edit: String,
    val delete: String,
    val deleteForever: String,
    val continueLabel: String,
    val back: String,
    val cash: String,
    val card: String,
    val fullName: String,
    val fullNamePlaceholder: String,
    val phone: String,
    val email: String,
    val comment: String,
    val fullNameError: String,
    val amountError: String,
    val amount: String,
    val currency: String,
    val confirm: String,
    val saveError: String,
    val deleteError: String,
    val contactSuggestionFound: String,
    val contactSuggestionUse: String,
    val clipboardPasteFound: String,
    val clipboardPasteUse: String,
    val clearSearch: String,

    // app
    val appName: String,

    // home
    val homeSyncSynced: String,
    val homeSyncSyncing: String,
    val homeSyncOfflinePending: (count: Int) -> String,
    val homeStats: String,
    val homeSettings: String,
    val homeTabDebtors: String,
    val homeTabCreditors: String,
    val homeMenu: String,

    // auth
    val authTitleSignUp: String,
    val authTitleSignIn: String,
    val authEmail: String,
    val authPassword: String,
    val authConfirmPassword: String,
    val authPasswordMismatch: String,
    val authSubmitSignUp: String,
    val authSubmitSignIn: String,
    val authToggleToSignIn: String,
    val authToggleToSignUp: String,
    val authError: String,
    /** Shown after a failed sign-in, next to [authOfferSignUpAction], inviting the user to register instead. */
    val authOfferSignUpPrompt: String,
    val authOfferSignUpAction: String,
    val showPassword: String,
    val hidePassword: String,

    // auth gate
    val authGateTitle: String,
    val authGateBiometricPrompt: String,
    val authGateRetry: String,
    val authGateEnterPin: String,
    val authGatePinLabel: String,
    val authGateUnlock: String,
    val authGateBiometricFailed: String,
    val authGateWrongPin: String,
    val authGateUsePinCode: String,
    val authGateUseBiometric: String,
    val authGateBackspace: String,
    val biometricUnlockReason: String,
    val biometricEnableReason: String,
    val showPin: String,
    val hidePin: String,

    // protection onboarding (first launch only)
    val onboardingProtectionTitle: String,
    val onboardingProtectionBody: String,
    val onboardingProtectionEnableBiometric: String,
    val onboardingProtectionEnablePin: String,
    val onboardingProtectionSkip: String,
    val onboardingProtectionConfirmFailed: String,

    // account onboarding (first launch only)
    val onboardingAccountTitle: String,
    val onboardingAccountBody: String,

    // add/edit creditor
    val addEditCreditorTitleNew: String,
    val addEditCreditorTitleEdit: String,
    val addEditCreditorInitialAmount: String,

    // add/edit debtor
    val addEditDebtorTitleNew: String,
    val addEditDebtorTitleEdit: String,
    val addEditDebtorInitialAmount: String,

    // contact picker (step before the Add record form — pick a previously entered person or start fresh)
    val contactPickerTitle: String,
    val contactPickerNewContact: String,
    val contactPickerSearchPlaceholder: String,
    val contactPickerEmpty: String,

    // creditor detail
    val creditorDetailTitleFallback: String,
    val creditorDetailExport: String,
    val creditorDetailBalance: (amount: String) -> String,
    val creditorDetailReturn: String,
    val creditorDetailBorrowMore: String,
    val creditorDetailReturnSheetTitle: String,
    val creditorDetailBorrowSheetTitle: String,

    // debtor detail
    val debtorDetailTitleFallback: String,
    val debtorDetailExport: String,
    val debtorDetailBalance: (amount: String) -> String,
    val debtorDetailRepay: String,
    val debtorDetailLendMore: String,
    val debtorDetailRepaySheetTitle: String,
    val debtorDetailLendSheetTitle: String,

    // creditor list
    val creditorListSearchPlaceholder: String,
    val creditorListSort: String,
    val creditorListSortByName: String,
    val creditorListSortByBalance: String,
    val creditorListSortRecent: String,
    val creditorListSortReverse: String,
    val creditorListFilterActive: String,
    val creditorListFilterClosed: String,
    val creditorListFilterAll: String,
    val creditorListTotal: String,

    // debtor list
    val debtorListSearchPlaceholder: String,
    val debtorListSort: String,
    val debtorListSortByName: String,
    val debtorListSortByBalance: String,
    val debtorListSortRecent: String,
    val debtorListSortReverse: String,
    val debtorListFilterActive: String,
    val debtorListFilterClosed: String,
    val debtorListFilterAll: String,
    val debtorListTotal: String,

    // export
    val exportTitle: String,
    val exportFormat: String,
    val exportFormatCsv: String,
    val exportFormatPdf: String,
    val exportDirection: String,
    val exportDirectionDebtors: String,
    val exportDirectionCreditors: String,
    val exportDirectionBoth: String,
    val exportDateRangeHint: String,
    val exportFrom: String,
    val exportTo: String,
    val exportDone: String,
    val exportError: String,
    val exportSubmit: String,
    val csvHeaderDate: String,
    val csvHeaderContact: String,
    val csvHeaderAmount: String,
    val csvHeaderComment: String,
    val exportPdfDescription: (userName: String) -> String,

    // settings
    val settingsTitle: String,
    val settingsAccount: String,
    val settingsAccountSynced: (userName: String) -> String,
    val settingsSignOut: String,
    val settingsSignOutConfirmTitle: String,
    val settingsSignOutConfirmText: String,
    val settingsLocalOnly: String,
    val settingsSignIn: String,
    val settingsPreferences: String,
    val settingsData: String,
    val settingsProtection: String,
    val settingsSound: String,
    val settingsHaptic: String,
    val settingsNotifications: String,
    /** Subtitle shown under the Notifications row when the OS permission was denied. */
    val settingsNotificationsBlocked: String,
    // Desktop-only (see AppSettings.runInBackground / desktopApp's main.kt).
    val settingsRunInBackground: String,
    val settingsRunInBackgroundSubtitle: String,
    /** Tray menu item that brings the window back — from the tray, or from a native minimise. */
    val trayOpen: String,
    /** Window menu item that hides the window behind the tray icon (desktop, "Run in background"). */
    val trayHide: String,
    /** Tray menu action that flushes pending local changes to the server right away. */
    val traySyncNow: String,
    val trayQuit: String,
    val settingsTheme: String,
    val settingsThemeSystem: String,
    val settingsThemeLight: String,
    val settingsThemeDark: String,
    val settingsLanguage: String,
    val settingsLanguageSystem: String,
    val settingsExportData: String,
    val settingsDeleteAllData: String,
    val settingsDeleteAllDataDone: String,
    val settingsDeleteAllDataFailed: String,
    val settingsDeleteConfirm1Title: String,
    val settingsDeleteConfirm1Text: String,
    val settingsDeleteConfirm2Title: String,
    val settingsDeleteConfirm2Text: String,
    val settingsPinSetupTitle: String,
    val settingsPinSetupNew: String,
    val settingsPinSetupConfirm: String,
    val settingsPinTooShort: String,
    val settingsPinMismatch: String,
    val settingsProtectionConfirmFailed: String,
    val settingsAvatarUploadError: String,
    val settingsAbout: String,
    val settingsAboutVersion: String,
    val settingsAboutAuthor: String,
    val settingsCheckForUpdates: String,
    val settingsCheckingForUpdates: String,
    val settingsUpToDate: String,
    val settingsActiveSessions: String,

    // account info (read-only Account screen — Settings → tap the account card; edit/Active
    // devices are separate destinations reached from here)
    val accountInfoEdit: String,

    // edit account
    val editAccountTitle: String,
    val editAccountEmailReadOnly: String,

    // active sessions (Settings → Active devices — session management/remote logout)
    val activeSessionsTitle: String,
    val activeSessionsCurrentDevice: String,
    val activeSessionsLastActive: (date: String) -> String,
    val activeSessionsLogOut: String,
    val activeSessionsLogOutAllOthers: String,
    val activeSessionsLogOutAllOthersConfirmTitle: String,
    val activeSessionsLogOutAllOthersConfirmText: String,
    val activeSessionsRevokeConfirmTitle: String,
    val activeSessionsRevokeConfirmText: (deviceName: String) -> String,
    val activeSessionsError: String,

    // update (Desktop-only, see core/update/AppUpdateChecker)
    val updateAvailableTitle: String,
    val updateAvailableMessage: (version: String) -> String,
    val updateDownloadInstall: String,
    val updateLater: String,
    val updateDownloading: String,
    val updateDownloadingDetail: (downloaded: String, total: String, speed: String) -> String,
    val updateFailed: String,
    val updateRetry: String,

    // update (Android-only, see core/update/InAppUpdateLauncher — Play's own flexible update flow)
    val updateReadyToInstall: String,
    val updateRestartNow: String,

    // stats
    val statsTitle: String,
    val statsDebtors: String,
    val statsCreditors: String,
    val statsTopDebtors: String,
    val statsTopCreditors: String,
    val statsMonthlyDebtTrend: String,
    val statsMonthlyCreditorTrend: String,
    /** Short month names, index 0 = January, for the monthly trend chart on [org.bigblackowl.debttracker.ui.screens.stats.StatsScreen]. */
    val monthsShort: List<String>,

    // widget
    val widgetDebtorsTotal: (amount: String) -> String,
    val widgetCreditorsTotal: (amount: String) -> String,

    // qr (org.bigblackowl.debttracker.ui.screens.qr)
    val homeQr: String,
    val qrHubScanTab: String,
    // Desktop/Web (QR_SCAN_CAPABLE_PLATFORMS): the single share-screen button opens the OS file
    // picker directly instead of the camera scanner.
    val qrHubSelectImageTab: String,
    val qrHubMyCardHint: String,
    val qrHubDescription: String,
    val qrHubCameraPermissionRationale: String,
    val qrHubCameraPermissionRetry: String,
    val qrHubScannedDialogTitle: String,
    val qrHubScannedDialogMessage: (name: String) -> String,
    val qrHubScannedAsDebtor: String,
    val qrHubScannedAsCreditor: String,
    // Desktop/Web have no camera scanner (QR_SCAN_CAPABLE_PLATFORMS) — ContactQrFilePickerContent
    val qrHubPickFileHint: String,
    val qrHubPickFileButton: String,
    val qrHubPickFileNotFound: String,
    val qrHubPickFileUnsupported: String,

    // notifications (org.bigblackowl.debttracker.ui.screens.notifications, core/notifications/NotificationsPoller)
    val notificationsTitle: String,
    val notificationsEmpty: String,
    val notificationsMarkAllRead: String,
    val notificationsBell: String,
    val notificationBodyDebtorLinked: (name: String, amount: String, currency: String) -> String,
    val notificationBodyCreditorLinked: (name: String, amount: String, currency: String) -> String,
    val notificationBodyDebtTransactionAdded: (name: String, amount: String, currency: String) -> String,
    val notificationBodyCreditTransactionAdded: (name: String, amount: String, currency: String) -> String,

    // delete confirmation for a debtor/creditor list row (ContactRow)
    val deleteContactConfirmTitle: String,
    val deleteContactConfirmText: (name: String) -> String,

    // per-contact repayment/payment reminder (add/edit form + core/notifications/DueReminderCoordinator)
    val dueReminder: DueReminderStrings,

    // exchange rates (org.bigblackowl.debttracker.ui.screens.exchange)
    val exchangeRates: ExchangeRatesStrings,
)