package org.bigblackowl.debttracker.domain.model

import kotlin.time.Instant
import org.bigblackowl.debttracker.core.platform.AppPlatform

/** One row of `user_sessions` — a device currently signed into the account (спек: Session management). */
data class DeviceSession(
    val id: String,
    val deviceName: String,
    val platform: AppPlatform,
    val lastSeenAt: Instant,
    val isCurrentDevice: Boolean,
)
