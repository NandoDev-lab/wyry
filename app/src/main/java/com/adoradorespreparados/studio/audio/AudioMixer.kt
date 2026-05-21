package com.adoradorespreparados.studio.audio

import kotlin.math.max
import kotlin.math.min

object AudioMixer {

    /**
     * Mistura dois buffers PCM 16-bit.
     * @param micBuffer Buffer do microfone
     * @param musicBuffer Buffer da música
     * @param musicVolume Volume da música (0.0 a 1.0)
     * @param applyDucking Se verdadeiro, reduz drasticamente o volume da música
     */
    fun mix(
        micBuffer: ShortArray,
        musicBuffer: ShortArray,
        musicVolume: Float,
        applyDucking: Boolean
    ): ShortArray {
        val result = ShortArray(micBuffer.size)
        val finalMusicVol = if (applyDucking) musicVolume * 0.2f else musicVolume

        for (i in micBuffer.indices) {
            val micSample = micBuffer[i].toInt()
            val musicSample = if (i < musicBuffer.size) (musicBuffer[i] * finalMusicVol).toInt() else 0
            
            // Soma os sinais e evita clipping (estouro de áudio)
            val mixed = micSample + musicSample
            result[i] = max(Short.MIN_VALUE.toInt(), min(Short.MAX_VALUE.toInt(), mixed)).toShort()
        }
        return result
    }
}
