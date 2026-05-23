package com.nandohypesoft.vibecast.audio

import kotlin.math.abs

class AudioEffects {
    private var delayBuffer = ShortArray(44100)
    private var writePos = 0
    var echoEnabled = false
    var echoFeedback = 0.4f
    var delayMs = 250

    var boostEnabled = false
    var boostLevel = 1.5f

    var gateEnabled = false
    var gateThreshold = 500

    fun process(buffer: ShortArray, length: Int) {
        for (i in 0 until length) {
            var sample = buffer[i].toInt()

            if (gateEnabled && abs(sample) < gateThreshold) {
                sample = 0
            }

            if (boostEnabled) {
                sample = (sample * boostLevel).toInt().coerceIn(-32768, 32767)
            }

            if (echoEnabled) {
                val delaySamples = (delayMs * 44.1f).toInt().coerceIn(1, delayBuffer.size - 1)
                val readPos = (writePos - delaySamples + delayBuffer.size) % delayBuffer.size
                val delayedSample = delayBuffer[readPos]
                
                val newSample = (sample + (delayedSample * echoFeedback)).toInt().coerceIn(-32768, 32767)
                sample = newSample
                
                delayBuffer[writePos] = newSample.toShort()
            } else {
                delayBuffer[writePos] = 0
            }

            writePos = (writePos + 1) % delayBuffer.size
            buffer[i] = sample.toShort()
        }
    }
}
