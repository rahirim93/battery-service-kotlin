package com.example.batteryservicekotlin.listFragment

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
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

/** Функция для получения данных о батарее */
private fun getBatteryInfo(context: Context) {
    // Для получения энергии добавить разрешение
    //<uses- Manifest.permission android:name="android.permission.BATTERY_STATS"
    //tools:ignore="ProtectedPermissions" />


    val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager

    //Remaining battery capacity as an integer percentage of total capacity (with no fractional part).
    //Оставшаяся емкость аккумулятора в виде целого процента от общей емкости (без дробной части).
    val capacityInPercentage = batteryManager
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    //log("Процент заряда: $capacityInPercentage")

    // Battery capacity in microampere-hours, as an integer.
    // Емкость аккумулятора в микроампер-часах, как целое число.
    val capacityInMicroampereHours = batteryManager
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
    //log("Емкость в микроампер-часах: $capacityInMicroampereHours")

    // Average battery current in microamperes, as an integer.
    // Средний ток батареи в микроамперах, как целое число.
    val currentAverage = batteryManager
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
    //log("Средний ток потребления: $currentAverage")

    // Instantaneous battery current in microamperes, as an integer.
    // Мгновенный ток батареи в микроамперах, как целое число.
    val currentNow = batteryManager
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    //log("Текущий ток потребления: $currentNow")

    // Battery remaining energy in nanowatt-hours, as a long integer.
    // Оставшаяся энергия батареи в нановатт-часах, как длинное целое число.
    val energyCounter = batteryManager
        .getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
    //log("Оставшаяся энергия: $energyCounter")
    // Не работает, возможно из-за:
    // Return the value of a battery property of long type
    // If the platform does not provide the property queried,
    // this value will be Long.MIN_VALUE

    // Compute an approximation for how much time (in milliseconds) remains until the battery is fully charged.
    // Вычмсляет приблизительное время (в миллисекундах), оставшееся до полной зарядки аккумулятора.
    val chargeTimeRemaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        batteryManager.computeChargeTimeRemaining() / 1000
        batteryManager.computeChargeTimeRemaining() // В миллисекундах
    } else {
        TODO("VERSION.SDK_INT < P")
    }
    //log("Время зарядки: $chargeTimeRemaining")


    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus = context.registerReceiver(null, intentFilter)

    // Extra for Intent.ACTION_BATTERY_CHANGED: integer containing the current battery temperature.
    // Дополнительно за намерение.ACTION_BATTERY_CHANGED: целое число, содержащее текущую температуру батареи.
    val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
    //log("Температура: $temperature")

    // Extra for Intent.ACTION_BATTERY_CHANGED: integer containing the current battery voltage level.
    // Дополнительно за намерение.ACTION_BATTERY_CHANGED: целое число, содержащее текущий уровень напряжения батареи.
    val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
    //log("Температура: $voltage")

}
