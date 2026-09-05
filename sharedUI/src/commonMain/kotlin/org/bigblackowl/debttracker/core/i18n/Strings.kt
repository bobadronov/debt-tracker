package org.bigblackowl.debttracker.core.i18n

/**
 * Every user-facing string in the app, in one place, so [AppSettings.locale][org.bigblackowl.debttracker.core.settings.AppSettings.locale]
 * can switch the whole UI at runtime without depending on the OS locale.
 * Fields that need to interpolate a value are functions instead of plain strings.
 *
 * Feature clusters are grouped into nested holders (one per screen/section) instead of one flat
 * constructor — a flat list of ~250 strings sits right at the JVM 255-parameter method limit.
 * Only the handful of strings shared across most screens (`cancel`, `save`, ...) stay top-level.
 */
data class HomeStrings(
    val syncSynced: String,
    val syncSyncing: String,
    val syncOfflinePending: (count: Int) -> String,
    val stats: String,
    val settings: String,
    val tabDebtors: String,
    val tabCreditors: String,
    val menu: String,
)

data class AuthStrings(
    val titleSignUp: String,
    val titleSignIn: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val passwordMismatch: String,
    val submitSignUp: String,
    val submitSignIn: String,
    val toggleToSignIn: String,
    val toggleToSignUp: String,
    val error: String,
    val showPassword: String,
    val hidePassword: String,
)

data class AuthGateStrings(
    val title: String,
    val biometricPrompt: String,
    val retry: String,
    val enterPin: String,
    val pinLabel: String,
    val unlock: String,
    val biometricFailed: String,
    val wrongPin: String,
    val usePinCode: String,
    val useBiometric: String,
    val backspace: String,
    val biometricUnlockReason: String,
    val biometricEnableReason: String,
    val showPin: String,
    val hidePin: String,
)

/** First-launch-only "enable biometric/PIN protection" prompt. */
data class OnboardingProtectionStrings(
    val title: String,
    val body: String,
    val enableBiometric: String,
    val enablePin: String,
    val skip: String,
    val confirmFailed: String,
)

data class AddEditCreditorStrings(
    val titleNew: String,
    val titleEdit: String,
    val initialAmount: String,
)

data class AddEditDebtorStrings(
    val titleNew: String,
    val titleEdit: String,
    val initialAmount: String,
)

/** Step before the Add record form — pick a previously entered person or start fresh. */
data class ContactPickerStrings(
    val title: String,
    val newContact: String,
    val searchPlaceholder: String,
    val empty: String,
)

data class CreditorDetailStrings(
    val titleFallback: String,
    val export: String,
    val balance: (amount: String) -> String,
    val returnLabel: String,
    val borrowMore: String,
    val returnSheetTitle: String,
    val borrowSheetTitle: String,
)

data class DebtorDetailStrings(
    val titleFallback: String,
    val export: String,
    val balance: (amount: String) -> String,
    val repay: String,
    val lendMore: String,
    val repaySheetTitle: String,
    val lendSheetTitle: String,
)

data class CreditorListStrings(
    val searchPlaceholder: String,
    val sort: String,
    val sortByName: String,
    val sortByBalance: String,
    val sortRecent: String,
    val sortReverse: String,
    val filterActive: String,
    val filterClosed: String,
    val filterAll: String,
    val total: String,
)

data class DebtorListStrings(
    val searchPlaceholder: String,
    val sort: String,
    val sortByName: String,
    val sortByBalance: String,
    val sortRecent: String,
    val sortReverse: String,
    val filterActive: String,
    val filterClosed: String,
    val filterAll: String,
    val total: String,
)

data class ExportStrings(
    val title: String,
    val format: String,
    val formatCsv: String,
    val formatPdf: String,
    val direction: String,
    val directionDebtors: String,
    val directionCreditors: String,
    val directionBoth: String,
    val dateRangeHint: String,
    val from: String,
    val to: String,
    val done: String,
    val error: String,
    val submit: String,
    val csvHeaderDate: String,
    val csvHeaderContact: String,
    val csvHeaderAmount: String,
    val csvHeaderComment: String,
    val pdfDescription: (userName: String) -> String,
)

/**
 * Settings → Data "Clear app cache" — nested under [SettingsStrings.clearCache]. Only offered
 * while signed in (Account+Sync) — see [org.bigblackowl.debttracker.domain.usecase.ClearAppCacheUseCase].
 */
data class ClearCacheStrings(
    val title: String,
    val confirmTitle: String,
    val confirmText: String,
    val done: String,
    val failed: String,
)

