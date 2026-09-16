package org.bigblackowl.debttracker.domain.model

/** Mirrors `client_error_log.severity` (migration 0018) — only the two levels worth persisting remotely. */
enum class ErrorSeverity { WARN, ERROR }
