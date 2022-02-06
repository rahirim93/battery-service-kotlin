package com.example.batteryservicekotlin.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Unit(@PrimaryKey val id: UUID = UUID.randomUUID(),
                var date: Date = Date(),
                var capacity: Int = 0)