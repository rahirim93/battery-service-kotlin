package com.example.batteryservicekotlin

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.slider.RangeSlider
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

// Начало выбранного дня
fun startChosenDay(chosenDay: Calendar): Calendar {
    var calendar = chosenDay
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar
}

// Начало выбранного дня
fun endChosenDay(chosenDay: Calendar): Calendar {
    var calendar = chosenDay
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar
}

fun myToast(context: Context, string: String) {
    Toast.makeText(context, string, Toast.LENGTH_SHORT).show()
}

// Возвращает начало выбранного отрезка выбранного для в виде календаря
fun sliderStartInCalendar(slider: RangeSlider, datePicker: MaterialDatePicker<Long>): Calendar {
    val sliderStart = slider.values[0].toInt()  // Значение начала слайдера
    val calendarStart = Calendar.getInstance()  // Время начала выборки

    // Время начала выборки текущей даты приводим к дате выбранной в datePicker
    calendarStart.timeInMillis = datePicker.selection!!
    // При получении даты из datePicker время там установлено на 3:00:00.
    // Поэтому корректируем только поле часов
    calendarStart.set(Calendar.HOUR_OF_DAY, sliderStart)

    return calendarStart
}

// Возвращает конец выбранного отрезка выбранного для в виде календаря
fun sliderEndInCalendar(slider: RangeSlider, datePicker: MaterialDatePicker<Long>): Calendar {
    val sliderEnd = slider.values[1].toInt()    // Значение конца слайдера
    val calendarEnd = Calendar.getInstance()    // Время конца выборки
    // Время конца выборки текущей даты
    calendarEnd.timeInMillis = datePicker.selection!!
    // Если выбрать 24, то это уже следующий день
    // Поэтому если выбор на 24, то делаем на 1 сек раньше, чтобы остаться в теущих сутках
    if (sliderEnd == 24) {
        calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
        calendarEnd.set(Calendar.MINUTE, 59)
        calendarEnd.set(Calendar.SECOND, 59)
        //myToast(applicationContext, "${calendarStart.time}")
        //myToast(applicationContext, "${calendarEnd.time}")
    } else {
        // При получении даты из datePicker время там установлено на 3:00:00.
        // Поэтому корректируем только поле часов
        calendarEnd.set(Calendar.HOUR_OF_DAY, sliderEnd)
        //myToast(applicationContext, "${calendarStart.time}")
        //myToast(applicationContext, "${calendarEnd.time}")
    }
    return calendarEnd
}