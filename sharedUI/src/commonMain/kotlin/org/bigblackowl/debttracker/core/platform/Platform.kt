package org.bigblackowl.debttracker.core.platform

/**
 * Determines the sign-in protection mechanism: mobile platforms — biometrics only (no PIN
 * fallback), Desktop — PIN only (no native biometrics), Web — no protection at all
 * (the mandatory email/password already serves as the entry barrier).
 */
enum class AppPlatform { ANDROID, IOS, DESKTOP, WEB }

expect val currentPlatform: AppPlatform
