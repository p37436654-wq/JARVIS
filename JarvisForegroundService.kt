package com.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class JarvisForegroundService : Service() {
    override fun onCreate() { super.onCreate(); createChannel(); startForeground(1001, notification()) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { return START_STICKY }
    private fun createChannel() { val nm=getSystemService(NotificationManager::class.java); nm.createNotificationChannel(NotificationChannel("jarvis","JARVIS Assistant",NotificationManager.IMPORTANCE_LOW)) }
    private fun notification(): Notification = NotificationCompat.Builder(this,"jarvis").setContentTitle("JARVIS is active").setContentText("Listening for the configured wake phrase").setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build()
    override fun onBind(intent: Intent?): IBinder? = null
}
