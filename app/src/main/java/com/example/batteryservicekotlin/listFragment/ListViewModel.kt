package com.example.batteryservicekotlin.listFragment

import androidx.lifecycle.ViewModel
import com.example.batteryservicekotlin.BatteryRepository

class ListViewModel : ViewModel() {

    private val batteryRepository = BatteryRepository.get()

    val unitListLiveData = batteryRepository.getUnits()

    val listDatesLiveData = batteryRepository.getDates()

}