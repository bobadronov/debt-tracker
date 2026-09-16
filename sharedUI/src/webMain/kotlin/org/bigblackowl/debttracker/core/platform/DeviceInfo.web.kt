package org.bigblackowl.debttracker.core.platform

import kotlinx.browser.window

// No local cache/session persistence on Web (see AppPlatform.WEB docs) — every sign-in is
// effectively a fresh browser tab, so a generic label is enough to tell it apart in the list.
actual fun deviceDisplayName(): String = "Web browser"

actual fun systemInfo(): String = runCatching { window.navigator.userAgent }.getOrDefault("Web browser")
