package com.example.batteryservicekotlin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.batteryservicekotlin.database.Unit

class MainViewModel : ViewModel() {

    private val batteryRepository = BatteryRepository.get()

    val unitListLiveData = batteryRepository.getUnits()

    val listDatesLiveData = batteryRepository.getDates()

    fun todayUnitsLiveData(startDay: Long, endDay: Long): LiveData<List<Unit>> {
        return batteryRepository.getChosenDayUnits(startDay, endDay)
    }
}