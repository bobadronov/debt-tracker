package org.bigblackowl.debttracker.core.platform

/** Human-readable label for the current device, used to tell devices apart on the "Active devices" screen. */
expect fun deviceDisplayName(): String

/**
 * Short OS/hardware summary (OS name+version, device model, CPU arch — whatever's cheap and
 * available per platform), attached to every row in `client_error_log` (migration 0019) alongside
 * `app_version`/`platform` — e.g. "Android 14 (SDK 34); Pixel 7". No user-identifying info: this
 * goes out for signed-out users too (see [org.bigblackowl.debttracker.domain.repository.ErrorReportRepository]).
 */
expect fun systemInfo(): String
