package com.example.lab_week_08

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // aman karena @id/main ada di layout kamu
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

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
        workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                toast("Second process is done")
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
