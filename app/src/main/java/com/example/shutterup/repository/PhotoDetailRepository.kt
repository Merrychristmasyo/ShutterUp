package com.example.shutterup.repository
import android.content.Context
import com.example.shutterup.model.PhotoDetail
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
class PhotoDetailRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jsonFileName = "photodetail.json"
    private val _cachedPhotoDetails = MutableStateFlow<List<PhotoDetail>?>(null)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            val loadedDetails = loadPhotoDetailsFromFile()
            _cachedPhotoDetails.value = loadedDetails
            android.util.Log.d("PhotoDetailRepository", "Photo details loaded and cached successfully.")
        }
    }

    private suspend fun loadPhotoDetailsFromFile(): List<PhotoDetail> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 먼저 internal storage에서 실제 업로드된 데이터 로드
                val internalJsonString = fileManager.loadJsonData(jsonFileName)
                val internalData = if (internalJsonString != null) {
                    try {
                        json.decodeFromString<List<PhotoDetail>>(internalJsonString)
                    } catch (e: Exception) {
                        android.util.Log.w("PhotoDetailRepository", "Error parsing internal data: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // 2. Assets에서 초기 더미 데이터 로드
                val assetsData = try {
                    val assetsJsonString = context.assets.open(jsonFileName).bufferedReader().use { it.readText() }
                    json.decodeFromString<List<PhotoDetail>>(assetsJsonString)
                } catch (e: Exception) {
                    android.util.Log.w("PhotoDetailRepository", "Error loading assets data: ${e.message}")
                    emptyList()
                }

                // 3. 두 데이터를 합치되, 중복 ID는 internal 데이터를 우선으로 함
                val combinedData = mutableMapOf<String, PhotoDetail>()
                
                // 먼저 assets 데이터를 추가
                assetsData.forEach { detail ->
                    combinedData[detail.id] = detail
                }
                
                // 그 다음 internal 데이터를 추가 (중복 ID가 있으면 덮어씀)
                internalData.forEach { detail ->
                    combinedData[detail.id] = detail
                }

                val finalData = combinedData.values.toList()
                android.util.Log.d("PhotoDetailRepository", "Loaded ${finalData.size} photo details (assets: ${assetsData.size}, internal: ${internalData.size})")
                
                finalData
            } catch (e: Exception) {
                android.util.Log.e("PhotoDetailRepository", "Error loading photo details: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun getPhotoDetailList(): List<PhotoDetail> {
        return _cachedPhotoDetails.first { it != null } ?: emptyList()
    }

    suspend fun getPhotoDetailById(id: String): PhotoDetail? {
        val photoDetails = _cachedPhotoDetails.first { it != null } ?: return null
        return photoDetails.find { it.id == id }
    }

    suspend fun addPhotoDetail(photoDetail: PhotoDetail): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentDetails = _cachedPhotoDetails.first { it != null } ?: emptyList()
                
                if (currentDetails.any { it.id == photoDetail.id }) {
                    android.util.Log.d("PhotoDetailRepository", "Photo detail with ID ${photoDetail.id} already exists")
                    return@withContext false
                }
                
                val updatedDetails = currentDetails + photoDetail
                _cachedPhotoDetails.value = updatedDetails
                
                savePhotoDetailsToFile(updatedDetails)
                
                android.util.Log.d("PhotoDetailRepository", "Photo detail ${photoDetail.id} added successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoDetailRepository", "Error adding photo detail: ${e.message}", e)
                false
            }
        }
    }

    suspend fun updatePhotoDetail(photoDetail: PhotoDetail): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentDetails = _cachedPhotoDetails.first { it != null } ?: emptyList()
                val detailIndex = currentDetails.indexOfFirst { it.id == photoDetail.id }
                
                if (detailIndex == -1) {
                    android.util.Log.d("PhotoDetailRepository", "Photo detail with ID ${photoDetail.id} not found")
                    return@withContext false
                }
                
                val updatedDetails = currentDetails.toMutableList()
                updatedDetails[detailIndex] = photoDetail
                _cachedPhotoDetails.value = updatedDetails
                
                savePhotoDetailsToFile(updatedDetails)
                
                android.util.Log.d("PhotoDetailRepository", "Photo detail ${photoDetail.id} updated successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoDetailRepository", "Error updating photo detail: ${e.message}", e)
                false
            }
        }
    }

    suspend fun deletePhotoDetail(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentDetails = _cachedPhotoDetails.first { it != null } ?: emptyList()
                val detailToDelete = currentDetails.find { it.id == id }
                
                if (detailToDelete == null) {
                    android.util.Log.d("PhotoDetailRepository", "Photo detail with ID $id not found")
                    return@withContext false
                }
                
                val updatedDetails = currentDetails.filter { it.id != id }
                _cachedPhotoDetails.value = updatedDetails
                
                savePhotoDetailsToFile(updatedDetails)
                
                android.util.Log.d("PhotoDetailRepository", "Photo detail ${detailToDelete.id} deleted successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoDetailRepository", "Error deleting photo detail: ${e.message}", e)
                false
            }
        }
    }

    private suspend fun savePhotoDetailsToFile(photoDetails: List<PhotoDetail>) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(ListSerializer(PhotoDetail.serializer()), photoDetails)
                fileManager.saveJsonData(jsonFileName, jsonString)
                android.util.Log.d("PhotoDetailRepository", "Photo details saved to file successfully")
            } catch (e: Exception) {
                android.util.Log.e("PhotoDetailRepository", "Error saving photo details to file: ${e.message}", e)
            }
        }
    }
}