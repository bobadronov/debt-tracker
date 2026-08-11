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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import org.bigblackowl.debttracker.core.di.requiresRemoteAuthGate
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.SessionRepository
import org.bigblackowl.debttracker.domain.usecase.ForceSignOutUseCase
import org.bigblackowl.debttracker.ui.screens.accountonboarding.AccountOnboardingScreen
import org.bigblackowl.debttracker.ui.screens.authgate.AuthGateScreen
import org.bigblackowl.debttracker.ui.screens.home.HomeScreen
import org.bigblackowl.debttracker.ui.screens.protectiononboarding.ProtectionOnboardingScreen
import org.bigblackowl.debttracker.ui.screens.splash.SplashDestination
import org.bigblackowl.debttracker.ui.screens.splash.SplashScreen
import org.bigblackowl.debttracker.ui.screens.auth.AuthScreen
import org.bigblackowl.debttracker.ui.screens.creditors.AddEditCreditorScreen
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorDetailScreen
import org.bigblackowl.debttracker.ui.screens.debtors.AddEditDebtorScreen
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
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val focusRequester = remember { FocusRequester() }

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
                        navigate(Screen.AddEditCreditor())
                        true
                    }
                    event.isCtrlPressed && event.key == Key.N -> {
                        navigate(Screen.AddEditDebtor())
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
                    onAddDebtor = { navigate(Screen.AddEditDebtor()) },
                    onOpenDebtor = { id -> navigate(Screen.DebtorDetail(id)) },
                    onAddCreditor = { navigate(Screen.AddEditCreditor()) },
                    onOpenCreditor = { id -> navigate(Screen.CreditorDetail(id)) },
                    onOpenStats = { navigate(Screen.Stats) },
                    onOpenSettings = { navigate(Screen.Settings) },
                )
            }
            entry<Screen.AddEditDebtor> { screen ->
                AddEditDebtorScreen(debtorId = screen.debtorId, onDone = { back() })
            }
            entry<Screen.DebtorDetail> { screen ->
                DebtorDetailScreen(
                    debtorId = screen.debtorId,
                    onBack = { back() },
                    onExport = { navigate(Screen.Export(debtorId = screen.debtorId)) }
                )
            }
            entry<Screen.AddEditCreditor> { screen ->
                AddEditCreditorScreen(creditorId = screen.creditorId, onDone = { back() })
            }
            entry<Screen.CreditorDetail> { screen ->
                CreditorDetailScreen(
                    creditorId = screen.creditorId,
                    onBack = { back() },
                    onExport = { navigate(Screen.Export(creditorId = screen.creditorId)) }
                )
            }
            entry<Screen.Stats> {
                StatsScreen(onBack = { back() })
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
}
