package org.bigblackowl.debttracker.core.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Викликається з кожної платформної точки входу перед рендером [org.bigblackowl.debttracker.App]. */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication = startKoin {
    config?.invoke(this)
    modules(appModule, platformDataModule())
}
