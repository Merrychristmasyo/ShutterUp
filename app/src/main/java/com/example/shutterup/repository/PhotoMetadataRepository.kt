package com.example.shutterup.repository

import android.content.Context
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.utils.FileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoMetadataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jsonFileName = "photometadata.json"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _cachedPhotoMetadata = MutableStateFlow<List<PhotoMetadata>?>(null)

    init {
        repositoryScope.launch {
            val loadedPhotoMetadata = loadPhotoMetadataFromFile()
            _cachedPhotoMetadata.value = loadedPhotoMetadata
            android.util.Log.d("PhotoMetadataRepository", "Photo metadata loaded and cached successfully.")
        }
    }

    private suspend fun loadPhotoMetadataFromFile(): List<PhotoMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 먼저 internal storage에서 실제 업로드된 데이터 로드
                val internalJsonString = fileManager.loadJsonData(jsonFileName)
                val internalData = if (internalJsonString != null) {
                    try {
                        json.decodeFromString<List<PhotoMetadata>>(internalJsonString)
                    } catch (e: Exception) {
                        android.util.Log.w("PhotoMetadataRepository", "Error parsing internal data: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // 2. Assets에서 초기 더미 데이터 로드 (internal storage에 데이터가 없을 때만)
                val assetsData = if (internalData.isEmpty()) {
                    try {
                        val assetsJsonString = context.assets.open(jsonFileName).bufferedReader().use { it.readText() }
                        json.decodeFromString<List<PhotoMetadata>>(assetsJsonString)
                    } catch (e: Exception) {
                        android.util.Log.w("PhotoMetadataRepository", "Error loading assets data: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // 3. 실제 데이터 우선, 없으면 더미 데이터 사용
                val finalData = if (internalData.isNotEmpty()) internalData else assetsData
                
                android.util.Log.d("PhotoMetadataRepository", "Loaded ${finalData.size} photo metadata entries")
                finalData.forEach { metadata ->
                    android.util.Log.d("PhotoMetadataRepository", "Photo: ${metadata.filename}, userId: ${metadata.userId}")
                }
                
                finalData
            } catch (e: Exception) {
                android.util.Log.e("PhotoMetadataRepository", "Error loading photo metadata: ${e.message}", e)
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

    suspend fun getPhotoMetadataListByPhotoSpotId(photoSpotId: String?): List<PhotoMetadata> {
        val photoMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
        return photoMetadata.filter { it.photoSpotId == photoSpotId }
    }

    // 예시: userId로 사진 메타데이터 조회
    suspend fun getPhotoMetadataListByUserId(userId: String): List<PhotoMetadata> {
        val photoMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
        return withContext(Dispatchers.IO) {
            photoMetadata
                .filter { it.userId == userId }
            //.flatMap { it.photos }  // Profile 안에 photos 프로퍼티가 있다고 가정
        }
    }

    suspend fun getThumbnailPhotoMetadataList(): HashMap<String, PhotoMetadata> {
        val photoMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
        val thumbnailMap = hashMapOf<String, PhotoMetadata>()
        
        // 각 포토스팟의 첫 번째 사진을 찾아서 맵에 추가 (photoSpotId가 null이 아닌 경우만)
        photoMetadata.filter { it.photoSpotId != null }
            .groupBy { it.photoSpotId!! } // null이 아님을 확인했으므로 !! 사용
            .forEach { (spotId, photos) ->
                photos.firstOrNull()?.let { firstPhoto ->
                    thumbnailMap[spotId] = firstPhoto
                }
            }
        
        return thumbnailMap
    }

    suspend fun addPhotoMetadata(photoMetadata: PhotoMetadata): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
                
                if (currentMetadata.any { it.id == photoMetadata.id }) {
                    android.util.Log.d("PhotoMetadataRepository", "Photo metadata with ID ${photoMetadata.id} already exists")
                    return@withContext false
                }
                
                val updatedMetadata = currentMetadata + photoMetadata
                _cachedPhotoMetadata.value = updatedMetadata
                
                savePhotoMetadataToFile(updatedMetadata)
                
                android.util.Log.d("PhotoMetadataRepository", "Photo metadata ${photoMetadata.id} added successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoMetadataRepository", "Error adding photo metadata: ${e.message}", e)
                false
            }
        }
    }

    suspend fun updatePhotoMetadata(photoMetadata: PhotoMetadata): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
                val metadataIndex = currentMetadata.indexOfFirst { it.id == photoMetadata.id }
                
                if (metadataIndex == -1) {
                    android.util.Log.d("PhotoMetadataRepository", "Photo metadata with ID ${photoMetadata.id} not found")
                    return@withContext false
                }
                
                val updatedMetadata = currentMetadata.toMutableList()
                updatedMetadata[metadataIndex] = photoMetadata
                _cachedPhotoMetadata.value = updatedMetadata
                
                savePhotoMetadataToFile(updatedMetadata)
                
                android.util.Log.d("PhotoMetadataRepository", "Photo metadata ${photoMetadata.id} updated successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoMetadataRepository", "Error updating photo metadata: ${e.message}", e)
                false
            }
        }
    }

    suspend fun deletePhotoMetadata(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentMetadata = _cachedPhotoMetadata.first { it != null } ?: emptyList()
                val metadataToDelete = currentMetadata.find { it.id == id }
                
                if (metadataToDelete == null) {
                    android.util.Log.d("PhotoMetadataRepository", "Photo metadata with ID $id not found")
                    return@withContext false
                }
                
                val updatedMetadata = currentMetadata.filter { it.id != id }
                _cachedPhotoMetadata.value = updatedMetadata
                
                savePhotoMetadataToFile(updatedMetadata)
                
                android.util.Log.d("PhotoMetadataRepository", "Photo metadata ${metadataToDelete.id} deleted successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoMetadataRepository", "Error deleting photo metadata: ${e.message}", e)
                false
            }
        }
    }

    private suspend fun savePhotoMetadataToFile(photoMetadata: List<PhotoMetadata>) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(ListSerializer(PhotoMetadata.serializer()), photoMetadata)
                fileManager.saveJsonData(jsonFileName, jsonString)
                android.util.Log.d("PhotoMetadataRepository", "Photo metadata saved to file successfully")
            } catch (e: Exception) {
                android.util.Log.e("PhotoMetadataRepository", "Error saving photo metadata to file: ${e.message}", e)
            }
        }
    }
}
