package org.bigblackowl.debttracker.domain.model

import org.bigblackowl.debttracker.core.platform.AppPlatform
import kotlin.time.Instant

/** One row of `user_sessions` — a device currently signed into the account (spec: Session management). */
data class DeviceSession(
    val id: String,
    val deviceName: String,
    val platform: AppPlatform,
    val lastSeenAt: Instant,
    val isCurrentDevice: Boolean,
)
