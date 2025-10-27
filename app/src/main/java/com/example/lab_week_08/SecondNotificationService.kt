package com.example.lab_week_08

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    private lateinit var handler: Handler
    private lateinit var builder: NotificationCompat.Builder

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val thread = HandlerThread("SecondNotifThread").apply { start() }
        handler = Handler(thread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        builder = createAndStartNotification()
        handler.post {
            for (i in 5 downTo 0) {
                Thread.sleep(1000)
                builder.setContentText("$i seconds remaining for second notification")
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, builder.build())
            }
            notifyCompletion("002") // <---- tambahkan ini
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createAndStartNotification(): NotificationCompat.Builder {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = "002"
            val channel = NotificationChannel(id, "002 Channel", NotificationManager.IMPORTANCE_HIGH)
            val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
            id
        } else ""
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0)
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker process is done")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        startForeground(NOTIF_ID, builder.build())
        return builder
    }

    private fun notifyCompletion(id: String) {
        Handler(Looper.getMainLooper()).post { mutableID.value = id }
    }

    companion object {
        const val NOTIF_ID = 0xCA8
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
