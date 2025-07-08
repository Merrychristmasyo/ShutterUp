package com.example.shutterup.repository

import android.content.Context
import androidx.annotation.DrawableRes
import com.example.shutterup.R
import com.example.shutterup.model.PhotoDetail
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.model.PhotoSpot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoMetadataRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jsonFilePath = "photometadata.json"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _cachedPhotoMetadata = MutableStateFlow<List<PhotoMetadata>?>(null)

    init {
        repositoryScope.launch {
            val loadedPhotoMetadata = loadPhotoMetadataFromJsonInternal()
            _cachedPhotoMetadata.value = loadedPhotoMetadata
            println("Photo metadata loaded and cached successfully.")
        }
    }

    private suspend fun loadPhotoMetadataFromJsonInternal(): List<PhotoMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open(jsonFilePath).bufferedReader().use { it.readText() }
                json.decodeFromString<List<PhotoMetadata>>(jsonString)
            } catch (e: IOException) {
                e.printStackTrace()
                println("Error reading $jsonFilePath: ${e.message}")
                emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error parsing $jsonFilePath: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun getPhotoMetadataById(photoId: String): PhotoMetadata? {
        val photoMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
        return photoMetadata.find { it.id == photoId }
    }

    suspend fun getAllPhotoMetadata(): List<PhotoMetadata> {
        val photoMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
        return photoMetadata
    }

    suspend fun getPhotoMetadataListByPhotoSpotId(photoSpotId: String): List<PhotoMetadata> {
        val photoMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
        return photoMetadata.filter { it.photoSpotId == photoSpotId }
    }

    suspend fun getThumbnailPhotoMetadataList(): HashMap<String, PhotoMetadata> {
        val photoMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
        val thumbnailMap = hashMapOf<String, PhotoMetadata>()
        
        // 각 포토스팟의 첫 번째 사진을 찾아서 맵에 추가
        photoMetadata.groupBy { it.photoSpotId }
            .forEach { (spotId, photos) ->
                photos.firstOrNull()?.let { firstPhoto ->
                    thumbnailMap[spotId] = firstPhoto
                }
            }
        
        return thumbnailMap
    }
}
