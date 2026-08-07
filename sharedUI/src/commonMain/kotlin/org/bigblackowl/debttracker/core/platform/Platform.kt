package org.bigblackowl.debttracker.core.platform

/**
 * Визначає механізм захисту входу: мобільні платформи — лише біометрія (без PIN-фолбеку),
 * Desktop — лише PIN (немає нативної біометрії), Web — захисту немає взагалі
 * (обов'язковий email/пароль вже слугує вхідним бар'єром).
 */
enum class AppPlatform { ANDROID, IOS, DESKTOP, WEB }

expect val currentPlatform: AppPlatform
