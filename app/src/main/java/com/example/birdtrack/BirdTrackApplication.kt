package com.example.birdtrack

import android.app.Application
import androidx.work.*
import com.example.birdtrack.sync.SyncWorker
import java.util.concurrent.TimeUnit

class BirdTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupBackgroundSync()
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Only on Wi-Fi
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BirdTrackSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
