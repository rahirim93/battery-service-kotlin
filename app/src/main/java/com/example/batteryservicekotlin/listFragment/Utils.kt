package com.example.batteryservicekotlin.listFragment

import com.example.batteryservicekotlin.BatteryBundleForAdapter
import com.example.batteryservicekotlin.database.Unit
import java.util.*

/** Вынесенные функции для фрагмента списка */


/** Фильтр записей БД для адаптера утилизатора.
 * Закомментировать пока помню как это работает. */
fun filterUnitsForAdapter(units: List<Unit>): List<BatteryBundleForAdapter> {

    val listFiltered = arrayListOf<Date>()
    units.forEach { unit ->
        if (listFiltered.isEmpty()) listFiltered.add(unit.date)
        val listFilteredIterator = listFiltered.listIterator()
        var inputDate: Date? = null
        while (listFilteredIterator.hasNext()) {
            val tempNextDate = listFilteredIterator.next()
            inputDate = if (tempNextDate.date != unit.date.date) {
                unit.date
            } else {
                null
            }
        }
        if (inputDate != null) listFilteredIterator.add(inputDate)
    }
    // Список с отфильтрованными датами
    var listDates = listFiltered


    // Дальше перебираем список с отфильтрованными датами
    // и для каждой даты вычисляем количество записей в эту дату
    // создавая при том список с количеством записей
    // Лист отфильтрованных списков
    var listSetFilteredInserts = arrayListOf<List<Unit>>()
    listDates.forEach { date ->
        //Начало дня перебираемой даты
        val startDayDate = Date(date.year,date.month,date.date,0,0,0)
        //Конец дня перебираемой даты
        val endDayDate = Date(date.year,date.month,date.date,23,59,59)
        // Даты в миллисекундах
        val startDayInMillis = startDayDate.time
        val endDayInMillis = endDayDate.time
        // Временный пустой лист для заполнения отфильтровнными записями в перебираемый день
        var listFilteredInserts = arrayListOf<Unit>()

        units.forEach {
            if (it.date.time in (startDayInMillis + 1) until endDayInMillis) {
                listFilteredInserts.add(it)
            }
        }
        listSetFilteredInserts.add(listFilteredInserts)
    }
    // Список объектов для адаптера
    var listOfBatteryBundles = arrayListOf<BatteryBundleForAdapter>()
    for (i in listDates.indices) {
        listOfBatteryBundles.add(
            BatteryBundleForAdapter(listDates[i], listSetFilteredInserts[i])
        )
    }

    return listOfBatteryBundles
}

/** Количество записей должно было быть на текущий момент
 * с учетом 5 сек интервала */
fun getMustNumberInsertsToday(): Int {

    val calendar = Calendar.getInstance()

    // Начало дня текущей даты
    val startDay = GregorianCalendar(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
        0, 0, 0)

    // Конец дня текущей даты
    val endDay = GregorianCalendar(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
        23, 59, 59)

    // Даты в миллисекундах
    val calendarInMillis = calendar.timeInMillis
    val startDayInMillis = startDay.timeInMillis
    val endDayInMillis = endDay.timeInMillis

    val numberInsertsMustBe = ((calendarInMillis - startDayInMillis) / 1000) / 5

    return numberInsertsMustBe.toInt()
}