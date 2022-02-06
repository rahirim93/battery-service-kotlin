package com.example.batteryservicekotlin

import android.app.Application

class BatteryServiceApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        BatteryRepository.initialize(this)
    }
}