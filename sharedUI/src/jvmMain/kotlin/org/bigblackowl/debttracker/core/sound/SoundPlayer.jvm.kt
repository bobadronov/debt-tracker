package org.bigblackowl.debttracker.core.sound

import debt_tracker.sharedui.generated.resources.Res
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

actual fun createSoundPlayer(): SoundPlayer = DesktopSoundPlayer()

/** Desktop: [Clip] per [SoundEffect], preloaded from compose.resources WAV assets and rewound on every [play]. */
private class DesktopSoundPlayer : SoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clips = ConcurrentHashMap<SoundEffect, Clip>()

    init {
        SoundEffect.entries.forEach { effect ->
            scope.launch {
                runCatching {
                    val bytes = Res.readBytes(effect.assetFileName)
                    AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes)).use { audioStream ->
                        val clip = AudioSystem.getClip()
                        clip.open(audioStream)
                        clips[effect] = clip
                    }
                }.onFailure { Napier.w(tag = "SoundPlayer") { "Failed to preload $effect: ${it.message}" } }
            }
        }
    }

    override fun play(sound: SoundEffect) {
        val clip = clips[sound] ?: return
        clip.stop()
        clip.framePosition = 0
        clip.start()
    }
}
