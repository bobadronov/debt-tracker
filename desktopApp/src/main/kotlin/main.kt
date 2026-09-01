
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nucleusframework.application.NucleusWindow
import dev.nucleusframework.application.SingleInstanceRestoreEffect
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.composenativetray.tray.api.Tray
import dev.nucleusframework.window.material.MaterialDecoratedWindow
import dev.nucleusframework.window.material.MaterialTitleBar
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.notifications.DesktopNotificationWindow
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.bigblackowl.debttracker.domain.model.SyncUiStatus
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.bigblackowl.debttracker.navigation.AppMenu
import org.bigblackowl.debttracker.navigation.CurrentScreen
import org.bigblackowl.debttracker.navigation.windowTitle
import org.bigblackowl.debttracker.theme.rememberAppColorScheme
import org.bigblackowl.debttracker.ui.components.AppOverflowMenu
import org.bigblackowl.debttracker.ui.components.DesktopTitleBar
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.core.Koin

private const val APP_NAME = "Debt Tracker"
private const val APP_ID = "org.bigblackowl.debttracker"

/** Desktop (JVM) entry point: starts Koin + background sync/notifications, then opens the app window. */
fun main(args: Array<String>) {
    // Nucleus reads this for the single-instance lock and the Windows toast AUMID / Start Menu
    // shortcut (notification-common); ignored on Linux/macOS. Set before nucleusApplication().
    System.setProperty("nucleus.app.id", APP_ID)
    DesktopTitleBar.claim() // screens route their TopAppBar into the native title bar from here on
    FileKit.init(appId = APP_ID) // required for FileKit's Save-As dialogs and cache/files dirs on JVM
    val koinApp = initKoin()
    koinApp.koin.get<SyncCoordinator>().start()
    koinApp.koin.get<NotificationsPoller>().start()
    koinApp.koin.get<org.bigblackowl.debttracker.core.notifications.DueReminderCoordinator>().start()
    startApp(koinApp.koin, args)
}

/**
 * Runs on Nucleus' Tao (Rust-native) window backend rather than Compose Desktop's AWT one, so the
 * main window gets real OS decorations with a Material 3 [MaterialTitleBar]. `nucleusApplication`
 * also owns the single-instance lock: a second launch is routed to the running instance via
 * [SingleInstanceRestoreEffect] instead of starting a new process.
 *
 * [AppSettings.runInBackground] (Settings → "Run in background", desktop-only) changes what the
 * window's close button does: on, it hides the window behind a tray icon instead of exiting, so
 * background sync keeps running; off, it behaves like a normal window and exits the process.
 */
