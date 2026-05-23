package com.nandohypesoft.vibecast.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs

@UnstableApi
class PcmOutAudioProcessor : AudioProcessor {
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var isActive = false
    private var buffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER

    val pcmQueue = LinkedBlockingQueue<ShortArray>(100)
    val vuMeter = MutableStateFlow(0f)

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        outputAudioFormat = inputAudioFormat 
        isActive = true
        return outputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val remaining = inputBuffer.remaining()
        
        val shortBuffer = inputBuffer.asShortBuffer()
        val shorts = ShortArray(remaining / 2)
        shortBuffer.get(shorts)

        val monoShorts = if (inputAudioFormat.channelCount >= 2) {
            ShortArray(shorts.size / inputAudioFormat.channelCount) { i ->
                var sum = 0
                for (ch in 0 until inputAudioFormat.channelCount) {
                    sum += shorts[i * inputAudioFormat.channelCount + ch]
                }
                (sum / inputAudioFormat.channelCount).toShort()
            }
        } else {
            shorts
        }

        if (!pcmQueue.offer(monoShorts)) {
            pcmQueue.poll()
            pcmQueue.offer(monoShorts)
        }

        var max = 0f
        for (sample in monoShorts) {
            val absValue = abs(sample.toInt()).toFloat()
            if (absValue > max) max = absValue
        }
        
        val normalized = max / 32768f
        val visibleLevel = kotlin.math.sqrt(normalized) * 1.1f
        
        vuMeter.value = (vuMeter.value * 0.15f) + (visibleLevel.coerceIn(0f, 1f) * 0.85f)

        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }
        
        inputBuffer.position(0)
        buffer.put(inputBuffer)
        buffer.flip()
        outputBuffer = buffer
        
        inputBuffer.position(inputBuffer.limit())
    }

    override fun queueEndOfStream() {}

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = false

    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        pcmQueue.clear()
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        isActive = false
    }
}
