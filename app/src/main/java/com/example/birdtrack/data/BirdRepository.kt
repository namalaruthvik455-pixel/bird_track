package com.example.birdtrack.data

import kotlinx.coroutines.flow.Flow

class BirdRepository(private val birdDao: BirdDao) {
    val allTrips: Flow<List<Trip>> = birdDao.getAllTrips()
    val allSightings: Flow<List<Sighting>> = birdDao.getAllSightings()
    val hotspots: Flow<List<HotspotPoint>> = birdDao.getHotspots()

    suspend fun insertTrip(trip: Trip): Long {
        return birdDao.insertTrip(trip)
    }

    suspend fun deleteTrip(trip: Trip) {
        birdDao.deleteTrip(trip)
    }

    fun getSightingsForTrip(tripId: Long): Flow<List<Sighting>> {
        return birdDao.getSightingsForTrip(tripId)
    }

    suspend fun insertSighting(sighting: Sighting) {
        birdDao.insertSighting(sighting)
    }

    suspend fun deleteSighting(sighting: Sighting) {
        birdDao.deleteSighting(sighting)
    }

    fun searchSightings(query: String): Flow<List<Sighting>> {
        return birdDao.searchSightings("%$query%")
    }
}
