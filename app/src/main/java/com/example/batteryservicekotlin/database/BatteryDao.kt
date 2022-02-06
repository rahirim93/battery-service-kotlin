package com.example.batteryservicekotlin.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BatteryDao {

    @Query("SELECT * FROM unit")
    fun getUnits(): LiveData<List<Unit>>

    @Insert
    fun addUnit(unit: Unit)
}