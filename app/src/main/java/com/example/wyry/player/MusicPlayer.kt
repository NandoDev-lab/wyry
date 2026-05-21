package com.example.wyry.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow

import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

class MusicPlayer(context: Context) {
    private val pcmOutProcessor = PcmOutAudioProcessor()
    
    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            return DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(pcmOutProcessor))
                .build()
        }
    }

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .build()
    
    val currentSongTitle = MutableStateFlow<String?>(null)
    val isPlaying = MutableStateFlow(false)
    val pcmQueue = pcmOutProcessor.pcmQueue

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                currentSongTitle.value = mediaMetadata.title?.toString() ?: "Desconhecido"
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying.value = isPlayingNow
            }
        })
    }

    fun setPlaylist(uris: List<Uri>) {
        player.clearMediaItems()
        val mediaItems = uris.map { uri ->
            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(uri.lastPathSegment).build())
                .build()
        }
        player.setMediaItems(mediaItems)
        player.prepare()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun release() {
        player.release()
    }

    fun getPlayer(): ExoPlayer = player
}
