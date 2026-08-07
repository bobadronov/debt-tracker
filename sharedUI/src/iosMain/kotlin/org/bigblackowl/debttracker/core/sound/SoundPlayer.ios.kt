@file:OptIn(ExperimentalForeignApi::class)

package org.bigblackowl.debttracker.core.sound

import debt_tracker.sharedui.generated.resources.Res
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create

actual fun createSoundPlayer(): SoundPlayer = IosSoundPlayer()

/**
 * iOS: [AVAudioPlayer] per [SoundEffect], preloaded from compose.resources WAV assets written to a
 * temp file (`AVAudioPlayer` needs a real file URL). Runs on [Dispatchers.Main] so the player map
 * is only ever touched from one thread — Kotlin/Native has no built-in concurrent map.
 */
private class IosSoundPlayer : SoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val players = mutableMapOf<SoundEffect, AVAudioPlayer>()

    init {
        SoundEffect.entries.forEach { effect ->
            scope.launch {
                runCatching {
                    val bytes = Res.readBytes(effect.assetFileName)
                    val path = NSTemporaryDirectory() + "${effect.name.lowercase()}.wav"
                    NSFileManager.defaultManager.createFileAtPath(path, bytes.toNSData(), null)
                    val player = AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(path), error = null)
                    player.prepareToPlay()
                    players[effect] = player
                }.onFailure { Napier.w(tag = "SoundPlayer") { "Failed to preload $effect: ${it.message}" } }
            }
        }
    }

    override fun play(sound: SoundEffect) {
        val player = players[sound] ?: return
        player.currentTime = 0.0
        player.play()
    }
}

private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}
