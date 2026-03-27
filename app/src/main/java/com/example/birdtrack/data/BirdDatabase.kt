package com.example.birdtrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Trip::class, Sighting::class], version = 3, exportSchema = false)
abstract class BirdDatabase : RoomDatabase() {

    abstract fun birdDao(): BirdDao

    companion object {
        @Volatile
        private var Instance: BirdDatabase? = null

        fun getDatabase(context: Context): BirdDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, BirdDatabase::class.java, "bird_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
