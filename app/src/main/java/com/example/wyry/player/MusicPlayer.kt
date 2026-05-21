package com.example.wyry.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MusicPlayer(context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun play(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun release() {
        player.release()
    }

    fun getPlayer(): ExoPlayer = player
}
