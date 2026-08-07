package org.bigblackowl.debttracker.androidApp

import android.app.Application
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.koin.android.ext.koin.androidContext

/** Starts Koin and the background [SyncCoordinator] once for the process, before any Activity or the widget runs. */
class DebtTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApp = initKoin {
            androidContext(this@DebtTrackerApplication)
        }
        koinApp.koin.get<SyncCoordinator>().start()
    }
}
