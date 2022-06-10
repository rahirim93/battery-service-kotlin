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

// Перевод вречени в часы в десятично виде например 15,5 ч.
fun timeInHours(date: Date): Double {
    val hours = date.hours.toDouble() // Целых часов
    val minutes = date.minutes.toDouble() / 60.0 // Минуты в часы
    val seconds = date.seconds.toDouble() / 3600.0 // Секунды в часы
    return hours + minutes + seconds
}