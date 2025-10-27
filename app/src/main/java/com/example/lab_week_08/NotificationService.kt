package com.example.lab_week_08

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundServiceProper(): NotificationCompat.Builder {
        Log.d(TAG, "startForegroundServiceProper()")
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val builder = getNotificationBuilder(pendingIntent, channelId)

        // Mulai foreground (WAJIB sebelum kerja lama)
        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    private fun getPendingIntent(): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun createNotificationChannel(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "001 Channel"
            // Gunakan HIGH agar lebih terlihat
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            nm.createNotificationChannel(channel)
            channelId
        } else {
            ""
        }
    }

    private fun getNotificationBuilder(
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder {
        val b = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Countdown is running…")
            // pakai ikon yang pasti ada di semua proyek
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            b.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return b
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand(), flags=$flags, startId=$startId, intent=$intent")

        // Mulai foreground di sini (bukan di onCreate)
        notificationBuilder = startForegroundServiceProper()

        val id = intent?.getStringExtra(EXTRA_ID)
            ?: run {
                Log.e(TAG, "EXTRA_ID missing, stopping service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

        serviceHandler.post {
            Log.d(TAG, "Background task started (countdown)")
            countDownFromTenToZero(notificationBuilder)
            notifyCompletion(id)
            Log.d(TAG, "Background task finished, stopping service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun countDownFromTenToZero(builder: NotificationCompat.Builder) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 10 downTo 0) {
            try {
                Thread.sleep(1000L)
            } catch (_: InterruptedException) {
                Log.w(TAG, "Countdown interrupted")
                break
            }
            builder.setContentText("$i seconds until last warning").setSilent(true)
            nm.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun notifyCompletion(id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = id
        }
    }

    companion object {
        private const val TAG = "NotificationService"
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
