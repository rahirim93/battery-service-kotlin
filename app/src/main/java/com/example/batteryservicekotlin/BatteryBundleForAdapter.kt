package com.example.batteryservicekotlin

import com.example.batteryservicekotlin.database.Unit
import java.util.*

/** Класс для адаптера утилизатора.
 * Вмещает в себе дату и количество записей в БД в этот день
 * Передаётся в адаптер утилизатора */

data class BatteryBundleForAdapter (var date: Date,
                                    var insertsDay: List<Unit>)