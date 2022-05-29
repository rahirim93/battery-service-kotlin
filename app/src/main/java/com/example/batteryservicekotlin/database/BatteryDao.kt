package com.example.batteryservicekotlin.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.util.*

@Dao
interface BatteryDao {

    @Query("SELECT * FROM unit")
    fun getUnits(): LiveData<List<Unit>>

    // Запрос выборки записанной в течение выбранных суток
    //startDay - начало дня, endDay - конец дня
    @Query("SELECT * FROM unit WHERE date BETWEEN :startDay AND :endDay")
    fun getChosenDayUnits(startDay: Long, endDay: Long): LiveData<List<Unit>>

    @Query("SELECT date FROM unit")
    fun getDates(): LiveData<List<Date>>

    @Insert
    fun addUnit(unit: Unit)
}