package com.example.birdtrack.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.birdtrack.data.BirdDatabase
import com.example.birdtrack.data.Sighting
import com.example.birdtrack.data.Trip
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val driveService = GoogleDriveService(context)
    private val database = BirdDatabase.getDatabase(context)
    private val birdDao = database.birdDao()
    private val gson = Gson()

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting background sync with Google Drive...")
        
        val folderId = "1igF6Sh79Daw_ADZC7frNLda-3SEQd5WR"
        
        try {
            // 1. Download existing data from Drive
            val fileId = driveService.findFileByName("birdtrack_data.json")
            if (fileId != null) {
                val remoteJson = driveService.downloadFile(fileId)
                if (remoteJson != null) {
                    val syncDataType = object : TypeToken<SyncData>() {}.type
                    val remoteData = gson.fromJson<SyncData>(remoteJson, syncDataType)
                    mergeData(remoteData)
                }
            }

            // 2. Prepare local data for upload
            val allTrips = birdDao.getAllTrips().first()
            val allSightings = birdDao.getAllSightings().first()
            val syncData = SyncData(allTrips, allSightings)
            val localJson = gson.toJson(syncData)

            // 3. Upload updated data back to Drive
            driveService.uploadFile("birdtrack_data.json", localJson, folderId)

            Log.d("SyncWorker", "Sync completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            return Result.retry()
        }
    }

    private suspend fun mergeData(remoteData: SyncData) {
        remoteData.trips.forEach { birdDao.insertTrip(it) }
        remoteData.sightings.forEach { birdDao.insertSighting(it) }
    }

    data class SyncData(
        val trips: List<Trip>,
        val sightings: List<Sighting>
    )
}
