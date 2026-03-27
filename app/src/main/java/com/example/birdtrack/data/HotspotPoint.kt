package com.example.birdtrack.data

data class HotspotPoint(
    val species: String,
    val latitude: Double,
    val longitude: Double,
    val sightingCount: Int,
    val recentTimestamp: Long
)
