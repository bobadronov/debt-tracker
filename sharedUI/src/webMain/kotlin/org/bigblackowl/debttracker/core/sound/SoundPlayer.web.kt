package org.bigblackowl.debttracker.core.sound

import debt_tracker.sharedui.generated.resources.Res
import org.w3c.dom.Audio

actual fun createSoundPlayer(): SoundPlayer = WebSoundPlayer()

/** Web (JS/Wasm): HTML5 `Audio`, one element per [SoundEffect], loaded straight from its compose.resources URL. */
private class WebSoundPlayer : SoundPlayer {
    private val audioElements: Map<SoundEffect, Audio> = SoundEffect.entries.associateWith { effect ->
        Audio(Res.getUri(effect.assetFileName))
    }

    override fun play(sound: SoundEffect) {
        val audio = audioElements[sound] ?: return
        audio.currentTime = 0.0
        audio.play()
    }
}
