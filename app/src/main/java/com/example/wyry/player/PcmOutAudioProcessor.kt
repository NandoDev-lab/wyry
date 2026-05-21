package com.example.wyry.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue

@UnstableApi
class PcmOutAudioProcessor : AudioProcessor {
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var isActive = false
    private var buffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER

    val pcmQueue = LinkedBlockingQueue<ShortArray>(100)

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        // Mantemos Stereo para a saída do falante para não perder qualidade local
        outputAudioFormat = inputAudioFormat 
        isActive = true
        return outputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val remaining = inputBuffer.remaining()
        
        // 1. Captura para a Transmissão (Mixagem)
        val shortBuffer = inputBuffer.asShortBuffer()
        val shorts = ShortArray(remaining / 2)
        shortBuffer.get(shorts)

        // Downmix apenas para a rádio (mantém mono na transmissão)
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

        // 2. Devolve o áudio original para o ExoPlayer tocar nos alto-falantes
        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }
        
        inputBuffer.position(0) // Volta ao início para copiar
        buffer.put(inputBuffer)
        buffer.flip()
        outputBuffer = buffer
        
        // Marca como consumido para o ExoPlayer
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
