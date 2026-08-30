package org.bigblackowl.debttracker.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.di.requiresRemoteAuthGate
import org.bigblackowl.debttracker.core.notifications.NotificationDeepLinks
import org.bigblackowl.debttracker.core.qr.ContactDeepLinks
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.domain.model.ContactQrPayload
import org.bigblackowl.debttracker.domain.model.DebtDirection
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.domain.model.toPrefill
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.NotificationRepository
import org.bigblackowl.debttracker.domain.repository.SessionRepository
import org.bigblackowl.debttracker.domain.usecase.ForceSignOutUseCase
import org.bigblackowl.debttracker.ui.components.ScannedContactDialog
import org.bigblackowl.debttracker.ui.screens.accountonboarding.AccountOnboardingScreen
import org.bigblackowl.debttracker.ui.screens.authgate.AuthGateScreen
import org.bigblackowl.debttracker.ui.screens.home.HomeScreen
import org.bigblackowl.debttracker.ui.screens.notifications.NotificationsScreen
import org.bigblackowl.debttracker.ui.screens.protectiononboarding.ProtectionOnboardingScreen
import org.bigblackowl.debttracker.ui.screens.qr.QrHubScreen
import org.bigblackowl.debttracker.ui.screens.splash.SplashDestination
import org.bigblackowl.debttracker.ui.screens.splash.SplashScreen
import org.bigblackowl.debttracker.ui.screens.auth.AuthScreen
import org.bigblackowl.debttracker.ui.screens.contacts.AddEditContactScreen
import org.bigblackowl.debttracker.ui.screens.contacts.ContactPickerScreen
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorDetailScreen
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorDetailScreen
import org.bigblackowl.debttracker.ui.screens.export.ExportScreen
import org.bigblackowl.debttracker.ui.screens.settings.AccountInfoScreen
import org.bigblackowl.debttracker.ui.screens.settings.ActiveSessionsScreen
import org.bigblackowl.debttracker.ui.screens.settings.EditAccountScreen
import org.bigblackowl.debttracker.ui.screens.settings.LanguageScreen
import org.bigblackowl.debttracker.ui.screens.settings.SettingsScreen
import org.bigblackowl.debttracker.ui.screens.stats.StatsScreen
import org.koin.compose.koinInject

private const val NAV_TRANSITION_DURATION_MILLIS = 300

private fun NotificationDeepLinks.Target.toScreen(): Screen = when (this) {
    is NotificationDeepLinks.Target.Debtor -> Screen.DebtorDetail(id)
    is NotificationDeepLinks.Target.Creditor -> Screen.CreditorDetail(id)
    NotificationDeepLinks.Target.History -> Screen.Notifications
}

/** Screens where a notification deep link may be acted on — i.e. the user is already unlocked. */
private fun Screen.isPastUnlock(): Boolean = when (this) {
    Screen.Splash, Screen.Onboarding, Screen.AuthGate, Screen.AccountOnboarding -> false
    is Screen.Auth -> !isGate
    else -> true
}

/** iOS already gets a native-feeling slide from Navigation 3's platform default; Desktop/Web get
 * none at all out of the box. Setting this explicitly gives every platform the same slide+fade
 * for every screen change instead of an inconsistent (or missing) default. [towards] is the
 * direction content slides in from — Left to navigate forward, Right to pop back. */
private fun <T : Any> navSlideTransitionSpec(
    towards: AnimatedContentTransitionScope.SlideDirection,
): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    (slideIntoContainer(towards, tween(NAV_TRANSITION_DURATION_MILLIS)) + fadeIn(tween(NAV_TRANSITION_DURATION_MILLIS))) togetherWith
        (slideOutOfContainer(towards, tween(NAV_TRANSITION_DURATION_MILLIS)) + fadeOut(tween(NAV_TRANSITION_DURATION_MILLIS)))
}

