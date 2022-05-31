package com.example.batteryservicekotlin

import android.util.Log
import java.util.*

fun log(msg: String) {
    Log.d("rahirim", msg)
}

fun startDayMillis(): Long {
    val todayDay = Calendar.getInstance()

    // Начало сегодняшнего дня с 00:00:00
    val todayStartTime = Calendar.getInstance()
    todayStartTime.set(
        todayDay.get(Calendar.YEAR),
        todayDay.get(Calendar.MONTH),
        todayDay.get(Calendar.DAY_OF_MONTH),
        0, 0, 0)

    // Начало сегодня в миллисекундах
    return todayStartTime.timeInMillis
}

fun endDayMillis(): Long {
    val todayDay = Calendar.getInstance()

    // Конец сегодняшнего дня в 23:59:59
    val todayEndTime = Calendar.getInstance()
    todayEndTime.set(
        todayDay.get(Calendar.YEAR),
        todayDay.get(Calendar.MONTH),
        todayDay.get(Calendar.DAY_OF_MONTH),
        23, 59, 59)

    // Конец сегодня в миллисекундах
    return todayEndTime.timeInMillis
}