private fun startApp(koin: Koin, args: Array<String>) = nucleusApplication(args) {
    val appScope = this // MaterialTheme { } below drops the NucleusApplicationScope receiver
    val settings = remember { koin.get<AppSettings>() }
    val uiScope = rememberCoroutineScope()

    var isWindowVisible by remember { mutableStateOf(true) }
    // Captured from inside MaterialDecoratedWindow's content — lets a tray click / notification tap
    // surface the window imperatively. Toggling `visible` from a ComposeNativeTray callback thread
    // alone isn't enough: the window stays hidden until the composition next recomposes.
    val windowHandle = remember { mutableStateOf<NucleusWindow?>(null) }

    // The tray "show / hide" item must flip on BOTH states, not just `isWindowVisible`: a window the
    // user shrank with the native minimise button is still `visible`, but the item should then read
    // "restore", not "hide to tray". `minimizedFlow` gives us the real minimise state.
    var isWindowMinimized by remember { mutableStateOf(false) }
    LaunchedEffect(windowHandle.value) {
        windowHandle.value?.minimizedFlow?.collect { isWindowMinimized = it }
    }
    val isWindowShown = isWindowVisible && !isWindowMinimized

    // `uiScope.launch` marshals the (possibly off-thread) call onto the composition dispatcher.
    val surfaceWindow: () -> Unit = {
        uiScope.launch {
            isWindowVisible = true
            windowHandle.value?.apply {
                setMinimized(false)
                show()
                toFront()
                requestFocus()
            }
        }
    }

    // Second launch while we're already running (incl. from behind the tray) → surface the window.
    SingleInstanceRestoreEffect { surfaceWindow() }

    // Tapping a system notification's banner (core/notifications) un-hides the window when
    // "Run in background" had tucked it away behind the tray.
    DisposableEffect(Unit) {
        DesktopNotificationWindow.bringToFront = surfaceWindow
        onDispose { DesktopNotificationWindow.bringToFront = null }
    }

    val appIcon = remember { windowIcon() }

    val strings = remember(settings.locale) { resolveStrings(settings.locale) }

    // The visible screen routes its title / back button / action icons into the native title bar
    // (via DesktopTitleBar); CurrentScreen.windowTitle is the fallback for screens that don't.
    val currentScreen by CurrentScreen.screen.collectAsState()
    val titleBar by DesktopTitleBar.content.collectAsState()
    val windowTitle = titleBar.title ?: currentScreen?.windowTitle(strings) ?: APP_NAME

    // ComposeNativeTray (dev.nucleusframework) instead of Compose's built-in Tray: HDPI-correct on
    // Windows/Linux and its menu items support icons. Fully reactive — the menu re-reads the unread
    // count / sync status / locale / window state with no manual teardown.
    if (settings.runInBackground && appIcon != null) {
        TrayMenu(
            appIcon = appIcon,
            strings = strings,
            isWindowShown = isWindowShown,
            settings = settings,
            poller = remember { koin.get() },
            syncStatusProvider = remember { koin.get() },
            authRepository = remember { koin.get() },
            onOpen = surfaceWindow,
            onHide = { uiScope.launch { isWindowVisible = false } },
            onQuit = ::exitApplication,
        )
    }

    // Wrapped in the app's own color scheme so the native title bar + window chrome match the
    // in-app theme (light/dark per Settings), not Nucleus' default Material palette.
    MaterialTheme(colorScheme = rememberAppColorScheme()) {
        appScope.MaterialDecoratedWindow(
            title = windowTitle, // OS window title (taskbar / alt-tab)
            icon = appIcon,
            state = rememberWindowState(width = 800.dp, height = 600.dp),
            visible = isWindowVisible,
            minimumSize = DpSize(350.dp, 600.dp),
            onCloseRequest = {
                if (settings.runInBackground) isWindowVisible = false else exitApplication()
            },
        ) {
            LaunchedEffect(nucleusWindow) { windowHandle.value = nucleusWindow }

            MaterialTitleBar {
                titleBar.back?.let { onBack ->
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                        BackChevron()
                    }
                }
                Text(windowTitle, modifier = Modifier.align(Alignment.CenterHorizontally))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    titleBar.actions?.invoke(this)
                    AppOverflowMenu(strings = strings) // renders nothing on the lock / onboarding screens
                }
            }
            App()
        }
    }
}

/** Left-pointing chevron drawn to the current content colour — the title-bar back affordance. */
@Composable
private fun BackChevron() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.16f
        drawLine(color, Offset(w * 0.62f, h * 0.18f), Offset(w * 0.34f, h * 0.5f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.5f), Offset(w * 0.62f, h * 0.82f), stroke, StrokeCap.Round)
    }
}

/**
 * System-tray icon + a fully reactive "control centre" menu. Left-click opens the window; the menu
 * mirrors the app menu (Notifications with unread count, quick-add, Stats, Settings), plus a
 * "sync now" action with live status and a "run in background" toggle. Everything past the first
 * two items is gated on [AppMenu] being visible, i.e. the user is past the lock / onboarding
 * screens. Split out so `dispose()` (a member of the tray menu DSL scope) reads cleanly.
 */