/**
 * Навігаційний граф з усіма екранами зі спека §6 (Navigation 3). Desktop
 * hotkeys (спек §6: Ctrl+N/Ctrl+Shift+N/Ctrl+F/Esc) навішені тут через
 * onPreviewKeyEvent — той самий код компілюється на Android/iOS теж
 * (апаратна клавіатура там просто рідкість, не помилка).
 */
@Composable
fun DebtTrackerNavGraph(
    searchFocusRequests: SearchFocusRequests = koinInject(),
    authRepository: AuthRepository = koinInject(),
    sessionRepository: SessionRepository = koinInject(),
    forceSignOut: ForceSignOutUseCase = koinInject(),
    settings: AppSettings = koinInject(),
    notificationRepository: NotificationRepository = koinInject(),
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun replaceStackWith(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    /** Pops entries until [target] is on top — used after saving a record to skip past the
     * contact-picker step that led to the form. Falls back to a single [back] if not found. */
    fun popTo(target: Screen) {
        if (backStack.none { it == target }) {
            back()
            return
        }
        while (backStack.size > 1 && backStack.last() != target) backStack.removeLastOrNull()
    }

    // Web has no local cache (спек §1) — repositories need a signed-in Supabase session to
    // work at all, so it forces the sign-in screen before Home. Other platforms are fully
    // offline-capable; Supabase auth there is opt-in sync — surfaced once via AccountOnboarding
    // (skippable), then only reachable from Settings after that.
    fun screenAfterUnlock(): Screen = when {
        requiresRemoteAuthGate && !authRepository.isAuthenticated.value -> Screen.Auth(isGate = true)
        !settings.hasSeenAccountOnboarding && !authRepository.isAuthenticated.value -> Screen.AccountOnboarding
        else -> Screen.Home
    }

    // A device gets logged out remotely (Settings → Active devices, on some other device) by
    // having its own user_sessions row's revoked_at set — this is the one place that reacts to
    // it, since it's the only spot that already owns the back stack.
    LaunchedEffect(Unit) {
        sessionRepository.revokedElsewhere.collect {
            forceSignOut()
            replaceStackWith(Screen.Auth(isGate = true))
        }
    }

    // Web has no local cache (see screenAfterUnlock() above) — signing out from Settings there
    // must land back on the sign-in screen, since every other screen needs a live session to
    // render anything. Other platforms stay put after sign-out (they're fully offline-capable).
    // drop(1) skips the replay of whatever isAuthenticated already was when this collector
    // started — Splash/screenAfterUnlock() already decided the first screen for that.
    LaunchedEffect(Unit) {
        if (!requiresRemoteAuthGate) return@LaunchedEffect
        authRepository.isAuthenticated.drop(1).collect { authenticated ->
            if (!authenticated) replaceStackWith(Screen.Auth(isGate = true))
        }
    }

    // A third-party QR scanner (Google Lens, the stock camera app) resolving a scanned
    // `debttracker://contact` link opens the app via the OS and hands it to ContactDeepLinks —
    // this is the app-wide landing spot for it, since the link can arrive on top of any screen
    // (or before any screen has even shown, on a cold start). Mirrors QrHubScreen's own in-app
    // camera-scan flow (same dialog, same destinations) so both paths feel identical.
    var pendingDeepLinkContact by remember { mutableStateOf<ScannedContact?>(null) }
    LaunchedEffect(Unit) {
        ContactDeepLinks.pendingLink.collect { rawLink ->
            if (rawLink == null) return@collect
            ContactDeepLinks.consume()
            ContactQrPayload.decode(rawLink)?.let { pendingDeepLinkContact = it }
        }
    }

    // Tapping a system notification (core/notifications/NotificationDeepLinks) — the OS-level twin
    // of tapping a row on NotificationsScreen. Held until the user is past the lock/onboarding
    // screens (a link can arrive on a cold start straight from the notification), then pushed like
    // any other navigation.
    var pendingNotificationRoute by remember { mutableStateOf<NotificationDeepLinks.Route?>(null) }
    LaunchedEffect(Unit) {
        NotificationDeepLinks.pendingLink.collect { rawLink ->
            if (rawLink == null) return@collect
            NotificationDeepLinks.consume()
            NotificationDeepLinks.parse(rawLink)?.let { pendingNotificationRoute = it }
        }
    }
    LaunchedEffect(pendingNotificationRoute, backStack.lastOrNull()) {
        val route = pendingNotificationRoute ?: return@LaunchedEffect
        if (backStack.lastOrNull()?.isPastUnlock() != true) return@LaunchedEffect
        pendingNotificationRoute = null
        // Tapping the OS notification counts as opening it, same as tapping the in-app row.
        // On coroutineScope (not this effect's) so re-keying here doesn't cancel the write.
        route.notificationId?.let { id ->
            coroutineScope.launch { runCatching { notificationRepository.markRead(id) } }
        }
        val screen = route.target.toScreen()
        if (backStack.lastOrNull() != screen) navigate(screen)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.Escape -> {
                        back()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.N && event.isShiftPressed -> {
                        navigate(Screen.ContactPicker(DebtDirection.CREDITOR))
                        true
                    }
                    event.isCtrlPressed && event.key == Key.N -> {
                        navigate(Screen.ContactPicker(DebtDirection.DEBTOR))
                        true
                    }
                    event.isCtrlPressed && event.key == Key.F -> {
                        searchFocusRequests.request()
                        true
                    }
                    else -> false
                }
            },
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        NavDisplay(
            backStack = backStack,
            onBack = { back() },
            // NavDisplay's own default entryDecorators only restores rememberSaveable state per
            // entry, not ViewModels — without rememberViewModelStoreNavEntryDecorator() every
            // koinViewModel() call resolves against the single app-wide ViewModelStoreOwner, so
            // e.g. reopening "Add Debtor" after backing out reuses the same instance and shows
            // whatever was typed last time instead of a blank form.
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = navSlideTransitionSpec(AnimatedContentTransitionScope.SlideDirection.Left),
            popTransitionSpec = navSlideTransitionSpec(AnimatedContentTransitionScope.SlideDirection.Right),
            entryProvider = entryProvider {
            entry<Screen.Splash> {
                SplashScreen(onFinished = { destination ->
                    replaceStackWith(
                        when (destination) {
                            SplashDestination.ONBOARDING -> Screen.Onboarding
                            SplashDestination.AUTH_GATE -> Screen.AuthGate
                            SplashDestination.UNLOCKED -> screenAfterUnlock()
                        }
                    )
                })
            }
            entry<Screen.Onboarding> {
                ProtectionOnboardingScreen(onDone = { replaceStackWith(screenAfterUnlock()) })
            }
            entry<Screen.AccountOnboarding> {
                // isGate = true here too (not just Web's forced gate): the stack was just replaced,
                // so a plain back() would have nothing to pop to — reuse the same "hide back, land
                // on Home after success" handling as the Web gate instead of a second special case.
                AccountOnboardingScreen(
                    onSignIn = { replaceStackWith(Screen.Auth(isGate = true)) },
                    onSkip = { replaceStackWith(Screen.Home) },
                )
            }
            entry<Screen.AuthGate> {
                AuthGateScreen(onUnlocked = { replaceStackWith(screenAfterUnlock()) })
            }
            entry<Screen.Home> {
                HomeScreen(
                    onAddDebtor = { navigate(Screen.ContactPicker(DebtDirection.DEBTOR)) },
                    onOpenDebtor = { id -> navigate(Screen.DebtorDetail(id)) },
                    onAddCreditor = { navigate(Screen.ContactPicker(DebtDirection.CREDITOR)) },
                    onOpenCreditor = { id -> navigate(Screen.CreditorDetail(id)) },
                    onOpenStats = { navigate(Screen.Stats) },
                    onOpenSettings = { navigate(Screen.Settings) },
                    onOpenQr = { navigate(Screen.QrHub) },
                    onOpenNotifications = { navigate(Screen.Notifications) },
                )
            }
            entry<Screen.Notifications> {
                NotificationsScreen(
                    onBack = { back() },
                    onNavigateToDebtor = { id -> navigate(Screen.DebtorDetail(id)) },
                    onNavigateToCreditor = { id -> navigate(Screen.CreditorDetail(id)) },
                )
            }
            entry<Screen.ContactPicker> { screen ->
                ContactPickerScreen(
                    onBack = { back() },
                    onNewContact = { navigate(Screen.AddEditContact(screen.direction)) },
                    onPickContact = { contact ->
                        navigate(Screen.AddEditContact(screen.direction, contact.toPrefill()))
                    },
                )
            }
            entry<Screen.AddEditContact> { screen ->
                AddEditContactScreen(
                    direction = screen.direction,
                    prefill = screen.prefill,
                    onDone = { popTo(Screen.Home) },
                )
            }
            entry<Screen.DebtorDetail> { screen ->
                DebtorDetailScreen(
                    debtorId = screen.debtorId,
                    onBack = { back() },
                    onExport = { navigate(Screen.Export(debtorId = screen.debtorId)) }
                )
            }
            entry<Screen.CreditorDetail> { screen ->
                CreditorDetailScreen(
                    creditorId = screen.creditorId,
                    onBack = { back() },
                    onExport = { navigate(Screen.Export(creditorId = screen.creditorId)) }
                )
            }
            entry<Screen.Stats> {
                StatsScreen(
                    onBack = { back() },
                    onOpenDebtor = { id -> navigate(Screen.DebtorDetail(id)) },
                    onOpenCreditor = { id -> navigate(Screen.CreditorDetail(id)) },
                )
            }
            entry<Screen.Settings> {
                SettingsScreen(
                    onBack = { back() },
                    onExport = { navigate(Screen.Export()) },
                    onOpenAuth = { navigate(Screen.Auth()) },
                    onOpenAccountInfo = { navigate(Screen.AccountInfo) },
                    onOpenLanguage = { navigate(Screen.Language) },
                )
            }
            entry<Screen.Language> {
                LanguageScreen(onBack = { back() })
            }
            entry<Screen.AccountInfo> {
                AccountInfoScreen(
                    onBack = { back() },
                    onEdit = { navigate(Screen.EditAccount) },
                    onOpenActiveSessions = { navigate(Screen.ActiveSessions) },
                )
            }
            entry<Screen.EditAccount> {
                EditAccountScreen(onBack = { back() })
            }
            entry<Screen.ActiveSessions> {
                ActiveSessionsScreen(onBack = { back() })
            }
            entry<Screen.Export> { screen ->
                ExportScreen(onBack = { back() }, debtorId = screen.debtorId, creditorId = screen.creditorId)
            }
            entry<Screen.QrHub> {
                QrHubScreen(
                    onBack = { back() },
                    onNavigateToAddDebtor = { contact ->
                        navigate(Screen.AddEditContact(DebtDirection.DEBTOR, contact.toPrefill()))
                    },
                    onNavigateToAddCreditor = { contact ->
                        navigate(Screen.AddEditContact(DebtDirection.CREDITOR, contact.toPrefill()))
                    },
                )
            }
            entry<Screen.Auth> { screen ->
                AuthScreen(
                    onBack = { back() },
                    onAuthenticated = { if (screen.isGate) replaceStackWith(Screen.Home) else back() },
                    showBackButton = !screen.isGate,
                )
            }
            }
        )
    }

    pendingDeepLinkContact?.let { contact ->
        ScannedContactDialog(
            contact = contact,
            onDismiss = { pendingDeepLinkContact = null },
            onAddAsDebtor = {
                pendingDeepLinkContact = null
                navigate(Screen.AddEditContact(DebtDirection.DEBTOR, contact.toPrefill()))
            },
            onAddAsCreditor = {
                pendingDeepLinkContact = null
                navigate(Screen.AddEditContact(DebtDirection.CREDITOR, contact.toPrefill()))
            },
        )
    }
}
