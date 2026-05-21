package com.adoradorespreparados.studio.ui

import androidx.lifecycle.ViewModel
import com.adoradorespreparados.studio.data.StreamingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StreamingViewModel : ViewModel() {

    private val _config = MutableStateFlow(StreamingConfig())
    val config: StateFlow<StreamingConfig> = _config

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    private val _micVolume = MutableStateFlow(1.0f)
    val micVolume: StateFlow<Float> = _micVolume

    private val _musicVolume = MutableStateFlow(0.5f)
    val musicVolume: StateFlow<Float> = _musicVolume

    fun updateConfig(newConfig: StreamingConfig) {
        _config.value = newConfig
    }

    fun toggleStreaming() {
        _isStreaming.value = !_isStreaming.value
    }

    fun setMicVolume(value: Float) {
        _micVolume.value = value
    }

    fun setMusicVolume(value: Float) {
        _musicVolume.value = value
    }
}
