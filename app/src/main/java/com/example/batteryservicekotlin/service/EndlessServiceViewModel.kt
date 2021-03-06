package com.example.batteryservicekotlin.service

import androidx.lifecycle.ViewModel
import com.example.batteryservicekotlin.BatteryRepository
import com.example.batteryservicekotlin.database.Unit

class EndlessServiceViewModel : ViewModel() {

    private val batteryRepository = BatteryRepository.get()

    val unitListLiveData = batteryRepository.getUnits()


    fun addUnit(unit: Unit) {
        batteryRepository.addUnit(unit)
    }
}