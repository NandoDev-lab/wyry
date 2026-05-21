package com.example.wyry.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wyry.audio.AudioProcessor
import com.example.wyry.data.SettingsRepository
import com.example.wyry.data.StreamConfig
import com.example.wyry.player.MusicPlayer
import com.example.wyry.streaming.StreamManager
import com.example.wyry.streaming.StreamingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioProcessor = AudioProcessor()
    private val musicPlayer = MusicPlayer(application)
    private val streamManager = StreamManager(application)
    private val repository = SettingsRepository(application)

    private val _streamConfig = MutableStateFlow(StreamConfig())
    val streamConfig: StateFlow<StreamConfig> = _streamConfig.asStateFlow()

    init {
        viewModelScope.launch {
            repository.streamConfigFlow.collectLatest { config ->
                _streamConfig.value = config
            }
        }
    }

    val isStreaming = streamManager.isStreaming
    val status = streamManager.status
    val vuMeter = audioProcessor.vuMeter

    val currentSongTitle = musicPlayer.currentSongTitle
    val isMusicPlaying = musicPlayer.isPlaying

    private val _micVolume = MutableStateFlow(1f)
    val micVolume: StateFlow<Float> = _micVolume

    private val _micEnabled = MutableStateFlow(false)
    val micEnabled: StateFlow<Boolean> = _micEnabled

    private val _musicVolume = MutableStateFlow(0.5f)
    val musicVolume: StateFlow<Float> = _musicVolume

    fun updateConfig(config: StreamConfig) {
        _streamConfig.value = config
        viewModelScope.launch {
            repository.saveConfig(config)
        }
    }

    fun toggleStream() {
        if (isStreaming.value) {
            stopStreaming()
        } else {
            startStreaming()
        }
    }

    fun toggleMic() {
        val newState = !_micEnabled.value
        _micEnabled.value = newState
        if (newState) {
            audioProcessor.startCapture()
        } else if (!isStreaming.value) {
            audioProcessor.stopCapture()
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

        // Always start capture to provide a hardware-based timing clock
        audioProcessor.startCapture()

        viewModelScope.launch(Dispatchers.IO) {
            streamManager.startStream(_streamConfig.value)
            
            val micBuffer = ShortArray(1024)
            val musicStore = ShortArray(8192)
            var musicStoreHead = 0
            var musicStoreTail = 0

            while (isStreaming.value) {
                // AudioRecord.read will block the loop and maintain 44.1kHz cadence
                val read = audioProcessor.read(micBuffer)
                
                if (read <= 0) {
                    kotlinx.coroutines.delay(20)
                    if (read < 0) continue
                }

                val actualRead = if (read > 0) read else 1024
                val mixedBuffer = ShortArray(actualRead)
                val micVol = if (_micEnabled.value) _micVolume.value else 0f
                val musVol = _musicVolume.value

                while ((musicStoreTail - musicStoreHead + 8192) % 8192 < actualRead) {
                    val chunk = musicPlayer.pcmQueue.poll()
                    if (chunk != null) {
                        for (sample in chunk) {
                            musicStore[musicStoreTail] = sample
                            musicStoreTail = (musicStoreTail + 1) % 8192
                        }
                    } else {
                        musicStore[musicStoreTail] = 0
                        musicStoreTail = (musicStoreTail + 1) % 8192
                    }
                }

                for (i in 0 until actualRead) {
                    val micSample = (if (read > 0) micBuffer[i] else 0) * micVol
                    val musSample = musicStore[musicStoreHead] * musVol
                    musicStoreHead = (musicStoreHead + 1) % 8192
                    
                    mixedBuffer[i] = (micSample + musSample).coerceIn(-32768f, 32767f).toInt().toShort()
                }

                streamManager.writeAudio(mixedBuffer, actualRead)
            }
        }
    }

    private fun stopStreaming() {
        streamManager.stopStream()
        if (!_micEnabled.value) {
            audioProcessor.stopCapture()
        }
        val context = getApplication<Application>()
        context.stopService(Intent(context, StreamingService::class.java))
    }

    fun playMusic(uris: List<Uri>) {
        musicPlayer.setPlaylist(uris)
        musicPlayer.play()
    }

    fun toggleMusic() {
        if (isMusicPlaying.value) {
            musicPlayer.pause()
        } else {
            musicPlayer.play()
        }
    }

    fun nextSong() {
        musicPlayer.next()
    }

    fun previousSong() {
        musicPlayer.previous()
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
