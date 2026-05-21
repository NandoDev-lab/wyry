package com.example.wyry.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

class AudioProcessor {
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    val vuMeter = MutableStateFlow(0f)

    @SuppressLint("MissingPermission")
    fun startCapture() {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
            } else {
                android.util.Log.e("AudioProcessor", "AudioRecord initialization failed")
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioProcessor", "Error starting capture", e)
        }
    }

    fun stopCapture() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun read(buffer: ShortArray): Int {
        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
        if (read > 0) {
            updateVuMeter(buffer, read)
        }
        return read
    }

    private fun updateVuMeter(buffer: ShortArray, read: Int) {
        var sum = 0f
        for (i in 0 until read) {
            sum += abs(buffer[i].toInt()).toFloat()
        }
        vuMeter.value = if (read > 0) sum / read / 32768f else 0f
    }
}
