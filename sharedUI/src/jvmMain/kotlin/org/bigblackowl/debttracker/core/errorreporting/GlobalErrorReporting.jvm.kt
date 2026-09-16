package org.bigblackowl.debttracker.core.errorreporting

import io.github.aakira.napier.Napier

/** Chains onto the JVM's existing handler (default: print to stderr) rather than replacing it. */
actual fun installGlobalErrorReporting() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        Napier.e(throwable = throwable, tag = "UncaughtException") { throwable.message ?: "Uncaught exception" }
        previous?.uncaughtException(thread, throwable)
    }
}
