package com.example.batteryservicekotlin.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BatteryDao {

    @Query("SELECT * FROM unit")
    fun getUnits(): LiveData<List<Unit>>

    // Запрос выборки записанной в течение выбранных суток
    //startDay - начало дня, endDay - конец дня
    @Query("SELECT * FROM unit WHERE date BETWEEN :startDay AND :endDay")
    fun getChosenDayUnits(startDay: Int, endDay: Int): LiveData<List<Unit>>

    @Insert
    fun addUnit(unit: Unit)
}