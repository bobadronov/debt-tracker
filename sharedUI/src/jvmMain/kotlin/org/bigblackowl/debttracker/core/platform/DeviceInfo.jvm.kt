package org.bigblackowl.debttracker.core.platform

import java.net.InetAddress

/**
 * `COMPUTERNAME`/`HOSTNAME` are set by the OS with no I/O — checked first so the common case never
 * pays for [InetAddress.getLocalHost]'s reverse-DNS lookup, which can block for seconds on a slow
 * or misconfigured resolver (this runs on whatever dispatcher collects [org.bigblackowl.debttracker.domain.repository.SessionRepository.revokedElsewhere]).
 */
actual fun deviceDisplayName(): String =
    System.getenv("COMPUTERNAME")?.takeIf { it.isNotBlank() }
        ?: System.getenv("HOSTNAME")?.takeIf { it.isNotBlank() }
        ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: System.getProperty("os.name")
        ?: "Desktop"
