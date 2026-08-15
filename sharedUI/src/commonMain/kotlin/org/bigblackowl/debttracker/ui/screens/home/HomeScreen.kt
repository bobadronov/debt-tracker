package org.bigblackowl.debttracker.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.domain.model.SyncUiStatus
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorListScreen
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorListScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * HomeScreen: верхній TabRow "Мені винні" / "Я винен" (спек §6, п. 3, §4.1).
 * Кожна вкладка — окреме джерело даних (Debtor/Creditor), спільний лише UI-каркас.
 * Індикатор синхронізації (спек §5) — тільки для авторизованих користувачів.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddDebtor: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    onAddCreditor: () -> Unit,
    onOpenCreditor: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQr: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.appName)
                        if (state.isAuthenticated) {
                            Spacer(Modifier.height(Dimens.space2))
                            SyncStatusBadge(status = state.syncStatus, strings = strings)
                        }
                    }
                },
                actions = {
                    // QRKit (core/qr) publishes no working Web scanner — hide the entry point there entirely.
                    if (currentPlatform != AppPlatform.WEB) {
                        TopBarActionIcon(icon = Icons.Filled.QrCode, description = strings.homeQr, onClick = onOpenQr)
                    }
                    TopBarActionIcon(icon = Icons.Filled.QueryStats, description = strings.homeStats, onClick = onOpenStats)
                    TopBarActionIcon(icon = Icons.Filled.Settings, description = strings.homeSettings, onClick = onOpenSettings)
                }
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
                modifier = Modifier.clip( RoundedCornerShape(
                    topStart = Dimens.space16,
                    topEnd = Dimens.space16,
                    bottomStart = Dimens.space0,
                    bottomEnd = Dimens.space0
                )),
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
                    text = { Text(strings.homeTabDebtors) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(strings.homeTabCreditors) }
                )
            }
            // Свайп між вкладками (HorizontalPager) замінює swipe-to-delete на рядках —
            // горизонтальний жест тепер однозначно належить перемиканню Debtor/Creditor.
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> DebtorListScreen(onAddDebtor = onAddDebtor, onOpenDebtor = onOpenDebtor)
                    else -> CreditorListScreen(
                        onAddCreditor = onAddCreditor,
                        onOpenCreditor = onOpenCreditor
                    )
                }
            }
        }}
    }
}

/** Іконка дій top bar із M3 [TooltipBox] — підказка на довге натискання/наведення (Google-style app bar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBarActionIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = description) }
    }
}

/** Tonal-бейдж статусу синхронізації (спек §5): іконка, що обертається під час Syncing, + колір за станом. */
@Composable
private fun SyncStatusBadge(status: SyncUiStatus, strings: Strings) {
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
        SyncUiStatus.Synced -> strings.homeSyncSynced
        SyncUiStatus.Syncing -> strings.homeSyncSyncing
        is SyncUiStatus.OfflinePending -> strings.homeSyncOfflinePending(status.count)
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

    Surface(shape = RoundedCornerShape(Dimens.space6), color = tint.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.space6, vertical = Dimens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Dimens.space12).rotate(rotation))
            Spacer(Modifier.width(Dimens.space4))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

@Composable
private fun HomeScreenPreviewContent() {
    HomeScreen(
        onAddDebtor = {},
        onOpenDebtor = {},
        onAddCreditor = {},
        onOpenCreditor = {},
        onOpenStats = {},
        onOpenSettings = {},
        onOpenQr = {},
    )
}

@Preview
@Composable
private fun HomeScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { HomeScreenPreviewContent() }

@Preview
@Composable
private fun HomeScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { HomeScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun HomeScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { HomeScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun HomeScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { HomeScreenPreviewContent() }
