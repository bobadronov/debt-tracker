package org.bigblackowl.debttracker.core.notifications

/**
 * Показує справжнє системне сповіщення ОС (Android `NotificationManager`, iOS
 * `UNUserNotificationCenter`, Desktop System Tray, Web `Notification` API) — за зразком
 * [org.bigblackowl.debttracker.core.sound.SoundPlayer]: plain-інтерфейс (не `expect class`), щоб
 * [org.bigblackowl.debttracker.preview.previewModule] міг підв'язати no-op реалізацію замість
 * справжньої, і Compose Preview ніколи не торкався платформних API сповіщень.
 */
interface LocalNotifier {
    /** Запитує дозвіл на сповіщення, якщо платформа цього вимагає (Android 13+, iOS, Web); no-op/true там, де дозвіл не потрібен (Desktop). */
    suspend fun requestPermission(): Boolean
    fun notify(title: String, body: String)
}

// Немає `expect fun createLocalNotifier()` (на відміну від SoundPlayer) — Android-реалізація
// потребує Context, тож кожна платформа біндить свій [LocalNotifier] напряму в
// `platformDataModule()` (той самий підхід, що й Room/SyncCoordinator), а не через
// параметризовану expect-функцію в спільному AppModule.
//
// No-op реалізація для Compose Preview живе в preview/PreviewFakes.kt
// (NoOpLocalNotifier), поруч з рештою фейкових залежностей preview-модуля.
