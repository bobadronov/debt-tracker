import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.core.errorreporting.installGlobalErrorReporting
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
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.rememberAppColorScheme
import org.bigblackowl.debttracker.ui.components.appbar.AppOverflowMenu
import org.bigblackowl.debttracker.ui.components.appbar.DesktopTitleBar
import org.bigblackowl.debttracker.ui.components.button.IconButton
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.core.Koin

private const val APP_NAME = "Debt Tracker"
private const val APP_ID = "org.bigblackowl.debttracker"

/** Desktop (JVM) entry point: starts Koin + background sync/notifications, then opens the app window. */
fun main() {
    DesktopTitleBar.claim() // screens route their TopAppBar into the in-window title bar from here on
    FileKit.init(appId = APP_ID) // required for FileKit's Save-As dialogs and cache/files dirs on JVM
    val koinApp = initKoin()
    installGlobalErrorReporting()
    koinApp.koin.get<SyncCoordinator>().start()
    koinApp.koin.get<NotificationsPoller>().start()
    koinApp.koin.get<org.bigblackowl.debttracker.core.notifications.DueReminderCoordinator>().start()
    startApp(koinApp.koin)
}

/**
 * Standard Compose Desktop window. The screens' title / back button / action icons are drawn in an
 * in-window Material bar (via [DesktopTitleBar]); the OS frame only carries the window title.
 *
 * [AppSettings.runInBackground] (Settings → "Run in background", desktop-only) changes what the
 * window's close button does: on, it hides the window behind a tray icon instead of exiting, so
 * background sync keeps running; off, it behaves like a normal window and exits the process.
 */
private fun startApp(koin: Koin) = application {
    val settings = remember { koin.get<AppSettings>() }
    val uiScope = rememberCoroutineScope()

    var isWindowVisible by remember { mutableStateOf(true) }
    val windowState = rememberWindowState(width = 500.dp, height = 700.dp)
    // Set from inside Window's content — lets a tray click / notification tap raise the window.
    val awtWindow = remember { mutableStateOf<java.awt.Window?>(null) }

    // The tray "show / hide" item must flip on BOTH states, not just `isWindowVisible`: a window the
    // user shrank with the native minimize button is still `visible`, but the item should then read
    // "restore", not "hide to tray".
    val isWindowShown = isWindowVisible && !windowState.isMinimized

    // `uiScope.launch` marshals the (possibly off-thread) call onto the composition dispatcher.
    val surfaceWindow: () -> Unit = {
        uiScope.launch {
            isWindowVisible = true
            windowState.isMinimized = false
            awtWindow.value?.apply {
                toFront()
                requestFocus()
            }
        }
    }

    // Tapping a system notification's banner (core/notifications) un-hides the window when
    // "Run in background" had tucked it away behind the tray.
    DisposableEffect(Unit) {
        DesktopNotificationWindow.bringToFront = surfaceWindow
        onDispose { DesktopNotificationWindow.bringToFront = null }
    }

    val appIcon = remember { windowIcon() }

    val strings = remember(settings.locale) { resolveStrings(settings.locale) }

    // The visible screen routes its title / back button / action icons into the title bar
    // (via DesktopTitleBar); CurrentScreen.windowTitle is the fallback for screens that don't.
    val currentScreen by CurrentScreen.screen.collectAsState()
    val titleBar by DesktopTitleBar.content.collectAsState()
    val windowTitle = titleBar.title ?: currentScreen?.windowTitle(strings) ?: APP_NAME

    // Fully reactive — the menu re-reads the unread count / sync status / locale / window state
    // with no manual teardown.
    if (appIcon != null) {
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

    Window(
        title = windowTitle, // OS window title (taskbar / alt-tab)
        icon = appIcon,
        state = windowState,
        visible = isWindowVisible,
        onCloseRequest = {
            if (settings.runInBackground) isWindowVisible = false else exitApplication()
        },
    ) {
        LaunchedEffect(window) {
            window.minimumSize = java.awt.Dimension(500, 700)
            awtWindow.value = window
        }

        // Wrapped in the app's own color scheme so the title bar matches the in-app theme
        // (light/dark per Settings).
        MaterialTheme(colorScheme = rememberAppColorScheme()) {
            Column(Modifier.fillMaxSize()) {
                Surface(color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
                    Box(Modifier.fillMaxWidth().height(40.dp)) {
                        titleBar.back?.let { onBack ->
                            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(Dimens.Spacing.xs)) {
                                BackChevron()
                            }
                        }
                        Text(
                            windowTitle,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            titleBar.actions?.invoke(this)
                            AppOverflowMenu(strings = strings) // renders nothing on the lock / onboarding screens
                        }
                    }
                }
                Box(Modifier.weight(1f)) { App() }
            }
        }
    }
}

