package com.example.batteryservicekotlin

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val batteryRepository = BatteryRepository.get()
    val unitListLiveData = batteryRepository.getUnits()

    val listDatesLiveData = batteryRepository.getDates()
}