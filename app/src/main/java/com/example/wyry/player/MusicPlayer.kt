package com.example.wyry.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MusicPlayer(context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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
    val currentPosition = MutableStateFlow(0L)
    val duration = MutableStateFlow(0L)
    val pcmQueue = pcmOutProcessor.pcmQueue
    val vuMeter = pcmOutProcessor.vuMeter

    init {
        scope.launch {
            while (true) {
                if (player.isPlaying) {
                    currentPosition.value = player.currentPosition
                    duration.value = player.duration
                }
                delay(1000)
            }
        }

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
        
        if (uris.size > 1) {
            player.repeatMode = Player.REPEAT_MODE_OFF
        }

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
