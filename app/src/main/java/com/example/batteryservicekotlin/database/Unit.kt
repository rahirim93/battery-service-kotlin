package com.example.batteryservicekotlin.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Unit(@PrimaryKey val id: UUID = UUID.randomUUID(),
                var date: Date = Date(),
                var capacityInMicroampereHours: Int = 0,
                var capacityInPercentage: Int = 0,
                var currentAverage: Int = 0,
                var currentNow: Int = 0,
                var temperature: Int? = 0,
                var voltage: Int? = 0)