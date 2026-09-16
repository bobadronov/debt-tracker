package org.bigblackowl.debttracker.core.errorreporting

/**
 * Best-effort hook for uncaught (fatal) exceptions, funnelled through [io.github.aakira.napier.Napier.e]
 * so they reach [ErrorReportingAntilog] like any other logged error. Call once, right after
 * [org.bigblackowl.debttracker.core.di.initKoin] so the Antilog is already planted.
 *
 * "Best-effort": the report fires off asynchronously right as the process is about to die, so on
 * Android/Desktop the OS/JVM may finish tearing the process down before the network call completes
 * — this catches some crashes, not all. iOS/Web don't hook their native crash path yet here (Kotlin/
 * Native's uncaught-exception termination and browser error events need platform-specific interop
 * this pass didn't add); already-*caught* errors logged via `Napier.w`/`Napier.e` still get reported
 * on every platform regardless, since [ErrorReportingAntilog] doesn't depend on this at all.
 */
expect fun installGlobalErrorReporting()
