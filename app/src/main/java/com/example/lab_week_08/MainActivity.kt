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
import androidx.work.*
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy { WorkManager.getInstance(this) }

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        requestPostNotificationsPermissionIfNeeded()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        val firstRequest = OneTimeWorkRequestBuilder<FirstWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(FirstWorker.INPUT_DATA_ID, id).build())
            .build()

        val secondRequest = OneTimeWorkRequestBuilder<SecondWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(SecondWorker.INPUT_DATA_ID, id).build())
            .build()

        val thirdRequest = OneTimeWorkRequestBuilder<ThirdWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(ThirdWorker.INPUT_DATA_ID, id).build())
            .build()

        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()

        workManager.getWorkInfoByIdLiveData(firstRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                toast("First process is done")
            }
        }

        workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                toast("Second process is done")
                launchNotificationService()
            }
        }

        workManager.getWorkInfoByIdLiveData(thirdRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                toast("Third process is done")
                launchSecondNotificationService()
                SecondNotificationService.trackingCompletion.observe(this) { doneId ->
                    toast("Process for Notification Channel ID $doneId is done!")
                }
            }
        }
    }

    private fun launchNotificationService() {
        NotificationService.trackingCompletion.observe(this) { doneId ->
            toast("Process for Notification Channel ID $doneId is done!")
        }

        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(NotificationService.EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun launchSecondNotificationService() {
        val serviceIntent = Intent(this, SecondNotificationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

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
