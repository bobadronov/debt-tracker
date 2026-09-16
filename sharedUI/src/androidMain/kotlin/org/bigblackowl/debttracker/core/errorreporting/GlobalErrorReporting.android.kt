package org.bigblackowl.debttracker.core.errorreporting

import io.github.aakira.napier.Napier

/** Chains onto Android's existing handler (crash dialog / process death) rather than replacing it. */
actual fun installGlobalErrorReporting() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        Napier.e(throwable = throwable, tag = "UncaughtException") { throwable.message ?: "Uncaught exception" }
        previous?.uncaughtException(thread, throwable)
    }
}
