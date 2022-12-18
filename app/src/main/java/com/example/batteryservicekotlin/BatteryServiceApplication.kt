package com.example.batteryservicekotlin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BatteryServiceApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        BatteryRepository.initialize(this)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "myChannel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("rahirim", name, importance)
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}