data class SettingsStrings(
    val title: String,
    val account: String,
    val accountSynced: (userName: String) -> String,
    val signOut: String,
    val signOutConfirmTitle: String,
    val signOutConfirmText: String,
    val localOnly: String,
    val signIn: String,
    val preferences: String,
    val data: String,
    val protection: String,
    val sound: String,
    val haptic: String,
    val notifications: String,
    /** Subtitle shown under the Notifications row when the OS permission was denied. */
    val notificationsBlocked: String,
    // Desktop-only (see AppSettings.runInBackground / desktopApp's main.kt).
    val runInBackground: String,
    val runInBackgroundSubtitle: String,
    /** Tray menu item that brings the window back — from the tray, or from a native minimise. */
    val trayOpen: String,
    /** Window menu item that hides the window behind the tray icon (desktop, "Run in background"). */
    val trayHide: String,
    /** Tray menu action that flushes pending local changes to the server right away. */
    val traySyncNow: String,
    val trayQuit: String,
    val theme: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val language: String,
    val languageSystem: String,
    val exportData: String,
    val deleteAllData: String,
    val deleteAllDataDone: String,
    val deleteAllDataFailed: String,
    val deleteConfirm1Title: String,
    val deleteConfirm1Text: String,
    val deleteConfirm2Title: String,
    val deleteConfirm2Text: String,
    val clearCache: ClearCacheStrings,
    val pinSetupTitle: String,
    val pinSetupNew: String,
    val pinSetupConfirm: String,
    val pinTooShort: String,
    val pinMismatch: String,
    val protectionConfirmFailed: String,
    val avatarUploadError: String,
    val about: String,
    val aboutVersion: String,
    val aboutAuthor: String,
    val checkForUpdates: String,
    val checkingForUpdates: String,
    val upToDate: String,
    /** Settings row label — the whole "Active devices" screen is [ActiveSessionsStrings]. */
    val activeSessions: String,
)

/** Active sessions (Settings → Active devices — session management/remote logout). */
data class ActiveSessionsStrings(
    val title: String,
    val currentDevice: String,
    val lastActive: (date: String) -> String,
    val logOut: String,
    val logOutAllOthers: String,
    val logOutAllOthersConfirmTitle: String,
    val logOutAllOthersConfirmText: String,
    val revokeConfirmTitle: String,
    val revokeConfirmText: (deviceName: String) -> String,
    val error: String,
)

/** Desktop-only update banner (see core/update/AppUpdateChecker). */
data class UpdateStrings(
    val availableTitle: String,
    val availableMessage: (version: String) -> String,
    val downloadInstall: String,
    val later: String,
    val downloading: String,
    val downloadingDetail: (downloaded: String, total: String, speed: String) -> String,
    val failed: String,
    val retry: String,
)

data class StatsStrings(
    val title: String,
    val debtors: String,
    val creditors: String,
    val topDebtors: String,
    val topCreditors: String,
    val monthlyDebtTrend: String,
    val monthlyCreditorTrend: String,
    /** Short month names, index 0 = January, for the monthly trend chart on [org.bigblackowl.debttracker.ui.screens.stats.StatsScreen]. */
    val monthsShort: List<String>,
)

/** org.bigblackowl.debttracker.ui.screens.qr */
data class QrStrings(
    val home: String,
    val hubScanTab: String,
    // Desktop/Web (QR_SCAN_CAPABLE_PLATFORMS): the single share-screen button opens the OS file
    // picker directly instead of the camera scanner.
    val hubSelectImageTab: String,
    val hubMyCardHint: String,
    val hubDescription: String,
    val hubCameraPermissionRationale: String,
    val hubCameraPermissionRetry: String,
    val hubScannedDialogTitle: String,
    val hubScannedDialogMessage: (name: String) -> String,
    val hubScannedAsDebtor: String,
    val hubScannedAsCreditor: String,
    // Desktop/Web have no camera scanner (QR_SCAN_CAPABLE_PLATFORMS) — ContactQrFilePickerContent
    val hubPickFileHint: String,
    val hubPickFileButton: String,
    val hubPickFileNotFound: String,
    val hubPickFileUnsupported: String,
)

/** org.bigblackowl.debttracker.ui.screens.notifications, core/notifications/NotificationsPoller */
data class NotificationsStrings(
    val title: String,
    val empty: String,
    val markAllRead: String,
    val bell: String,
)

