package com.example.lab_week_08.worker

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class FirstWorker(
    context: Context, params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val id = inputData.getString(INPUT_DATA_ID)
        Thread.sleep(3000L) // simulasi kerja berat 3 detik
        val out = Data.Builder().putString(OUTPUT_DATA_ID, id).build()
        return Result.success(out)
    }

    companion object {
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId"
    }
}
