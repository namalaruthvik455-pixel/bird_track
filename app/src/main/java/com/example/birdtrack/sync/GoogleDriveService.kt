package com.example.birdtrack.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.client.http.InputStreamContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Collections

class GoogleDriveService(context: Context) {
    private val driveService: Drive? by lazy {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = account.account
            }
            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("BirdTrack").build()
        } else {
            null
        }
    }

    suspend fun uploadFile(fileName: String, content: String, folderId: String? = null): String? = withContext(Dispatchers.IO) {
        val metadata = File().apply {
            name = fileName
            folderId?.let { parents = listOf(it) }
        }
        val contentStream = InputStreamContent("application/json", content.byteInputStream())
        
        try {
            val file = driveService?.files()?.create(metadata, contentStream)?.execute()
            file?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadFile(fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val outputStream = ByteArrayOutputStream()
            driveService?.files()?.get(fileId)?.executeMediaAndDownloadTo(outputStream)
            outputStream.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun findFileByName(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = driveService?.files()?.list()
                ?.setQ("name = '$fileName' and trashed = false")
                ?.setSpaces("drive")
                ?.execute()
            result?.files?.firstOrNull()?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
