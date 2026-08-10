package org.bigblackowl.debttracker.core.sound

/** ADD/DELETE/DIALOG_OPEN — короткі звукові ефекти (спек §7). */
enum class SoundEffect { ADD, DELETE, DIALOG_OPEN }

/** Path (relative to composeResources) of the generated short WAV asset for this effect — see `files/sounds/README.txt`. */
internal val SoundEffect.assetFileName: String
    get() = "files/sounds/${name.lowercase()}.wav"

/**
 * Short (<300ms) UI feedback sounds. Each platform's implementation (created by [createSoundPlayer])
 * preloads the WAV assets from `composeResources/files/sounds/` in the background (via
 * compose.resources) and [play] fires a pre-decoded, ready-to-play sound — no I/O happens on the
 * [play] call itself.
 *
 * A plain interface (not `expect class`) on purpose: it lets [org.bigblackowl.debttracker.preview.previewModule]
 * bind a no-op implementation instead of the real one, so that merely resolving a [SoundPlayer]
 * dependency in a Compose Preview never references a platform media API (e.g. Android's
 * `SoundPool`, which doesn't exist in Android Studio's Layoutlib preview sandbox).
 */
interface SoundPlayer {
    fun play(sound: SoundEffect)
}

/** Creates the platform's real [SoundPlayer] (Android `SoundPool`, iOS `AVAudioPlayer`, desktop `Clip`, web `Audio`). */
expect fun createSoundPlayer(): SoundPlayer

/** No-op [SoundPlayer] bound app-wide when [org.bigblackowl.debttracker.BuildConfig.SOUND_ENABLED] is off. */
internal object NoopSoundPlayer : SoundPlayer {
    override fun play(sound: SoundEffect) = Unit
}
