package org.bigblackowl.debttracker.core.notifications

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** [DesktopLocalNotifier] hands every event to Nucleus `notification-common`; there is no fallback. */
class DesktopLocalNotifierTest {

    @Test
    fun requestPermissionIsAlwaysGranted() = runTest {
        assertTrue(DesktopLocalNotifier().requestPermission(), "desktop needs no notification permission")
    }

    @Test
    fun notifyNeverThrowsWhenNativeBackendIsUnavailable() {
        // Regression: NotificationManager.isAvailable() / send() throw UnsatisfiedLinkError when a
        // backend's native lib can't load (headless CI, arch mismatch). notify() must swallow it.
        val notifier = DesktopLocalNotifier()
        notifier.notify("Новий борг", "Олена позичила вам 500 ₴", deepLink = "debttracker://notification?id=1")
        notifier.notify("Sync", "Balance updated", deepLink = null)
    }
}
