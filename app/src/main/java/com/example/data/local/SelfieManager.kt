package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedSelfie(
    val file: File,
    val name: String,
    val formattedDate: String,
    val sizeBytes: Long
)

class SelfieManager(private val context: Context) {
    private val selfiesDir: File
        get() {
            val dir = File(context.filesDir, "private_selfies")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    suspend fun saveBitmap(bitmap: Bitmap): Result<SavedSelfie> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "selfie_$timestamp.jpg"
            val file = File(selfiesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Result.success(
                SavedSelfie(
                    file = file,
                    name = filename,
                    formattedDate = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date()),
                    sizeBytes = file.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveFromUri(uri: Uri): Result<SavedSelfie> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open image stream"))
            val bitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext Result.failure(Exception("Failed to decode image"))
            saveBitmap(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedSelfies(): List<SavedSelfie> = withContext(Dispatchers.IO) {
        val files = selfiesDir.listFiles { file -> file.extension.lowercase() == "jpg" } ?: emptyArray()
        files.sortedByDescending { it.lastModified() }.map { file ->
            val dateStr = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(file.lastModified()))
            SavedSelfie(
                file = file,
                name = file.name,
                formattedDate = dateStr,
                sizeBytes = file.length()
            )
        }
    }

    suspend fun deleteSelfie(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (file.exists() && file.parentFile == selfiesDir) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
