package org.bigblackowl.debttracker.navigation

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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.bigblackowl.debttracker.core.di.requiresRemoteAuthGate
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.ui.screens.AuthGateScreen
import org.bigblackowl.debttracker.ui.screens.HomeScreen
import org.bigblackowl.debttracker.ui.screens.SplashScreen
import org.bigblackowl.debttracker.ui.screens.auth.AuthScreen
import org.bigblackowl.debttracker.ui.screens.creditors.AddEditCreditorScreen
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorDetailScreen
import org.bigblackowl.debttracker.ui.screens.debtors.AddEditDebtorScreen
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorDetailScreen
import org.bigblackowl.debttracker.ui.screens.export.ExportScreen
import org.bigblackowl.debttracker.ui.screens.settings.EditAccountScreen
import org.bigblackowl.debttracker.ui.screens.settings.SettingsScreen
import org.bigblackowl.debttracker.ui.screens.stats.StatsScreen
import org.koin.compose.koinInject

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
    // offline-capable; Supabase auth there is opt-in sync, reached only from Settings.
    fun screenAfterUnlock(): Screen =
        if (requiresRemoteAuthGate && !authRepository.isAuthenticated.value) Screen.Auth(isGate = true)
        else Screen.Home

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
            entryProvider = entryProvider {
            entry<Screen.Splash> {
                SplashScreen(onFinished = { skipAuthGate ->
                    replaceStackWith(if (skipAuthGate) screenAfterUnlock() else Screen.AuthGate)
                })
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
                    onExport = { navigate(Screen.Export) }
                )
            }
            entry<Screen.AddEditCreditor> { screen ->
                AddEditCreditorScreen(creditorId = screen.creditorId, onDone = { back() })
            }
            entry<Screen.CreditorDetail> { screen ->
                CreditorDetailScreen(
                    creditorId = screen.creditorId,
                    onBack = { back() },
                    onExport = { navigate(Screen.Export) }
                )
            }
            entry<Screen.Stats> {
                StatsScreen(onBack = { back() })
            }
            entry<Screen.Settings> {
                SettingsScreen(
                    onBack = { back() },
                    onExport = { navigate(Screen.Export) },
                    onOpenAuth = { navigate(Screen.Auth()) },
                    onEditAccount = { navigate(Screen.EditAccount) },
                )
            }
            entry<Screen.EditAccount> {
                EditAccountScreen(onBack = { back() })
            }
            entry<Screen.Export> {
                ExportScreen(onBack = { back() })
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
