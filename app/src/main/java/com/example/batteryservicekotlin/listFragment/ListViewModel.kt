package com.example.batteryservicekotlin.listFragment

import androidx.lifecycle.ViewModel
import com.example.batteryservicekotlin.BatteryRepository

class ListViewModel : ViewModel() {

    private val batteryRepository = BatteryRepository.get()
    val unitListLiveData = batteryRepository.getUnits()


    /**
     * Итак, что нужно сделать. Нужно вывести в список даты в которые были сделаны записи в БД.
     * При выборе дня в списке будет выведена детализация данных в этот день.
     * Как вытащить эти дни?
     * Можно вытащить оттуда весь столбец. Пробежать его циклом и вытащить дни
     */

}