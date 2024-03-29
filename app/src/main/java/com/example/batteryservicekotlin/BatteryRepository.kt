package com.example.batteryservicekotlin

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.batteryservicekotlin.database.BatteryDatabase
import com.example.batteryservicekotlin.database.Unit
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "battery_database"

class BatteryRepository private constructor(context: Context) {

    private val database : BatteryDatabase = Room.databaseBuilder(
        context.applicationContext,
        BatteryDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val batteryDao = database.batteryDao()

    private val executor = Executors.newSingleThreadExecutor()

    fun getUnits(): LiveData<List<Unit>> = batteryDao.getUnits()

    fun getDates(): LiveData<List<Date>> = batteryDao.getDates()

    // Выборка данных в выбранный день
    fun getChosenDayUnits(startDay: Long, endDay: Long): LiveData<List<Unit>> =
        batteryDao.getChosenDayUnits(startDay, endDay)

    suspend fun getChosenDayUnitsList(startDay: Long, endDay: Long): List<Unit> =
        batteryDao.getChosenDayUnitsList(startDay, endDay)

    fun addUnit(unit: Unit) {
        executor.execute {
            batteryDao.addUnit(unit)
        }
    }

    companion object {
        private var INSTANCE: BatteryRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = BatteryRepository(context)
            }
        }

        fun get(): BatteryRepository {
            return INSTANCE ?:
            throw IllegalStateException("BatteryRepository must be initialized")
        }
    }
}