/** Left-pointing chevron drawn to the current content color — the title-bar back affordance. */
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
 * System-tray icon + a fully reactive "control center" menu. Left-click opens the window; the menu
 * mirrors the app menu (Notifications with unread count, quick-add, Stats, Settings), plus a
 * "sync now" action with live status and a "run in background" toggle. Everything past the first
 * two items is gated on [AppMenu] being visible, i.e. the user is past the lock / onboarding
 * screens. Split out so `dispose()` (a member of the tray menu DSL scope) reads cleanly.
 */
@Composable
private fun ApplicationScope.TrayMenu(
    appIcon: Painter,
    strings: Strings,
    /** Window is on screen and not minimized — drives whether the first item hides or restores it. */
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
        SyncUiStatus.Synced -> strings.home.syncSynced
        SyncUiStatus.Syncing -> strings.home.syncSyncing
        is SyncUiStatus.OfflinePending -> strings.home.syncOfflinePending(s.count)
    }
    val notificationsLabel =
        if (isAuthenticated && unread > 0) "${strings.notifications.title} ($unread)" else strings.notifications.title

    Tray(
        icon = appIcon,
        tooltip = buildString {
            append(APP_NAME)
            if (isAuthenticated) append(" · ").append(if (unread > 0) notificationsLabel else syncLabel)
        },
        // Left-click / double-click toggles: hide a shown window, restore a hidden/minimized one.
        onAction = { if (isWindowShown) onHide() else onOpen() },
    ) {
        if (isWindowShown) {
            Item(strings.settings.trayHide, onClick = onHide)
        } else {
            Item(strings.settings.trayOpen, onClick = onOpen)
        }

        if (menu.visible) {
            // Items for screens already on the back stack are hidden (see AppMenu.activeTargets).
            if (AppMenu.Target.Notifications !in menu.activeTargets) {
                Separator()
                Item(notificationsLabel) { onOpen(); menu.openNotifications() }
            }

            Separator()
            Item(strings.addEditDebtor.titleNew) { onOpen(); menu.addDebtor() }
            Item(strings.addEditCreditor.titleNew) { onOpen(); menu.addCreditor() }
            if (AppMenu.Target.Stats !in menu.activeTargets) {
                Item(strings.stats.title) { onOpen(); menu.openStats() }
            }
            if (AppMenu.Target.Settings !in menu.activeTargets) {
                Item(strings.settings.title) { onOpen(); menu.openSettings() }
            }

            if (isAuthenticated) {
                Separator()
                Item(
                    text = strings.settings.traySyncNow,
                    enabled = syncStatus != SyncUiStatus.Syncing,
                ) { scope.launch { runCatching { syncStatusProvider.refreshNow() } } }
                Item(syncLabel, enabled = false, onClick = {}) // live status, not clickable
            }
        }

        Separator()
        CheckboxItem(
            text = strings.settings.runInBackground,
            checked = settings.runInBackground,
            onCheckedChange = { on ->
                settings.runInBackground = on
                if (!on) onOpen() // don't strand the app with no window and no tray
            },
        )

        Separator()
        Item(strings.settings.trayQuit, onClick = onQuit)
    }
}

/**
 * Skia (Compose Desktop's renderer) decodes ICO/PNG directly but not macOS' .icns container,
 * so the window icon isn't set here on macOS — it's taken from the app bundle at packaging time
 * instead (compose.desktop.application.nativeDistributions.macOS.iconFile in build.gradle.kts).
 *
 * Icons live in src/main/resources/appIcons and are read via the classloader (not File() with a
 * relative path): a relative path depends on the process' working directory, which differs
 * between a packaged (jpackage) app and `gradlew run` — File() couldn't find the icon after
 * installation.
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

