package com.example.birdtrack.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.birdtrack.data.AchievementBadge
import com.example.birdtrack.data.BirdRepository
import com.example.birdtrack.data.HotspotPoint
import com.example.birdtrack.data.Sighting
import com.example.birdtrack.data.SpeciesIdentifier
import com.example.birdtrack.data.Trip
import com.example.birdtrack.data.WeatherClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class BirdViewModel(private val repository: BirdRepository, context: Context) : ViewModel() {

    private val speciesIdentifier = SpeciesIdentifier(context)
    private val weatherClient = WeatherClient()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val allTrips: StateFlow<List<Trip>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _migratoryFilter = MutableStateFlow("All")
    val migratoryFilter = _migratoryFilter.asStateFlow()
    private val _conservationFilter = MutableStateFlow("All")
    val conservationFilter = _conservationFilter.asStateFlow()
    private val _songTypeFilter = MutableStateFlow("All")
    val songTypeFilter = _songTypeFilter.asStateFlow()

    val hotspots: StateFlow<List<HotspotPoint>> = repository.hotspots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<AchievementBadge>> = repository.allSightings
        .map { sightings -> computeBadges(sightings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lifeListCount: StateFlow<Int> = repository.allSightings
        .map { sightings -> sightings.map { it.species }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Sighting>> = combine(
        _searchQuery,
        _migratoryFilter,
        _conservationFilter,
        _songTypeFilter
    ) { query, migratory, conservation, songType ->
        Quadruple(query, migratory, conservation, songType)
    }.flatMapLatest { state ->
        repository.allSightings.map { sightings ->
            sightings.filter { sighting ->
                val queryMatch = state.first.isBlank() || sighting.species.contains(state.first, ignoreCase = true)
                val migratoryMatch = state.second == "All" || sighting.migratoryStatus == state.second
                val conservationMatch = state.third == "All" || sighting.conservationStatus == state.third
                val songMatch = state.fourth == "All" || sighting.songType == state.fourth
                queryMatch && migratoryMatch && conservationMatch && songMatch
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateMigratoryFilter(value: String) {
        _migratoryFilter.value = value
    }

    fun updateConservationFilter(value: String) {
        _conservationFilter.value = value
    }

    fun updateSongTypeFilter(value: String) {
        _songTypeFilter.value = value
    }

    fun addTrip(
        name: String,
        date: String,
        time: String,
        location: String,
        duration: String,
        description: String?,
        weatherCondition: String?,
        onResult: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.insertTrip(
                Trip(
                    name = name,
                    date = date,
                    time = time,
                    location = location,
                    duration = duration,
                    description = description,
                    weatherCondition = weatherCondition
                )
            )
            onResult(id)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    fun getSpeciesSuggestion(imageUri: android.net.Uri?): String? {
        return speciesIdentifier.suggestSpecies(imageUri)
    }

    @SuppressLint("MissingPermission")
    fun addSighting(
        tripId: Long,
        species: String,
        location: String,
        quantity: Int,
        comments: String?,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val deviceLocation = try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            } catch (e: Exception) {
                null
            }
            
            val weather = weatherClient.fetchWeather(deviceLocation?.latitude, deviceLocation?.longitude)
            
            repository.insertSighting(
                Sighting(
                    tripId = tripId,
                    species = species,
                    location = location,
                    quantity = quantity,
                    comments = comments,
                    imageUri = imageUri,
                    latitude = deviceLocation?.latitude,
                    longitude = deviceLocation?.longitude,
                    temperatureC = weather.temperatureC,
                    windSpeedKph = weather.windSpeedKph
                )
            )
        }
    }

    fun updateSighting(sighting: Sighting) {
        viewModelScope.launch {
            repository.insertSighting(sighting)
        }
    }

    fun deleteSighting(sighting: Sighting) {
        viewModelScope.launch {
            repository.deleteSighting(sighting)
        }
    }

    fun getSightingsForTrip(tripId: Long): Flow<List<Sighting>> {
        return repository.getSightingsForTrip(tripId)
    }

    private fun computeBadges(sightings: List<Sighting>): List<AchievementBadge> {
        val results = mutableListOf<AchievementBadge>()
        if (sightings.any { isEarlyBird(it.timestamp) }) {
            results.add(AchievementBadge("Early Bird", "Logged a sighting around 5 AM"))
        }
        val uniqueSpecies = sightings.map { it.species }.distinct().size
        if (uniqueSpecies >= 10) {
            results.add(AchievementBadge("Life List Builder", "Logged 10 unique species"))
        }
        if (sightings.count { it.conservationStatus == "Endangered" } >= 3) {
            results.add(AchievementBadge("Conservation Ally", "Recorded 3 endangered species"))
        }
        if (sightings.size >= 25) {
            results.add(AchievementBadge("Field Naturalist", "Logged 25 sightings"))
        }
        return results
    }

    private fun isEarlyBird(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.HOUR_OF_DAY) == 5
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

class BirdViewModelFactory(private val repository: BirdRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BirdViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BirdViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
