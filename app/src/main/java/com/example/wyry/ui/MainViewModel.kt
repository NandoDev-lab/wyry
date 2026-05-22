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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioProcessor = AudioProcessor()
    private val musicPlayer = MusicPlayer(application)
    private val vignettePlayer = MusicPlayer(application)
    private val streamManager = StreamManager(application)
    private val repository = SettingsRepository(application)

    private val _streamConfig = MutableStateFlow(StreamConfig())
    val streamConfig: StateFlow<StreamConfig> = _streamConfig.asStateFlow()

    private val _playlist = MutableStateFlow<List<Uri>>(emptyList())
    val playlist: StateFlow<List<Uri>> = _playlist.asStateFlow()

    private val _vignettes = MutableStateFlow<List<Uri?>>(List(5) { null })
    val vignettes: StateFlow<List<Uri?>> = _vignettes.asStateFlow()

    val isStreaming = streamManager.isStreaming
    val status = streamManager.status
    val micVuMeter = audioProcessor.vuMeter
    val musicVuMeter = musicPlayer.vuMeter

    val currentSongTitle = musicPlayer.currentSongTitle
    val isMusicPlaying = musicPlayer.isPlaying
    val musicPosition = musicPlayer.currentPosition
    val musicDuration = musicPlayer.duration
    
    private val _isVignettePlaying = MutableStateFlow(false)
    val isVignettePlaying: StateFlow<Boolean> = _isVignettePlaying.asStateFlow()

    private val _micVolume = MutableStateFlow(1f)
    val micVolume: StateFlow<Float> = _micVolume

    private val _micEnabled = MutableStateFlow(false)
    val micEnabled: StateFlow<Boolean> = _micEnabled

    private val _musicVolume = MutableStateFlow(0.5f)
    val musicVolume: StateFlow<Float> = _musicVolume

    private val _reconnectCountdown = MutableStateFlow(0)
    val reconnectCountdown: StateFlow<Int> = _reconnectCountdown.asStateFlow()

    private var micLoopJob: Job? = null
    private val micSharedBuffer = ShortArray(1024)
    private var lastMicReadSize = 0

    private var isReconnecting = false
    private val MAX_RETRY = 3
    private var retryCount = 0

    init {
        viewModelScope.launch {
            repository.streamConfigFlow.collectLatest { config ->
                _streamConfig.value = config
            }
        }
        
        // Monitora se a vinheta terminou de tocar
        viewModelScope.launch {
            vignettePlayer.isPlaying.collectLatest { playing ->
                _isVignettePlaying.value = playing
            }
        }
    }

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

    fun simulateError() {
        if (isStreaming.value) {
            streamManager.isStreaming.value = false
            streamManager.status.value = "ERRO DE CONEXÃO"
        }
    }

    fun toggleMic() {
        val newState = !_micEnabled.value
        _micEnabled.value = newState
        if (newState) {
            audioProcessor.startCapture()
            startMicLoop()
        } else {
            if (!isStreaming.value) {
                micLoopJob?.cancel()
                audioProcessor.stopCapture()
            }
        }
    }

    private fun startMicLoop() {
        if (micLoopJob?.isActive == true) return
        micLoopJob = viewModelScope.launch(Dispatchers.IO) {
            while (_micEnabled.value || isStreaming.value) {
                lastMicReadSize = audioProcessor.read(micSharedBuffer)
                if (lastMicReadSize <= 0) {
                    kotlinx.coroutines.delay(20)
                }
            }
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

        audioProcessor.startCapture()
        startMicLoop()

        viewModelScope.launch(Dispatchers.IO) {
            streamManager.startStream(_streamConfig.value)
            
            val musicStore = ShortArray(8192)
            var musicStoreHead = 0
            var musicStoreTail = 0

            val jingleStore = ShortArray(8192)
            var jingleStoreHead = 0
            var jingleStoreTail = 0
            
            var currentDuckFactor = 1.0f
            val duckThreshold = 0.05f
            val duckVolume = 0.2f
            val fadeSpeed = 0.05f

            while (isStreaming.value) {
                kotlinx.coroutines.delay(20) 
                
                val actualRead = 1024
                val mixedBuffer = ShortArray(actualRead)
                val micVol = if (_micEnabled.value) _micVolume.value else 0f
                val baseMusVol = _musicVolume.value

                // Prioridade de Vinheta e Ducking de Voz
                val vignetteActive = _isVignettePlaying.value
                val isSpeaking = _micEnabled.value && micVuMeter.value > duckThreshold
                
                val targetDuck = when {
                    vignetteActive -> 0.1f // Abaixa muito a música para a vinheta
                    isSpeaking -> duckVolume
                    else -> 1.0f
                }

                if (currentDuckFactor > targetDuck) {
                    currentDuckFactor -= fadeSpeed * 2
                } else if (currentDuckFactor < targetDuck) {
                    currentDuckFactor += fadeSpeed
                }
                currentDuckFactor = currentDuckFactor.coerceIn(0.1f, 1.0f)
                val finalMusVol = baseMusVol * currentDuckFactor

                // Abastece Música
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

                // Abastece Vinhetas
                while ((jingleStoreTail - jingleStoreHead + 8192) % 8192 < actualRead) {
                    val chunk = vignettePlayer.pcmQueue.poll()
                    if (chunk != null) {
                        for (sample in chunk) {
                            jingleStore[jingleStoreTail] = sample
                            jingleStoreTail = (jingleStoreTail + 1) % 8192
                        }
                    } else {
                        jingleStore[jingleStoreTail] = 0
                        jingleStoreTail = (jingleStoreTail + 1) % 8192
                    }
                }

                for (i in 0 until actualRead) {
                    val micSample = (if (_micEnabled.value && i < lastMicReadSize) micSharedBuffer[i] else 0) * micVol
                    val musSample = musicStore[musicStoreHead] * finalMusVol
                    val jinSample = jingleStore[jingleStoreHead] * 1.0f // Vinheta sempre 100%
                    
                    musicStoreHead = (musicStoreHead + 1) % 8192
                    jingleStoreHead = (jingleStoreHead + 1) % 8192
                    
                    mixedBuffer[i] = (micSample + musSample + jinSample).coerceIn(-32768f, 32767f).toInt().toShort()
                }

                streamManager.writeAudio(mixedBuffer, actualRead)
            }

            if (!isStreaming.value && !isReconnecting && retryCount < MAX_RETRY && status.value != "Desconectado") {
                isReconnecting = true
                retryCount++
                
                for (i in 10 downTo 1) {
                    _reconnectCountdown.value = i
                    streamManager.status.value = "RECONECTANDO ($retryCount)"
                    kotlinx.coroutines.delay(1000)
                }
                
                _reconnectCountdown.value = 0
                isReconnecting = false
                startStreaming()
            } else if (status.value == "Desconectado" || retryCount >= MAX_RETRY) {
                retryCount = 0
                _reconnectCountdown.value = 0
                streamManager.status.value = "Desconectado"
            }
        }
    }

    private fun stopStreaming() {
        streamManager.status.value = "Desconectado"
        streamManager.stopStream()
        if (!_micEnabled.value) {
            micLoopJob?.cancel()
            audioProcessor.stopCapture()
        }
        val context = getApplication<Application>()
        context.stopService(Intent(context, StreamingService::class.java))
    }

    fun playMusic(uris: List<Uri>) {
        _playlist.value = uris.take(5) 
        musicPlayer.setPlaylist(_playlist.value)
        musicPlayer.play()
    }

    fun setTrackAt(index: Int, uri: Uri) {
        val current = _playlist.value.toMutableList()
        while (current.size <= index) {
            current.add(Uri.EMPTY)
        }
        current[index] = uri
        _playlist.value = current.take(5)
        musicPlayer.setPlaylist(_playlist.value.filter { it != Uri.EMPTY })
    }

    fun getFileName(uri: Uri): String {
        if (uri == Uri.EMPTY) return "Vazio"
        var name = "Áudio"
        val context = getApplication<Application>()
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            name = uri.lastPathSegment ?: "Áudio"
        }
        return name.replaceBeforeLast("/", "").removePrefix("/")
    }

    fun removeTrackAt(index: Int) {
        val current = _playlist.value.toMutableList()
        if (index < current.size) {
            current.removeAt(index)
            _playlist.value = current
            musicPlayer.setPlaylist(_playlist.value)
        }
    }

    fun clearPlaylist() {
        _playlist.value = emptyList()
        musicPlayer.setPlaylist(emptyList())
    }

    fun setVignetteAt(index: Int, uri: Uri) {
        val current = _vignettes.value.toMutableList()
        current[index] = uri
        _vignettes.value = current
    }

    fun removeVignetteAt(index: Int) {
        val current = _vignettes.value.toMutableList()
        current[index] = null
        _vignettes.value = current
    }

    fun playVignette(index: Int) {
        _vignettes.value[index]?.let { uri ->
            vignettePlayer.setPlaylist(listOf(uri))
            vignettePlayer.play()
        }
    }

    fun toggleMusic() {
        if (isMusicPlaying.value) musicPlayer.pause() else musicPlayer.play()
    }

    fun nextSong() = musicPlayer.next()
    fun previousSong() = musicPlayer.previous()
    fun setMicVolume(volume: Float) { _micVolume.value = volume }
    fun setMusicVolume(volume: Float) {
        _musicVolume.value = volume
        viewModelScope.launch { musicPlayer.setVolume(volume) }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
        vignettePlayer.release()
        audioProcessor.stopCapture()
        streamManager.stopStream()
    }
}
