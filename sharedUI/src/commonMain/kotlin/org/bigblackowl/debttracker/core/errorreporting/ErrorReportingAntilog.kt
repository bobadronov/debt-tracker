package org.bigblackowl.debttracker.core.errorreporting

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.domain.model.ErrorSeverity
import org.bigblackowl.debttracker.domain.repository.ErrorReportRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Forwards WARNING/ERROR/ASSERT [Napier] calls to [ErrorReportRepository] — planted unconditionally
 * in [org.bigblackowl.debttracker.core.di.initKoin] (unlike `DebugAntilog`, which stays debug-only:
 * that one exists for verbose local console output, this one writes a handful of already-meaningful
 * error calls to our own backend, in every build). Every existing `Napier.w`/`Napier.e` call site in
 * the codebase becomes a remote error report for free, including ones that only ever fire for a
 * signed-out/local-only user — see migration 0018 for why that's safe to allow.
 *
 * Throttled per (tag, message) pair rather than sent as-is: a stuck retry loop (e.g. offline sync)
 * logging the same warning every few seconds must not turn into a matching stream of RPC calls —
 * the server-side rate limit in `report_client_error` bounds the *damage*, this bounds the *noise*
 * before it ever leaves the device. Not a general crash reporter: this only sees what already goes
 * through Napier, so an uncaught fatal crash is only captured on platforms that also route their
 * uncaught-exception handler through `Napier.e` (see `DebtTrackerApplication`/desktop `main`).
 */
class ErrorReportingAntilog(
    private val repository: ErrorReportRepository,
    private val scope: CoroutineScope,
) : Antilog() {

    private val lastSentAt = mutableMapOf<String, Instant>()

    override fun isEnable(priority: LogLevel, tag: String?) =
        priority == LogLevel.WARNING || priority == LogLevel.ERROR || priority == LogLevel.ASSERT

    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        if (!isEnable(priority, tag)) return
        val text = message ?: throwable?.message ?: return
        val key = "$tag|$text"
        val now = Clock.System.now()
        // Not thread-safe by design — a rare lost/duplicate update to lastSentAt under a race is
        // harmless (worst case one extra report), and not worth a lock on a hot logging path.
        if (now - (lastSentAt[key] ?: Instant.DISTANT_PAST) < THROTTLE_WINDOW) return
        if (lastSentAt.size > MAX_TRACKED_KEYS) lastSentAt.clear()
        lastSentAt[key] = now

        val severity = if (priority == LogLevel.WARNING) ErrorSeverity.WARN else ErrorSeverity.ERROR
        scope.launch {
            repository.report(
                severity = severity,
                tag = tag,
                message = text,
                stackTrace = throwable?.stackTraceToString(),
            )
        }
    }

    private companion object {
        val THROTTLE_WINDOW = 60.seconds
        const val MAX_TRACKED_KEYS = 500
    }
}
