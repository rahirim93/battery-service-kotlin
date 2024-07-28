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
    suspend fun getUnits2(): List<Unit> = batteryDao.getUnits2()

    suspend fun getLimit(offset: Int): List<Unit> = batteryDao.getLimit(offset)

    suspend fun getCount(): Int = batteryDao.getCount()

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

    // Работа с SharedPreferences
    private val sharedPreferences = context.getSharedPreferences("shared_preferences_battery_service", Context.MODE_PRIVATE)
    // Получение состояние настройки автозапуска сервиса при запуске приложения.
    fun getAutostartService(): Boolean = sharedPreferences.getBoolean("autoStartService", false)
    // Сохранение состояние настройки автозапуска сервиса при запуске приложения.
    fun setAutostartService(isOn: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean("autoStartService", isOn)
            apply()
        }
    }
    fun getTestRestartService(): Boolean = sharedPreferences.getBoolean("testRestartService", false)
    fun setTestRestartService(isOn: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean("testRestartService", isOn)
            apply()
        }
    }

    // Состояние шага регулировки диапазона
    fun getStepRange(): Float = sharedPreferences.getFloat("stepRangeBattery", 0.2F)
    fun setStepRange(step: Float) {
        sharedPreferences.edit().apply {
            putFloat("stepRangeBattery", step)
            apply()
        }
    }

    // Режим сдвоенного аккумулятора (ток умножается на 2)
    fun getDoubleBattery(): Boolean = sharedPreferences.getBoolean("doubleBattery", false)
    fun setDoubleBattery(doubleBattery: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean("doubleBattery", doubleBattery)
            apply()
        }
    }

    /** У Сереги почему-то на телефоне ток отображается наоборот, ток заряда отрицательный
     * ток разряда положительный. Эта настройка будет менять полярность тока отображаемого на графике */
    fun getInversionCurrent(): Boolean = sharedPreferences.getBoolean("inversionCurrent", false)
    fun setInversionCurrent(inversion: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean("inversionCurrent", inversion)
            apply()
        }
    }

    /** У Сереги на Infinix (либо это связано с Android 13) ток система выдает в миллиамперах.
     * Это настройка корректирует это. */
    fun getCurrentCorrect(): Boolean = sharedPreferences.getBoolean("currentCorrect", false)
    fun setCurrentCorrect(correct: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean("currentCorrect", correct)
            apply()
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