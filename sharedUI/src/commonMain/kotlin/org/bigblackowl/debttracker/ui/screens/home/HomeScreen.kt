package org.bigblackowl.debttracker.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.core.notifications.rememberNotificationPermissionRequester
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.shortcuts.HomeTabRequest
import org.bigblackowl.debttracker.domain.model.SyncUiStatus
import org.bigblackowl.debttracker.navigation.LocalNavPane
import org.bigblackowl.debttracker.navigation.NavPane
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.appbar.AppOverflowMenu
import org.bigblackowl.debttracker.ui.components.appbar.DesktopTitleBar
import org.bigblackowl.debttracker.ui.components.text.LabelText
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorListScreen
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorListScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

/**
 * HomeScreen: top TabRow "Owes me" / "I owe" (spec §6, item 3, §4.1).
 * Each tab is its own data source (Debtor/Creditor), only the UI shell is shared.
 * The sync indicator (spec §5) is shown only for authenticated users.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddDebtor: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    onAddCreditor: () -> Unit,
    onOpenCreditor: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Asked here, once — the first moment the user actually reaches the app (past onboarding/
    // auth-gate/sign-in) — rather than at raw process start (Android's OS "allow notifications?"
    // prompt used to fire in AppActivity.onCreate before the user had made any choice at all, which
    // read as unrelated to anything since Settings → Notifications isn't even visible without an
    // account yet). Needed regardless of sign-in status: local due-date reminders (spec-only, no
    // account) use the same OS permission as account-linked notifications.
    val appSettings = koinInject<AppSettings>()
    val notificationPermissionRequester = rememberNotificationPermissionRequester()
    LaunchedEffect(Unit) {
        if (!appSettings.notificationsPermissionRequested) {
            notificationPermissionRequester.request()
            appSettings.notificationsPermissionRequested = true
        }
    }

    HomeContent(
        state = state,
        onAddDebtor = onAddDebtor,
        onOpenDebtor = onOpenDebtor,
        onAddCreditor = onAddCreditor,
        onOpenCreditor = onOpenCreditor,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    onAddDebtor: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    onAddCreditor: () -> Unit,
    onOpenCreditor: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    // The Android home-screen widget's two rows deep-link to the matching tab (HomeTabRequest via
    // AppActivity). No-op on other platforms — nothing ever emits there.
    LaunchedEffect(Unit) {
        HomeTabRequest.pending.collect { tab ->
            if (tab != null) {
                pagerState.scrollToPage(tab)
                HomeTabRequest.consume()
            }
        }
    }

    // Desktop: the title (app name) + sync badge go into the native OS title bar (main.kt), which
    // also renders the shared AppOverflowMenu — so Home draws no TopAppBar, only the tabs below.
    // Exception: when Home is the *list* pane of a two-pane view it keeps its own bar (the detail
    // pane owns the title bar).
    val inDesktopTitleBar = DesktopTitleBar.claimed && LocalNavPane.current == NavPane.Full
    if (inDesktopTitleBar) {
        val owner = remember { Any() }
        SideEffect {
            DesktopTitleBar.set(
                owner = owner,
                title = strings.appName,
                back = null,
                actions = {
                    if (state.isAuthenticated) {
                        Spacer(Modifier.width(Dimens.Spacing.sm))
                        SyncStatusBadge(status = state.syncStatus, strings = strings, modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(Modifier.width(Dimens.Spacing.sm))
                    }
                },
            )
        }
        DisposableEffect(Unit) { onDispose { DesktopTitleBar.release(owner) } }
    }

    Scaffold(
        topBar = {
            if (inDesktopTitleBar) return@Scaffold
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.appName)
                        if (state.isAuthenticated) {
                            Spacer(Modifier.width(Dimens.Spacing.xs))
                            SyncStatusBadge(status = state.syncStatus, strings = strings)
                        }
                    }
                },
                actions = { AppOverflowMenu(strings = strings) },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.width(Dimens.contentMaxWidth).padding(padding)) {
                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.clip(
                        RoundedCornerShape(
                            topStart = Dimens.Radius.sm,
                            topEnd = Dimens.Radius.sm,
                            bottomStart = Dimens.Radius.none,
                            bottomEnd = Dimens.Radius.none
                        )
                    ),
                    containerColor = TabRowDefaults.primaryContainerColor,
                    contentColor = TabRowDefaults.primaryContentColor,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTabIndex = pagerState.currentPage)
                        )
                    },
                    divider = { HorizontalDivider() }) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text(strings.home.tabDebtors) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text(strings.home.tabCreditors) }
                    )
                }
                // Swiping between tabs (HorizontalPager) replaces swipe-to-delete on the rows —
                // the horizontal gesture now unambiguously belongs to switching Debtor/Creditor.
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> DebtorListScreen(onAddDebtor = onAddDebtor, onOpenDebtor = onOpenDebtor)
                        else -> CreditorListScreen(
                            onAddCreditor = onAddCreditor,
                            onOpenCreditor = onOpenCreditor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top bar actions (notifications/QR/stats/settings), collapsed into a single overflow menu (⋮)
 * with icons and labels instead of separate buttons — the unread-notifications badge stays visible
 * right on the menu button, without waiting for the list to be expanded.
 */
