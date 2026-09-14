package org.bigblackowl.debttracker.core.notifications

/**
 * Shows a real OS-level system notification (Android `NotificationManager`, iOS
 * `UNUserNotificationCenter`, Desktop System Tray, Web `Notification` API) — following the pattern
 * of [org.bigblackowl.debttracker.core.sound.SoundPlayer]: a plain interface (not `expect class`)
 * so [org.bigblackowl.debttracker.preview.previewModule] can bind a no-op implementation instead
 * of the real one, and Compose Preview never touches platform notification APIs.
 */
interface LocalNotifier {
    /** Requests notification permission where the platform requires it (Android 13+, iOS, Web); no-op/true where no permission is needed (Desktop). */
    suspend fun requestPermission(): Boolean

    /**
     * Shows a notification. [deepLink] (see [NotificationDeepLinks.linkFor]) — where to navigate on
     * tap: the platform attaches it to the notification and returns it to [NotificationDeepLinks]
     * on click. `null` means a notification with no navigation (tapping only brings the app to front).
     */
    fun notify(title: String, body: String, deepLink: String? = null)
}

// No `expect fun createLocalNotifier()` (unlike SoundPlayer) — the Android implementation needs a
// Context, so each platform binds its own [LocalNotifier] directly in `platformDataModule()` (the
// same approach as Room/SyncCoordinator), rather than through a parameterized expect function in
// the shared AppModule.
//
// The no-op implementation for Compose Preview lives in preview/PreviewFakes.kt
// (NoOpLocalNotifier), alongside the rest of the preview module's fake dependencies.
