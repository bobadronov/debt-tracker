package org.bigblackowl.debttracker.core.di

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.errorreporting.ErrorReportingAntilog
import org.bigblackowl.debttracker.domain.repository.ErrorReportRepository
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import kotlinx.coroutines.CoroutineScope

/** Called from each platform's entry point before rendering [org.bigblackowl.debttracker.App]. */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    // Only planted in debug builds (see BuildConfig.DEBUG) — Napier calls are no-ops otherwise,
    // so release builds never pay for or leak verbose logging.
    if (BuildConfig.DEBUG) Napier.base(DebugAntilog())
    val koinApp = startKoin {
        config?.invoke(this)
        modules(appModule, platformDataModule())
    }
    // Unlike DebugAntilog above, planted in every build — this is our own error log
    // (client_error_log), not verbose console output. Needs a live Koin instance to resolve its
    // dependencies, so it's planted after startKoin rather than alongside DebugAntilog.
    with(koinApp.koin) { Napier.base(ErrorReportingAntilog(get<ErrorReportRepository>(), get<CoroutineScope>())) }
    return koinApp
}
