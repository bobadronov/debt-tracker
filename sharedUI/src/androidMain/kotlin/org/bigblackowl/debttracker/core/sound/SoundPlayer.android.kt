package org.bigblackowl.debttracker.core.sound

import android.media.AudioAttributes
import android.media.SoundPool
import debt_tracker.sharedui.generated.resources.Res
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

actual fun createSoundPlayer(): SoundPlayer = AndroidSoundPlayer()

/** Android: [SoundPool], sized for short overlapping effects; sounds are preloaded from temp files copied out of compose.resources. */
private class AndroidSoundPlayer : SoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val soundIds = ConcurrentHashMap<SoundEffect, Int>()

    init {
        SoundEffect.entries.forEach { effect ->
            scope.launch {
                runCatching {
                    val bytes = Res.readBytes(effect.assetFileName)
                    val file = File.createTempFile(effect.name, ".wav").apply {
                        deleteOnExit()
                        writeBytes(bytes)
                    }
                    soundIds[effect] = soundPool.load(file.absolutePath, 1)
                }.onFailure { Napier.w(tag = "SoundPlayer") { "Failed to preload $effect: ${it.message}" } }
            }
        }
    }

    override fun play(sound: SoundEffect) {
        val id = soundIds[sound] ?: return
        soundPool.play(id, 1f, 1f, 0, 0, 1f)
    }
}
