package org.bigblackowl.debttracker.androidApp

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.memoryCacheMaxSizePercentWhileInBackground
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.koin.android.ext.koin.androidContext
import qrgenerator.AppContext

/**
 * Starts Koin and the background [SyncCoordinator]/[NotificationsPoller] once for the process, before any Activity or the widget runs.
 *
 * Implements [SingletonImageLoader.Factory] so Coil's process-wide [ImageLoader] is tuned for the
 * Play "memory optimization" quality bar (2026 app-quality standards): decoded bitmaps must not be
 * retained in non-visible app states, and the in-memory image cache is kept small for an app whose
 * images are only avatars/thumbnails.
 */
class DebtTrackerApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        AppContext.set(applicationContext) // required by QRKit's QR generator/scanner (core/qr)
        val koinApp = initKoin {
            androidContext(this@DebtTrackerApplication)
        }
        koinApp.koin.get<SyncCoordinator>().start()
        koinApp.koin.get<NotificationsPoller>().start()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            // Drop the entire in-memory bitmap cache the moment the UI stops being visible
            // (Coil rebuilds it on the next foreground). Directly satisfies the "bitmaps must
            // not be held in memory in background/cached states" requirement.
            .memoryCacheMaxSizePercentWhileInBackground(0.0)
            // Avatar/thumbnail-only workload — cap the foreground cache at 15% of app RAM
            // instead of Coil's 25% default to lower steady-state anonymous RSS.
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .build()
}
