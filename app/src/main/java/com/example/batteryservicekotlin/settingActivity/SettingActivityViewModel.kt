package com.example.batteryservicekotlin.settingActivity

import androidx.lifecycle.ViewModel
import com.example.batteryservicekotlin.BatteryRepository

class SettingActivityViewModel: ViewModel() {

    private val batteryRepository = BatteryRepository.get()

    fun getAutostartService(): Boolean = batteryRepository.getAutostartService()

    fun setAutostartService(isOn: Boolean) = batteryRepository.setAutostartService(isOn)

    fun getTestRestartService(): Boolean = batteryRepository.getTestRestartService()

    fun setTestRestartService(isOn: Boolean) = batteryRepository.setTestRestartService(isOn)

    fun getStepRange(): Float = batteryRepository.getStepRange()

    fun setStepRange(step: Float) = batteryRepository.setStepRange(step)

    fun getDoubleBattery(): Boolean = batteryRepository.getDoubleBattery()

    fun setDoubleBattery(doubleBattery: Boolean) = batteryRepository.setDoubleBattery(doubleBattery)

    fun getInversionCurrent(): Boolean = batteryRepository.getInversionCurrent()

    fun setInversionCurrent(inversion: Boolean) = batteryRepository.setInversionCurrent(inversion)

    fun getCurrentCorrect(): Boolean = batteryRepository.getCurrentCorrect()

    fun setCurrentCorrect(correct: Boolean) = batteryRepository.setCurrentCorrect(correct)

}