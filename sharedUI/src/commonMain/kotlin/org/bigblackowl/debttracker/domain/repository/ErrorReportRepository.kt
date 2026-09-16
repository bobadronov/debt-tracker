package org.bigblackowl.debttracker.domain.repository

import org.bigblackowl.debttracker.domain.model.ErrorSeverity

/**
 * The app's own client-side error log (`public.client_error_log`, migration 0018) — a fire-and-
 * forget append, not a request/response call. Works while signed out too: the row is just
 * attributed to no account (see migration 0018 for how that's kept safe for both the DB and the
 * user — server-side rate limiting/length caps, and RLS that gives nobody, including the reporter,
 * any read access back).
 *
 * Implementations must never throw: a broken error reporter must not itself become the thing that
 * crashes the caller, or mask whatever error it was trying to report in the first place.
 */
interface ErrorReportRepository {
    suspend fun report(severity: ErrorSeverity, tag: String?, message: String, stackTrace: String?)
}
