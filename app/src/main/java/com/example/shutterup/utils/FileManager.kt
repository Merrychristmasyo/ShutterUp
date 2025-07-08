package com.example.shutterup.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FileManager"
        private const val IMAGES_DIR = "images"
        private const val DATA_DIR = "data"
        private const val THUMBNAIL_DIR = "thumbnails"
    }

    private val imagesDir: File by lazy {
        File(context.filesDir, IMAGES_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    private val dataDir: File by lazy {
        File(context.filesDir, DATA_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbnailsDir: File by lazy {
        File(context.filesDir, THUMBNAIL_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 이미지 파일을 저장합니다
     */
    suspend fun saveImage(uri: Uri, filename: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val outputFile = File(imagesDir, filename)
                
                inputStream?.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "Image saved successfully: ${outputFile.absolutePath}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image: ${e.message}", e)
                false
            }
        }
    }

    /**
     * 이미지 파일을 삭제합니다
     */
    suspend fun deleteImage(filename: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(imagesDir, filename)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "Image deleted: $filename, success: $deleted")
                    deleted
                } else {
                    Log.w(TAG, "Image file not found: $filename")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image: ${e.message}", e)
                false
            }
        }
    }

    /**
     * 이미지 파일의 URI를 반환합니다
     */
    fun getImageUri(filename: String): Uri? {
        val file = File(imagesDir, filename)
        return if (file.exists()) {
            Uri.fromFile(file)
        } else {
            Log.w(TAG, "Image file not found: $filename")
            null
        }
    }

    /**
     * 썸네일을 생성하고 저장합니다
     */
    suspend fun createAndSaveThumbnail(uri: Uri, filename: String, maxSize: Int = 300): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                if (bitmap != null) {
                    val thumbnail = createThumbnail(bitmap, maxSize)
                    val thumbnailFile = File(thumbnailsDir, "thumb_$filename")
                    
                    FileOutputStream(thumbnailFile).use { out ->
                        thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    
                    bitmap.recycle()
                    thumbnail.recycle()
                    
                    Log.d(TAG, "Thumbnail created successfully: ${thumbnailFile.absolutePath}")
                    true
                } else {
                    Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating thumbnail: ${e.message}", e)
                false
            }
        }
    }

    /**
     * 썸네일 URI를 반환합니다
     */
    fun getThumbnailUri(filename: String): Uri? {
        val file = File(thumbnailsDir, "thumb_$filename")
        return if (file.exists()) {
            Uri.fromFile(file)
        } else {
            Log.w(TAG, "Thumbnail file not found: $filename")
            null
        }
    }

    /**
     * JSON 데이터를 파일에 저장합니다
     */
    suspend fun saveJsonData(filename: String, jsonString: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(dataDir, filename)
                FileOutputStream(file).use { output ->
                    output.write(jsonString.toByteArray())
                }
                Log.d(TAG, "JSON data saved successfully: $filename")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving JSON data: ${e.message}", e)
                false
            }
        }
    }

    /**
     * JSON 데이터를 파일에서 읽어옵니다
     */
    suspend fun loadJsonData(filename: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(dataDir, filename)
                if (file.exists()) {
                    val jsonString = file.readText()
                    Log.d(TAG, "JSON data loaded successfully: $filename")
                    jsonString
                } else {
                    Log.w(TAG, "JSON file not found: $filename")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading JSON data: ${e.message}", e)
                null
            }
        }
    }

    /**
     * 파일이 존재하는지 확인합니다
     */
    fun fileExists(filename: String): Boolean {
        val file = File(imagesDir, filename)
        return file.exists()
    }

    /**
     * 모든 이미지 파일 목록을 반환합니다
     */
    fun getAllImageFiles(): List<String> {
        return imagesDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif") }
            ?.map { it.name }
            ?: emptyList()
    }

    private fun createThumbnail(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
} 