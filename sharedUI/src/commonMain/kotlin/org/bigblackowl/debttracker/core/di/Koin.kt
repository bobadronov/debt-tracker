package org.bigblackowl.debttracker.core.di

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.bigblackowl.debttracker.BuildConfig
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Викликається з кожної платформної точки входу перед рендером [org.bigblackowl.debttracker.App]. */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    // Only planted in debug builds (see BuildConfig.DEBUG) — Napier calls are no-ops otherwise,
    // so release builds never pay for or leak verbose logging.
    if (BuildConfig.DEBUG) Napier.base(DebugAntilog())
    return startKoin {
        config?.invoke(this)
        modules(appModule, platformDataModule())
    }
}
