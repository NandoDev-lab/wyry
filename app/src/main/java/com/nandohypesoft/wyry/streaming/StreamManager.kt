package com.nandohypesoft.wyry.streaming

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.nandohypesoft.wyry.data.StreamConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.*
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
        // Adicionado -fflags nobuffer e -flush_packets 1 para latência mínima
        val command = "-f s16le -ar 44100 -ac 1 -i $pipePath -c:a libmp3lame -b:a ${config.bitrate}k -legacy_icecast 1 -content_type audio/mpeg -ice_name \"Adoradores Studio\" -tune zerolatency -preset ultrafast -fflags nobuffer -flush_packets 1 -f mp3 $url"

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

    fun updateMetadata(config: com.nandohypesoft.wyry.data.StreamConfig, metadata: String) {
        if (!isStreaming.value) return
        
        val cleanMetadata = metadata.trim()
        val encodedMetadata = java.net.URLEncoder.encode(cleanMetadata, "UTF-8")
        val mount = if (config.mountpoint.startsWith("/")) config.mountpoint else "/${config.mountpoint}"
        
        // Padrão exato sugerido: mode=updinfo&mount=/stream&song=Artista+-+Musica
        val urlString = "http://${config.host}:${config.port}/admin/metadata?" +
                "mode=updinfo&mount=$mount&song=$encodedMetadata"
        
        val auth = "${config.user}:${config.pass}"
        val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("Authorization", "Basic $encodedAuth")
                connection.setRequestProperty("User-Agent", "BUTT/0.1.15") 
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                
                val responseCode = connection.responseCode
                Log.d("StreamManager", "Metadata Update ($cleanMetadata) -> Response: $responseCode")
                
                // Se der erro 401 (Não autorizado), tenta usar 'admin' como usuário
                if (responseCode == 401 && config.user != "admin") {
                    val adminAuth = "admin:${config.pass}"
                    val encodedAdmin = android.util.Base64.encodeToString(adminAuth.toByteArray(), android.util.Base64.NO_WRAP)
                    val retryConn = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
                    retryConn.setRequestProperty("Authorization", "Basic $encodedAdmin")
                    retryConn.setRequestProperty("User-Agent", "BUTT/0.1.15")
                    Log.d("StreamManager", "Retry Admin Result: ${retryConn.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("StreamManager", "Erro na rede ao enviar metadados", e)
            }
        }
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
