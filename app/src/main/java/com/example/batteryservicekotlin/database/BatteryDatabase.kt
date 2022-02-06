package com.example.batteryservicekotlin.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [ Unit::class ], version = 1)
@TypeConverters(BatteryTypeConverters::class)
abstract class BatteryDatabase : RoomDatabase() {

    abstract fun batteryDao(): BatteryDao

}