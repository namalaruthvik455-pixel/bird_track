package com.example.birdtrack.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class WeatherClient {
    suspend fun fetchWeather(latitude: Double?, longitude: Double?): WeatherSnapshot {
        if (latitude == null || longitude == null) return WeatherSnapshot(null, null)
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,wind_speed_10m"
                )
                val body = url.readText()
                val current = JSONObject(body).getJSONObject("current")
                WeatherSnapshot(
                    temperatureC = current.optDouble("temperature_2m"),
                    windSpeedKph = current.optDouble("wind_speed_10m")
                )
            }.getOrElse {
                WeatherSnapshot(null, null)
            }
        }
    }
}
