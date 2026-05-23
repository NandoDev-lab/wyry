package com.example.wyry.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wyry.audio.AudioProcessor
import com.example.wyry.audio.AudioEffects
import com.example.wyry.data.SettingsRepository
import com.example.wyry.data.StreamConfig
import com.example.wyry.player.MusicPlayer
import com.example.wyry.streaming.StreamManager
import com.example.wyry.streaming.StreamingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioProcessor = AudioProcessor()
    private val audioEffects = AudioEffects()
    private val musicPlayer = MusicPlayer(application)
    private val vignettePlayer = MusicPlayer(application)
    private val streamManager = StreamManager(application)
    private val repository = SettingsRepository(application)

    private val _profiles = MutableStateFlow<List<StreamConfig>>(emptyList())
    val profiles: StateFlow<List<StreamConfig>> = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<StreamConfig?>(null)
    val selectedProfile: StateFlow<StreamConfig?> = _selectedProfile.asStateFlow()

    private val _playlist = MutableStateFlow<List<Uri?>>(List(5) { null })
    val playlist: StateFlow<List<Uri?>> = _playlist.asStateFlow()

    private val _vignettes = MutableStateFlow<List<Uri?>>(List(5) { null })
    val vignettes: StateFlow<List<Uri?>> = _vignettes.asStateFlow()

    val isStreaming = streamManager.isStreaming
    val status = streamManager.status
    val micVuMeter = audioProcessor.vuMeter
    val musicVuMeter = musicPlayer.vuMeter

    val currentSongTitle = musicPlayer.currentSongTitle
    val isMusicPlaying = musicPlayer.isPlaying
    val musicPosition: StateFlow<Long> = musicPlayer.currentPosition.asStateFlow()
    val musicDuration: StateFlow<Long> = musicPlayer.duration.asStateFlow()
    
    private val _isVignettePlaying = MutableStateFlow(false)
    val isVignettePlaying: StateFlow<Boolean> = _isVignettePlaying.asStateFlow()

    private val _micVolume = MutableStateFlow(1f)
    val micVolume: StateFlow<Float> = _micVolume

    private val _micEnabled = MutableStateFlow(false)
    val micEnabled: StateFlow<Boolean> = _micEnabled

    private val _musicVolume = MutableStateFlow(0.5f)
    val musicVolume: StateFlow<Float> = _musicVolume

    private val _echoEnabled = MutableStateFlow(false)
    val echoEnabled: StateFlow<Boolean> = _echoEnabled

    private val _echoLevel = MutableStateFlow(0.4f)
    val echoLevel: StateFlow<Float> = _echoLevel

    private val _boostEnabled = MutableStateFlow(false)
    val boostEnabled: StateFlow<Boolean> = _boostEnabled

    private val _gateEnabled = MutableStateFlow(false)
    val gateEnabled: StateFlow<Boolean> = _gateEnabled

    private val _reconnectCountdown = MutableStateFlow(0)
    val reconnectCountdown: StateFlow<Int> = _reconnectCountdown.asStateFlow()

    private var audioEngineJob: Job? = null
    private var metadataJob: Job? = null
    private var isReconnecting = false
    private val MAX_RETRY = 3
    private var retryCount = 0

    init {
        viewModelScope.launch {
            combine(repository.profilesFlow, repository.selectedProfileIdFlow) { profiles, selectedId ->
                _profiles.value = profiles
                _selectedProfile.value = profiles.find { it.id == selectedId } ?: profiles.firstOrNull()
            }.collect()
        }
        
        viewModelScope.launch {
            vignettePlayer.isPlaying.collectLatest { playing ->
                _isVignettePlaying.value = playing
            }
        }

        // Carrega Playlist persistida
        viewModelScope.launch {
            repository.playlistFlow.first().let { uris ->
                val uriList = uris.map { if (it.isNotBlank()) Uri.parse(it) else null }
                _playlist.value = uriList
                val activeUris = uriList.filterNotNull()
                val titles = activeUris.map { getFileName(it) }
                musicPlayer.setPlaylist(activeUris, titles)
            }
        }

        // Carrega Vinhetas persistidas
        viewModelScope.launch {
            repository.vignettesFlow.first().let { uris ->
                _vignettes.value = uris.map { if (it != null) Uri.parse(it) else null }
            }
        }

        // Observa mudança de música para atualizar metadados na rádio
        viewModelScope.launch {
            currentSongTitle.collectLatest { title ->
                if (!title.isNullOrBlank() && isStreaming.value) {
                    _selectedProfile.value?.let { config ->
                        streamManager.updateMetadata(config, title)
                    }
                }
            }
        }
    }

    private fun persistUriPermission(uri: Uri) {
        try {
            val contentResolver = getApplication<Application>().contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            // Ignorado
        }
    }

    fun updateConfig(config: StreamConfig) {
        _selectedProfile.value = config
        viewModelScope.launch { repository.saveProfiles(_profiles.value.map { if (it.id == config.id) config else it }) }
    }

    fun toggleStream() {
        if (isStreaming.value) stopStreaming() else startStreaming()
    }

    fun simulateError() {
        if (isStreaming.value) {
            streamManager.isStreaming.value = false
            streamManager.status.value = "ERRO DE CONEXÃO"
        }
    }

    fun toggleMic() {
        _micEnabled.value = !_micEnabled.value
        startAudioEngineIfNeeded()
    }

    private fun startAudioEngineIfNeeded() {
        if (audioEngineJob?.isActive == true) return
        
        audioEngineJob = viewModelScope.launch(Dispatchers.IO) {
            audioProcessor.startCapture()
            
            val micBuffer = ShortArray(1024)
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

            while (_micEnabled.value || isStreaming.value) {
                val micBufferInternal = ShortArray(1024)
                val read = audioProcessor.read(micBufferInternal)
                if (read <= 0) {
                    kotlinx.coroutines.delay(10)
                    continue
                }

                audioEffects.process(micBufferInternal, read)

                val actualRead = read
                val mixedBuffer = ShortArray(actualRead)
                val micVol = if (_micEnabled.value) _micVolume.value else 0f
                val baseMusVol = _musicVolume.value

                val targetDuck = when {
                    _isVignettePlaying.value -> 0.1f
                    _micEnabled.value && micVuMeter.value > duckThreshold -> duckVolume
                    else -> 1.0f
                }
                if (currentDuckFactor > targetDuck) currentDuckFactor -= fadeSpeed * 2
                else if (currentDuckFactor < targetDuck) currentDuckFactor += fadeSpeed
                currentDuckFactor = currentDuckFactor.coerceIn(0.1f, 1.0f)
                val finalMusVol = baseMusVol * currentDuckFactor

                while ((musicStoreTail - musicStoreHead + 8192) % 8192 < read) {
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

                for (i in 0 until read) {
                    val micSample = micBufferInternal[i] * micVol
                    val musSample = musicStore[musicStoreHead] * finalMusVol
                    val jinSample = jingleStore[jingleStoreHead] * 1.0f
                    musicStoreHead = (musicStoreHead + 1) % 8192
                    jingleStoreHead = (jingleStoreHead + 1) % 8192
                    mixedBuffer[i] = (micSample + musSample + jinSample).coerceIn(-32768f, 32767f).toInt().toShort()
                }

                if (isStreaming.value) {
                    streamManager.writeAudio(mixedBuffer, actualRead)
                }
            }
            
            audioProcessor.stopCapture()
            audioEngineJob = null
            
            handleReconnection()
        }
    }

    private suspend fun handleReconnection() {
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
        }
    }

    private fun startStreaming() {
        val config = _selectedProfile.value ?: return
        val context = getApplication<Application>()
        val intent = Intent(context, StreamingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)

        streamManager.startStream(config)
        startAudioEngineIfNeeded()

        // Loop de Metadados: Envio inicial com delay e repetição a cada 30s
        metadataJob?.cancel()
        metadataJob = viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(5000) // Espera a rádio conectar
            while (isStreaming.value) {
                currentSongTitle.value?.let { title ->
                    if (title.isNotBlank()) {
                        streamManager.updateMetadata(config, title)
                    }
                }
                kotlinx.coroutines.delay(30000) // Reenvia a cada 30 segundos
            }
        }
    }

    private fun stopStreaming() {
        metadataJob?.cancel()
        streamManager.status.value = "Desconectado"
        streamManager.stopStream()
        val context = getApplication<Application>()
        context.stopService(Intent(context, StreamingService::class.java))
    }

    fun selectProfile(profile: StreamConfig) {
        viewModelScope.launch { repository.selectProfile(profile.id) }
    }

    fun addProfile(name: String) {
        val newProfile = StreamConfig(name = name)
        val newList = _profiles.value + newProfile
        saveProfiles(newList)
        selectProfile(newProfile)
    }

    fun updateProfile(updated: StreamConfig) {
        val newList = _profiles.value.map { if (it.id == updated.id) updated else it }
        saveProfiles(newList)
    }

    fun deleteProfile(profile: StreamConfig) {
        if (_profiles.value.size <= 1) return
        val newList = _profiles.value.filter { it.id != profile.id }
        saveProfiles(newList)
        if (_selectedProfile.value?.id == profile.id) selectProfile(newList.first())
    }

    private fun saveProfiles(list: List<StreamConfig>) {
        viewModelScope.launch { repository.saveProfiles(list) }
    }

    fun playMusic(uris: List<Uri>) {
        uris.forEach { persistUriPermission(it) }
        val newPlaylist = List(5) { i -> uris.getOrNull(i) }
        _playlist.value = newPlaylist
        val titles = newPlaylist.filterNotNull().map { getFileName(it) }
        musicPlayer.setPlaylist(newPlaylist.filterNotNull(), titles)
        musicPlayer.play()
        savePlaylist()
    }

    private fun savePlaylist() {
        viewModelScope.launch {
            repository.savePlaylist(_playlist.value.map { it?.toString() ?: "" })
        }
    }

    private fun saveVignettes() {
        viewModelScope.launch {
            repository.saveVignettes(_vignettes.value.map { it?.toString() })
        }
    }

    fun getFileName(uri: Uri): String {
        if (uri == Uri.EMPTY) return "Vazio"
        var name = "Áudio"
        try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) name = cursor.getString(nameIndex)
            }
        } catch (e: Exception) { name = uri.lastPathSegment ?: "Áudio" }
        return name.replaceBeforeLast("/", "").removePrefix("/")
    }

    fun setTrackAt(index: Int, uri: Uri) {
        persistUriPermission(uri)
        val current = _playlist.value.toMutableList()
        while (current.size < 5) current.add(null)
        if (index in 0 until 5) {
            current[index] = uri
            _playlist.value = current
            val activeUris = current.filterNotNull()
            val titles = activeUris.map { getFileName(it) }
            musicPlayer.setPlaylist(activeUris, titles)
            savePlaylist()
        }
    }

    fun removeTrackAt(index: Int) {
        val current = _playlist.value.toMutableList()
        if (index in 0 until current.size) {
            current[index] = null
            _playlist.value = current
            val activeUris = current.filterNotNull()
            val titles = activeUris.map { getFileName(it) }
            musicPlayer.setPlaylist(activeUris, titles)
            savePlaylist()
        }
    }

    fun clearPlaylist() {
        _playlist.value = List(5) { null }
        musicPlayer.setPlaylist(emptyList(), emptyList())
        savePlaylist()
    }

    fun setVignetteAt(index: Int, uri: Uri) {
        persistUriPermission(uri)
        val current = _vignettes.value.toMutableList()
        current[index] = uri
        _vignettes.value = current
        saveVignettes()
    }

    fun removeVignetteAt(index: Int) {
        val current = _vignettes.value.toMutableList()
        current[index] = null
        _vignettes.value = current
        saveVignettes()
    }

    fun playVignette(index: Int) {
        _vignettes.value[index]?.let { uri ->
            vignettePlayer.setPlaylist(listOf(uri), listOf("Vinheta ${index + 1}"))
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

    fun toggleEcho() {
        _echoEnabled.value = !_echoEnabled.value
        audioEffects.echoEnabled = _echoEnabled.value
    }

    fun setEchoLevel(level: Float) {
        _echoLevel.value = level
        audioEffects.echoFeedback = level
    }

    fun toggleBoost() {
        _boostEnabled.value = !_boostEnabled.value
        audioEffects.boostEnabled = _boostEnabled.value
    }

    fun toggleGate() {
        _gateEnabled.value = !_gateEnabled.value
        audioEffects.gateEnabled = _gateEnabled.value
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
        vignettePlayer.release()
        audioProcessor.stopCapture()
        streamManager.stopStream()
    }
}