/** Tonal sync-status badge (spec §5): an icon that spins while Syncing, plus a color by state. */
@Composable
private fun SyncStatusBadge(status: SyncUiStatus, strings: Strings, modifier: Modifier = Modifier) {
    var isBadgeVisible by remember { mutableStateOf(true) }

    val tint = when (status) {
        SyncUiStatus.Synced -> MaterialTheme.debtAccentColors.repay
        SyncUiStatus.Syncing -> MaterialTheme.colorScheme.primary
        is SyncUiStatus.OfflinePending -> MaterialTheme.debtAccentColors.debt
    }

    val icon = when (status) {
        SyncUiStatus.Synced -> Icons.Filled.CloudDone
        SyncUiStatus.Syncing -> Icons.Filled.Sync
        is SyncUiStatus.OfflinePending -> Icons.Filled.CloudOff
    }

    val label = when (status) {
        SyncUiStatus.Synced -> strings.home.syncSynced
        SyncUiStatus.Syncing -> strings.home.syncSyncing
        is SyncUiStatus.OfflinePending -> strings.home.syncOfflinePending(status.count)
    }

    val rotation by if (status == SyncUiStatus.Syncing) {
        val transition = rememberInfiniteTransition(label = "sync-rotation")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
            label = "sync-rotation-angle",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    LaunchedEffect(status) {
        isBadgeVisible = true
        // Stay visible the whole time there's something to report (Syncing / OfflinePending) —
        // only auto-hide once it settles back to Synced.
        if (status == SyncUiStatus.Synced) {
            delay(2.seconds)
            isBadgeVisible = false
        }
    }

    // fadeIn/fadeOut + expand/shrinkHorizontally on the SAME AnimatedVisibility, rather than a
    // separate slide animation on the text plus an independent animateContentSize on the Surface
    // — two independently-timed animations used to fight over the badge's width. This way the
    // text's fade and the badge's width collapse are one animation, driven by one clock.
    val labelAnimationSpec = tween<Float>(durationMillis = 250, easing = LinearEasing)
    val widthAnimationSpec = tween<IntSize>(durationMillis = 250, easing = LinearEasing)

    Surface(modifier = modifier, shape = RoundedCornerShape(Dimens.Radius.sm), color = tint.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.Spacing.sm, vertical = Dimens.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Dimens.Spacing.md).rotate(rotation))
            AnimatedVisibility(
                visible = isBadgeVisible,
                enter = fadeIn(labelAnimationSpec) + expandHorizontally(widthAnimationSpec),
                exit = fadeOut(labelAnimationSpec) + shrinkHorizontally(widthAnimationSpec),
            ) {
                Row {
                    Spacer(Modifier.width(Dimens.Spacing.xs))
                    LabelText(label, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}

@Composable
private fun Preview(state: HomeState) = HomeContent(
    state = state,
    onAddDebtor = {},
    onOpenDebtor = {},
    onAddCreditor = {},
    onOpenCreditor = {},
)

private val PREVIEW_STATE = HomeState(isAuthenticated = true, syncStatus = SyncUiStatus.Synced)

@Preview
@Composable
private fun HomeScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { Preview(PREVIEW_STATE) }

@Preview
@Composable
private fun HomeScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { Preview(PREVIEW_STATE) }

@Preview(device = DESKTOP)
@Composable
private fun HomeScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { Preview(PREVIEW_STATE) }

@Preview(device = DESKTOP)
@Composable
private fun HomeScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { Preview(PREVIEW_STATE) }