/** [org.bigblackowl.debttracker.core.notifications.NotificationText] / Settings → notifications. */
data class NotificationBodyStrings(
    val debtorLinked: (name: String, amount: String, currency: String) -> String,
    val creditorLinked: (name: String, amount: String, currency: String) -> String,
    val debtTransactionAdded: (name: String, amount: String, currency: String) -> String,
    val creditTransactionAdded: (name: String, amount: String, currency: String) -> String,
    /** "Hide amounts in notifications" toggle label (Settings → Preferences). */
    val hideAmountsToggle: String,
    /** Shown instead of the name/amount detail in the OS notification body when that toggle is on. */
    val genericBody: String,
    /** [org.bigblackowl.debttracker.domain.model.NotificationType.LINK_REQUEST] — shown to the phone-matched target. */
    val linkRequestReceived: (name: String) -> String,
    /** [org.bigblackowl.debttracker.domain.model.NotificationType.LINK_REQUEST_APPROVED] — shown back to the requester. */
    val linkRequestApproved: (name: String) -> String,
    /** Approve/reject buttons on a [org.bigblackowl.debttracker.domain.model.NotificationType.LINK_REQUEST] row. */
    val approveAction: String,
    val rejectAction: String,
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

/** [org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen] — назви джерел (ПриватБанк/НБУ/Monobank) не тут: це власні назви, спільні для всіх локалей ([org.bigblackowl.debttracker.domain.model.RateSource]). */
data class ExchangeRatesStrings(
    /** ⋮ меню + заголовок екрана. */
    val menuTitle: String,
    val sourceLabel: String,
    /** Лейбл селектора базової валюти (активний лише для джерел із довільною базою). */
    val baseLabel: String,
    /** Лейбл поля суми-конвертера (кожен курс множиться на неї). */
    val amountLabel: String,
    /** Placeholder поля пошуку валюти. */
    val searchHint: String,
    /** Заголовок секції закріплених валют + опис зірки. */
    val pinned: String,
    /** Порожній результат пошуку. */
    val noResults: String,
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

/** [org.bigblackowl.debttracker.ui.screens.auth.AuthScreen] extras that don't fit [AuthStrings]. */
data class AuthExtraStrings(
    /** Shown after a failed sign-in, next to [offerSignUpAction], inviting the user to register instead. */
    val offerSignUpPrompt: String,
    val offerSignUpAction: String,
    /** "Continue with Google" button (behind `BuildConfig.GOOGLE_SIGN_IN_ENABLED`). */
    val continueWithGoogle: String,
    /** Divider label between the email form and the Google button ("or"). */
    val divider: String,
    /** Client-side sign-up nudge only — the actual minimum is enforced server-side by Supabase Auth. */
    val passwordTooShort: String,
)

/**
 * Edit/delete a single transaction from a debtor's or creditor's history — the ⋮ row menu, the
 * edit bottom sheet, and the delete confirmation.
 */
data class TransactionEditStrings(
    /** ⋮ menu item + edit sheet title. */
    val editTitle: String,
    /** Field label above the date+time row in the edit sheet. */
    val dateLabel: String,
    val deleteConfirmTitle: String,
    val deleteConfirmText: String,
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

    val home: HomeStrings,
    val auth: AuthStrings,
    val authGate: AuthGateStrings,
    val onboardingProtection: OnboardingProtectionStrings,

    // account onboarding (first launch only)
    val onboardingAccountTitle: String,
    val onboardingAccountBody: String,

    val addEditCreditor: AddEditCreditorStrings,
    val addEditDebtor: AddEditDebtorStrings,
    val contactPicker: ContactPickerStrings,
    val creditorDetail: CreditorDetailStrings,
    val debtorDetail: DebtorDetailStrings,
    val creditorList: CreditorListStrings,
    val debtorList: DebtorListStrings,
    val export: ExportStrings,
    val settings: SettingsStrings,

    // account info (read-only Account screen — Settings → tap the account card; edit/Active
    // devices are separate destinations reached from here)
    val accountInfoEdit: String,

    // edit account
    val editAccountTitle: String,
    val editAccountEmailReadOnly: String,

    val activeSessions: ActiveSessionsStrings,

    // update (Desktop-only)
    val update: UpdateStrings,

    // update (Android-only, see core/update/InAppUpdateLauncher — Play's own flexible update flow)
    val updateReadyToInstall: String,
    val updateRestartNow: String,

    val stats: StatsStrings,

    // widget
    val widgetDebtorsTotal: (amount: String) -> String,
    val widgetCreditorsTotal: (amount: String) -> String,

    val qr: QrStrings,
    val notifications: NotificationsStrings,
    val notificationBody: NotificationBodyStrings,

    // delete confirmation for a debtor/creditor list row (ContactRow)
    val deleteContactConfirmTitle: String,
    val deleteContactConfirmText: (name: String) -> String,

    // per-contact repayment/payment reminder (add/edit form + core/notifications/DueReminderCoordinator)
    val dueReminder: DueReminderStrings,

    val exchangeRates: ExchangeRatesStrings,
    val authExtra: AuthExtraStrings,
    val transactionEdit: TransactionEditStrings,
)
