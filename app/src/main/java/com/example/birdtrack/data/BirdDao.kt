package com.example.birdtrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BirdDao {
    // Trips
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Query("SELECT * FROM trips ORDER BY date DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Delete
    suspend fun deleteTrip(trip: Trip)

    // Sightings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSighting(sighting: Sighting)

    @Query("SELECT * FROM sightings WHERE tripId = :tripId")
    fun getSightingsForTrip(tripId: Long): Flow<List<Sighting>>

    @Query("SELECT * FROM sightings WHERE species LIKE :searchQuery")
    fun searchSightings(searchQuery: String): Flow<List<Sighting>>

    @Query("SELECT * FROM sightings ORDER BY timestamp DESC")
    fun getAllSightings(): Flow<List<Sighting>>

    @Delete
    suspend fun deleteSighting(sighting: Sighting)

    @Query(
        """
        SELECT 
            species,
            latitude,
            longitude,
            COUNT(*) as sightingCount,
            MAX(timestamp) as recentTimestamp
        FROM sightings
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        GROUP BY species, latitude, longitude
        ORDER BY recentTimestamp DESC
        """
    )
    fun getHotspots(): Flow<List<HotspotPoint>>
}
