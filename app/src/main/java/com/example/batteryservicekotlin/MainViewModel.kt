package com.example.batteryservicekotlin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.batteryservicekotlin.database.Unit

class MainViewModel : ViewModel() {

    private val batteryRepository = BatteryRepository.get()

    val unitListLiveData = batteryRepository.getUnits()

    val listDatesLiveData = batteryRepository.getDates()

    fun getAutostartService(): Boolean = batteryRepository.getAutostartService()

    suspend fun getUnits2(): List<Unit> = batteryRepository.getUnits2()

    suspend fun getLimit(offset: Int): List<Unit> = batteryRepository.getLimit(offset)

    suspend fun getCount(): Int = batteryRepository.getCount()


    fun chosenDayUnitsLiveData(startDay: Long, endDay: Long): LiveData<List<Unit>> {
        return batteryRepository.getChosenDayUnits(startDay, endDay)
    }

    suspend fun todayUnitsList(startDay: Long, endDay: Long): List<Unit> {
        return batteryRepository.getChosenDayUnitsList(startDay, endDay)
    }

    fun getStepRange(): Float = batteryRepository.getStepRange()

    fun setStepRange(step: Float) = batteryRepository.setStepRange(step)

    fun getDoubleBattery(): Boolean = batteryRepository.getDoubleBattery()

    fun getInversionCurrent(): Boolean = batteryRepository.getInversionCurrent()

    fun getCurrentCorrect(): Boolean = batteryRepository.getCurrentCorrect()

}