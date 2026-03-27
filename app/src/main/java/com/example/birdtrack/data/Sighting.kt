package com.example.birdtrack.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sightings",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"])]
)
data class Sighting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val species: String,
    val location: String, // Required field
    val quantity: Int, // Required field
    val comments: String? = null, // Optional field
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val temperatureC: Double? = null,
    val windSpeedKph: Double? = null,
    val migratoryStatus: String = "Unknown",
    val conservationStatus: String = "Common",
    val songType: String = "General",
    val category: String = "Songbirds",
    val aiSuggestedSpecies: String? = null
)
