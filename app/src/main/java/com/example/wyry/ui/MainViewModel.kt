package com.example.wyry.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wyry.audio.AudioProcessor
import com.example.wyry.data.StreamConfig
import com.example.wyry.player.MusicPlayer
import com.example.wyry.streaming.StreamManager
import com.example.wyry.streaming.StreamingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioProcessor = AudioProcessor()
    private val musicPlayer = MusicPlayer(application)
    private val streamManager = StreamManager(application)

    private val _streamConfig = MutableStateFlow(StreamConfig())
    val streamConfig: StateFlow<StreamConfig> = _streamConfig

    val isStreaming = streamManager.isStreaming
    val status = streamManager.status
    val vuMeter = audioProcessor.vuMeter

    private val _micVolume = MutableStateFlow(1f)
    val micVolume: StateFlow<Float> = _micVolume

    private val _musicVolume = MutableStateFlow(0.5f)
    val musicVolume: StateFlow<Float> = _musicVolume

    fun updateConfig(config: StreamConfig) {
        _streamConfig.value = config
    }

    fun toggleStream() {
        if (isStreaming.value) {
            stopStreaming()
        } else {
            startStreaming()
        }
    }

    private fun startStreaming() {
        val context = getApplication<Application>()
        val intent = Intent(context, StreamingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        viewModelScope.launch(Dispatchers.IO) {
            streamManager.startStream(_streamConfig.value)
            audioProcessor.startCapture()
            
            val buffer = ShortArray(1024)
            var currentMusicVol = _musicVolume.value
            while (isStreaming.value) {
                val read = audioProcessor.read(buffer)
                if (read > 0) {
                    // Simple Ducking: reduce music volume if mic input is significant
                    val targetVol = if (vuMeter.value > 0.1f) _musicVolume.value * 0.3f else _musicVolume.value
                    if (targetVol != currentMusicVol) {
                        currentMusicVol = targetVol
                        withContext(Dispatchers.Main) {
                            musicPlayer.setVolume(targetVol)
                        }
                    }

                    streamManager.writeAudio(buffer, read)
                }
            }
        }
    }

    private fun stopStreaming() {
        streamManager.stopStream()
        audioProcessor.stopCapture()
        val context = getApplication<Application>()
        context.stopService(Intent(context, StreamingService::class.java))
    }

    fun playMusic(uri: Uri) {
        viewModelScope.launch {
            musicPlayer.play(uri)
            musicPlayer.setVolume(_musicVolume.value)
        }
    }

    fun setMicVolume(volume: Float) {
        _micVolume.value = volume
    }

    fun setMusicVolume(volume: Float) {
        _musicVolume.value = volume
        viewModelScope.launch {
            musicPlayer.setVolume(volume)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
        audioProcessor.stopCapture()
        streamManager.stopStream()
    }
}
