package com.example.lab_week_08

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy { WorkManager.getInstance(this) }

    // Launcher izin notifikasi (Android 13+)
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* optional: tidak wajib diproses untuk modul */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Pastikan view root punya id @id/main (sudah ada di activity_main.xml)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // 1) Minta izin notifikasi (API 33+)
        requestPostNotificationsPermissionIfNeeded()

        // 2) Siapkan WorkManager (Part 1: First -> Second)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        val firstRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<FirstWorker>()
                .setConstraints(constraints)
                .setInputData(Data.Builder().putString(FirstWorker.INPUT_DATA_ID, id).build())
                .build()

        val secondRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SecondWorker>()
                .setConstraints(constraints)
                .setInputData(Data.Builder().putString(SecondWorker.INPUT_DATA_ID, id).build())
                .build()

        workManager.beginWith(firstRequest).then(secondRequest).enqueue()

        workManager.getWorkInfoByIdLiveData(firstRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                toast("First process is done")
            }
        }

        // 3) Step 9 modul — Setelah SecondWorker selesai → panggil launchNotificationService()
        workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                toast("Second process is done")
                launchNotificationService()
            }
        }
    }

    // =========================
    // Step 4 — fungsi peluncur service + observe selesai
    // =========================
    private fun launchNotificationService() {
        // Observe sinyal selesai dari service (dipost saat countdown selesai)
        NotificationService.trackingCompletion.observe(this) { doneId ->
            toast("Process for Notification Channel ID $doneId is done!")
        }

        // Mulai Foreground Service + kirim EXTRA_ID (wajib sesuai modul)
        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(NotificationService.EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // Izin notifikasi untuk Android 13+
    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
