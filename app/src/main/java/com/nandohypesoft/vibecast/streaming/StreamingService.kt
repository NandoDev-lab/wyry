package com.nandohypesoft.vibecast.streaming

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nandohypesoft.vibecast.R

import android.app.PendingIntent
import com.nandohypesoft.vibecast.MainActivity

class StreamingService : Service() {

    private val binder = LocalBinder()
    private val CHANNEL_ID = "StreamingChannel"
    private val NOTIFICATION_ID = 1

    companion object {
        const val ACTION_STOP = "STOP_STREAMING"
    }

    inner class LocalBinder : Binder() {
        fun getService(): StreamingService = this@StreamingService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Se o usuário clicar em "PARAR" na notificação, encerramos o serviço
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID, 
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotification(): Notification {
        // Intent para abrir o app ao clicar na notificação
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para o botão de parar
        val stopIntent = Intent(this, StreamingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VibeCast Studio")
            .setContentText("A transmissão está ativa em segundo plano.")
            .setSmallIcon(R.drawable.logo_vibecast_icon) // Usando a sua logo como ícone
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Impede que o usuário deslize para remover a notificação
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "PARAR TRANSMISSÃO", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "VibeCast Streaming",
                NotificationManager.IMPORTANCE_DEFAULT // Aumentado para aparecer na barra
            ).apply {
                description = "Notificação de transmissão ativa"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
