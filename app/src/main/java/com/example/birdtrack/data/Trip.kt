package com.example.birdtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val date: String,
    val time: String,
    val location: String,
    val duration: String,
    val description: String? = null,
    val weatherCondition: String? = null // Additional field: weather during trip
)