@Composable
private fun TrayMenu(
    appIcon: Painter,
    strings: Strings,
    /** Window is on screen and not minimised — drives whether the first item hides or restores it. */
    isWindowShown: Boolean,
    settings: AppSettings,
    poller: NotificationsPoller,
    syncStatusProvider: SyncStatusProvider,
    authRepository: AuthRepository,
    onOpen: () -> Unit,
    onHide: () -> Unit,
    onQuit: () -> Unit,
) {
    val menu by AppMenu.state.collectAsState()
    val unread by poller.unreadCount.collectAsState()
    val isAuthenticated by authRepository.isAuthenticated.collectAsState()
    val syncStatus by syncStatusProvider.status.collectAsState()
    val scope = rememberCoroutineScope()

    val syncLabel = when (val s = syncStatus) {
        SyncUiStatus.Synced -> strings.homeSyncSynced
        SyncUiStatus.Syncing -> strings.homeSyncSyncing
        is SyncUiStatus.OfflinePending -> strings.homeSyncOfflinePending(s.count)
    }
    val notificationsLabel =
        if (isAuthenticated && unread > 0) "${strings.notificationsTitle} ($unread)" else strings.notificationsTitle

    Tray(
        icon = appIcon,
        tooltip = buildString {
            append(APP_NAME)
            if (isAuthenticated) append(" · ").append(if (unread > 0) notificationsLabel else syncLabel)
        },
        // Left-click / double-click toggles: hide a shown window, restore a hidden/minimised one.
        primaryAction = { if (isWindowShown) onHide() else onOpen() },
    ) {
        if (isWindowShown) {
            Item(strings.trayHide, icon = Icons.Filled.CloseFullscreen, onClick = onHide)
        } else {
            Item(strings.trayOpen, icon = Icons.Filled.OpenInFull, onClick = onOpen)
        }

        if (menu.visible) {
            // Items for screens already on the back stack are hidden (see AppMenu.activeTargets).
            if (AppMenu.Target.Notifications !in menu.activeTargets) {
                Divider()
                Item(notificationsLabel, icon = Icons.Filled.Notifications) { onOpen(); menu.openNotifications() }
            }

            Divider()
            Item(strings.addEditDebtorTitleNew, icon = Icons.Filled.PersonAdd) { onOpen(); menu.addDebtor() }
            Item(strings.addEditCreditorTitleNew, icon = Icons.Filled.PersonAddAlt) { onOpen(); menu.addCreditor() }
            if (AppMenu.Target.Stats !in menu.activeTargets) {
                Item(strings.statsTitle, icon = Icons.Filled.QueryStats) { onOpen(); menu.openStats() }
            }
            if (AppMenu.Target.Settings !in menu.activeTargets) {
                Item(strings.settingsTitle, icon = Icons.Filled.Settings) { onOpen(); menu.openSettings() }
            }

            if (isAuthenticated) {
                Divider()
                Item(
                    label = strings.traySyncNow,
                    icon = Icons.Filled.Sync,
                    isEnabled = syncStatus != SyncUiStatus.Syncing,
                ) { scope.launch { runCatching { syncStatusProvider.refreshNow() } } }
                Item(syncLabel, isEnabled = false) // live status, not clickable
            }
        }

        Divider()
        CheckableItem(
            label = strings.settingsRunInBackground,
            checked = settings.runInBackground,
            onCheckedChange = { on ->
                settings.runInBackground = on
                if (!on) onOpen() // don't strand the app with no window and no tray
            },
        )

        Divider()
        Item(strings.trayQuit, icon = Icons.Filled.PowerSettingsNew) {
            dispose() // removes the tray icon before the process exits
            onQuit()
        }
    }
}

/**
 * Skia (Compose Desktop's renderer) decodes ICO/PNG directly but not macOS' .icns container,
 * тож на macOS іконка вікна не встановлюється тут — вона й так береться з бандла застосунку
 * при пакуванні (compose.desktop.application.nativeDistributions.macOS.iconFile у build.gradle.kts).
 *
 * Іконки лежать у src/main/resources/appIcons і читаються через classloader (а не File() з
 * відносним шляхом): відносний шлях залежить від робочої директорії процесу, яка в
 * запакованому (jpackage) застосунку інша, ніж під час `gradlew run` — через File() іконка
 * не знаходилась після встановлення.
 */
private fun windowIcon(): Painter? {
    val os = System.getProperty("os.name").lowercase()
    val resourcePath = when {
        os.contains("win") -> "appIcons/WindowsIcon.ico"
        os.contains("mac") -> "appIcons/MacosIcon.icns"
        else -> "appIcons/LinuxIcon.png"
    }
    return resourcePainter(resourcePath)
}

private fun resourcePainter(resourcePath: String): Painter? {
    val bytes = ClassLoader.getSystemResourceAsStream(resourcePath)?.use { it.readAllBytes() }
        ?: return null
    return BitmapPainter(bytes.decodeToImageBitmap())
}

