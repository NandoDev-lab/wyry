package com.example.wyry.streaming

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.example.wyry.data.StreamConfig
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StreamManager(private val context: Context) {
    private var ffmpegSession: FFmpegSession? = null
    private var pipeOutputStream: FileOutputStream? = null
    
    val isStreaming = MutableStateFlow(value = false)
    val status = MutableStateFlow(value = "Desconectado")

    fun startStream(config: StreamConfig) {
        val pipePath = FFmpegKitConfig.registerNewFFmpegPipe(context)
        
        FFmpegKitConfig.enableLogCallback { log ->
            Log.d("FFmpeg", log.message)
        }

        val url = "icecast://${config.user}:${config.pass}@${config.host}:${config.port}${config.mountpoint}"
        // Added -content_type and explicit mp3 format
        val command = "-f s16le -ar 44100 -ac 1 -i $pipePath -c:a libmp3lame -b:a ${config.bitrate}k -content_type audio/mpeg -f mp3 $url"

        status.value = "Conectando..."
        
        ffmpegSession = FFmpegKit.executeAsync(command) { session ->
            Log.d("StreamManager", "FFmpeg finished with state ${session.state}")
            isStreaming.value = false
            status.value = "Desconectado"
            try {
                pipeOutputStream?.close()
            } catch (e: Exception) {
                Log.e("StreamManager", "Error closing pipe", e)
            }
            pipeOutputStream = null
        }

        try {
            pipeOutputStream = FileOutputStream(pipePath)
            isStreaming.value = true
            status.value = "ON AIR"
        } catch (e: Exception) {
            Log.e("StreamManager", "Error opening pipe", e)
            status.value = "Erro na conexão"
            isStreaming.value = false
        }
    }

    fun stopStream() {
        FFmpegKit.cancel()
        isStreaming.value = false
        status.value = "Desconectado"
        try {
            pipeOutputStream?.close()
        } catch (e: Exception) {
            // Ignore
        }
        pipeOutputStream = null
    }

    fun writeAudio(data: ShortArray, length: Int) {
        if (!isStreaming.value) return
        try {
            val byteBuffer = ByteBuffer.allocate(length * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until length) {
                byteBuffer.putShort(data[i])
            }
            pipeOutputStream?.write(byteBuffer.array())
        } catch (e: Exception) {
            Log.e("StreamManager", "Error writing to pipe", e)
        }
    }
}
