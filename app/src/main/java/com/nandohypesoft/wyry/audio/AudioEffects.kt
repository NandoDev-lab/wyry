package com.nandohypesoft.wyry.audio

import kotlin.math.abs

class AudioEffects {
    // 1. Echo / Delay
    private var delayBuffer = ShortArray(44100)
    private var writePos = 0
    var echoEnabled = false
    var echoFeedback = 0.4f
    var delayMs = 250

    // 2. Voice Boost (Radio Punch)
    var boostEnabled = false
    var boostLevel = 1.5f // Ganho de 50%

    // 3. Noise Gate (Silencia ruídos de fundo)
    var gateEnabled = false
    var gateThreshold = 500 // Nível mínimo para abrir o gate

    fun process(buffer: ShortArray, length: Int) {
        for (i in 0 until length) {
            var sample = buffer[i].toInt()

            // Aplica Noise Gate primeiro
            if (gateEnabled && abs(sample) < gateThreshold) {
                sample = 0
            }

            // Aplica Voice Boost
            if (boostEnabled) {
                sample = (sample * boostLevel).toInt().coerceIn(-32768, 32767)
            }

            // Aplica Echo
            if (echoEnabled) {
                val delaySamples = (delayMs * 44.1f).toInt().coerceIn(1, delayBuffer.size - 1)
                val readPos = (writePos - delaySamples + delayBuffer.size) % delayBuffer.size
                val delayedSample = delayBuffer[readPos]
                
                val newSample = (sample + (delayedSample * echoFeedback)).toInt().coerceIn(-32768, 32767)
                sample = newSample
                
                // Salva no buffer com feedback
                delayBuffer[writePos] = newSample.toShort()
            } else {
                // Se o echo estiver desligado, limpa o buffer para não "vazar" som antigo quando ligar
                delayBuffer[writePos] = 0
            }

            writePos = (writePos + 1) % delayBuffer.size
            buffer[i] = sample.toShort()
        }
    }
}
