package com.example.batteryservicekotlin

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters

private const val SAVED_SIZE = "saved size"

class MyWorker(private val context: Context, workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private var pref: SharedPreferences? = null

    override suspend fun doWork(): Result {

        pref = context.getSharedPreferences("Name", Context.MODE_PRIVATE)

        BatteryRepository.initialize(context)

        val size = BatteryRepository.get().getChosenDayUnitsList(startDayMillis(), endDayMillis()).size
        val savedSize = pref?.getInt(SAVED_SIZE, 0)

        if (size == savedSize) {
            val resources = context.resources
            val notification = NotificationCompat
                .Builder(context, "rahirim")
                .setTicker("Hello")
                .setContentText("Сервис остановлен")
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setAutoCancel(true)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(0, notification)
        } else {
//            val notification = NotificationCompat
//                .Builder(context, "rahirim")
//                .setTicker("Hello")
//                .setContentText("Было: $savedSize Стало: $size")
//                .setSmallIcon(android.R.drawable.ic_menu_report_image)
//                .setAutoCancel(true)
//                .build()
//
//            val notificationManager = NotificationManagerCompat.from(context)
//            notificationManager.notify(0, notification)
        }

        val editor = pref?.edit()
        editor?.putInt(SAVED_SIZE, size)
        editor?.apply()

        return Result.success()
    }
}