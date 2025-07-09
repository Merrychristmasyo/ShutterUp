package com.example.shutterup.repository
import android.content.Context
import com.example.shutterup.model.PhotoSpot
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
class PhotoSpotRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jsonFileName = "photospot.json"
    private val _cachedPhotoSpots = MutableStateFlow<List<PhotoSpot>?>(null)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            val loadedSpots = loadPhotoSpotsFromFile()
            _cachedPhotoSpots.value = loadedSpots
            android.util.Log.d("PhotoSpotRepository", "Photo spots loaded and cached successfully.")
        }
    }

    private suspend fun loadPhotoSpotsFromFile(): List<PhotoSpot> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 먼저 internal storage에서 실제 업로드된 데이터 로드
                val internalJsonString = fileManager.loadJsonData(jsonFileName)
                val internalData = if (internalJsonString != null) {
                    try {
                        json.decodeFromString<List<PhotoSpot>>(internalJsonString)
                    } catch (e: Exception) {
                        android.util.Log.w("PhotoSpotRepository", "Error parsing internal data: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // 2. Assets에서 초기 더미 데이터 로드
                val assetsData = try {
                    val assetsJsonString = context.assets.open(jsonFileName).bufferedReader().use { it.readText() }
                    json.decodeFromString<List<PhotoSpot>>(assetsJsonString)
                } catch (e: Exception) {
                    android.util.Log.w("PhotoSpotRepository", "Error loading assets data: ${e.message}")
                    emptyList()
                }

                // 3. 두 데이터를 합치되, 중복 ID는 internal 데이터를 우선으로 함
                val combinedData = mutableMapOf<String, PhotoSpot>()
                
                // 먼저 assets 데이터를 추가
                assetsData.forEach { spot ->
                    combinedData[spot.id] = spot
                }
                
                // 그 다음 internal 데이터를 추가 (중복 ID가 있으면 덮어씀)
                internalData.forEach { spot ->
                    combinedData[spot.id] = spot
                }

                val finalData = combinedData.values.toList()
                android.util.Log.d("PhotoSpotRepository", "Loaded ${finalData.size} photo spots (assets: ${assetsData.size}, internal: ${internalData.size})")
                
                finalData
            } catch (e: Exception) {
                android.util.Log.e("PhotoSpotRepository", "Error loading photo spots: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun getPhotoSpotList(): List<PhotoSpot> {
        return _cachedPhotoSpots.first { it != null } ?: emptyList()
    }

    suspend fun getPhotoSpotById(id: String): PhotoSpot? {
        val photoSpots = _cachedPhotoSpots.first { it != null } ?: return null
        return photoSpots.find { it.id == id }
    }

    suspend fun addPhotoSpot(photoSpot: PhotoSpot): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentSpots = _cachedPhotoSpots.first { it != null } ?: emptyList()
                
                if (currentSpots.any { it.id == photoSpot.id }) {
                    android.util.Log.d("PhotoSpotRepository", "Photo spot with ID ${photoSpot.id} already exists")
                    return@withContext false
                }
                
                val updatedSpots = currentSpots + photoSpot
                _cachedPhotoSpots.value = updatedSpots
                
                savePhotoSpotsToFile(updatedSpots)
                
                android.util.Log.d("PhotoSpotRepository", "Photo spot ${photoSpot.name} added successfully. Total spots: ${updatedSpots.size}")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoSpotRepository", "Error adding photo spot: ${e.message}", e)
                false
            }
        }
    }

    suspend fun updatePhotoSpot(photoSpot: PhotoSpot): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentSpots = _cachedPhotoSpots.first { it != null } ?: emptyList()
                val spotIndex = currentSpots.indexOfFirst { it.id == photoSpot.id }
                
                if (spotIndex == -1) {
                    android.util.Log.d("PhotoSpotRepository", "Photo spot with ID ${photoSpot.id} not found")
                    return@withContext false
                }
                
                val updatedSpots = currentSpots.toMutableList()
                updatedSpots[spotIndex] = photoSpot
                _cachedPhotoSpots.value = updatedSpots
                
                savePhotoSpotsToFile(updatedSpots)
                
                android.util.Log.d("PhotoSpotRepository", "Photo spot ${photoSpot.name} updated successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoSpotRepository", "Error updating photo spot: ${e.message}", e)
                false
            }
        }
    }

    suspend fun deletePhotoSpot(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentSpots = _cachedPhotoSpots.first { it != null } ?: emptyList()
                val spotToDelete = currentSpots.find { it.id == id }
                
                if (spotToDelete == null) {
                    android.util.Log.d("PhotoSpotRepository", "Photo spot with ID $id not found")
                    return@withContext false
                }
                
                val updatedSpots = currentSpots.filter { it.id != id }
                _cachedPhotoSpots.value = updatedSpots
                
                savePhotoSpotsToFile(updatedSpots)
                
                android.util.Log.d("PhotoSpotRepository", "Photo spot ${spotToDelete.name} deleted successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoSpotRepository", "Error deleting photo spot: ${e.message}", e)
                false
            }
        }
    }

    private suspend fun savePhotoSpotsToFile(photoSpots: List<PhotoSpot>) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(ListSerializer(PhotoSpot.serializer()), photoSpots)
                fileManager.saveJsonData(jsonFileName, jsonString)
                android.util.Log.d("PhotoSpotRepository", "Photo spots saved to file successfully")
            } catch (e: Exception) {
                android.util.Log.e("PhotoSpotRepository", "Error saving photo spots to file: ${e.message}", e)
            }
        }
    }
}