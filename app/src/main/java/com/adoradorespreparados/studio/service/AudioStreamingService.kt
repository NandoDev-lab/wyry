package com.adoradorespreparados.studio.service

import android.app.*
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.adoradorespreparados.studio.data.StreamingConfig
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class AudioStreamingService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private var audioRecord: AudioRecord? = null
    private var ffmpegSession: FFmpegSession? = null
    private var isStreaming = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "START") {
            startForegroundService()
            startStreaming()
        } else if (action == "STOP") {
            stopStreaming()
            stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "streaming_channel"
        val channel = NotificationChannel(
            channelId, 
            "Transmissão ao Vivo", 
            NotificationManager.IMPORTANCE_HIGH
        )
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        // Intent para parar o serviço via botão na notificação
        val stopIntent = Intent(this, AudioStreamingService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para abrir o app ao clicar na notificação
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, com.adoradorespreparados.studio.MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Adoradores Preparados Studio")
            .setContentText("Você está ao vivo!")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "PARAR", stopPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
    }

    private fun startStreaming() {
        if (isStreaming) return
        isStreaming = true

        scope.launch {
            val sampleRate = 44100
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
            
            val pipedOut = PipedOutputStream()
            val pipedIn = PipedInputStream(pipedOut)

            // FFmpeg Command: Lê do pipe e envia para o servidor
            // Exemplo simplificado para Icecast MP3
            val url = "icecast://source:senha@seu-servidor.com:8000/stream" 
            val command = "-f s16le -ar $sampleRate -ac 1 -i pipe:0 -c:a libmp3lame -b:a 128k -f mp3 $url"

            ffmpegSession = FFmpegKit.executeAsync(command) { session ->
                isStreaming = false
            }

            audioRecord?.startRecording()
            val buffer = ByteArray(bufferSize)

            try {
                while (isStreaming) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        pipedOut.write(buffer, 0, read)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pipedOut.close()
            }
        }
    }

    private fun stopStreaming() {
        isStreaming = false
        audioRecord?.stop()
        audioRecord?.release()
        FFmpegKit.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
