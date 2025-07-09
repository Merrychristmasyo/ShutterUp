package com.example.shutterup.repository
import android.content.Context
import com.example.shutterup.model.PhotoReview
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoReviewRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jsonFilePath = "photoreview.json"
    private val _cachedPhotoReviews = MutableStateFlow<List<PhotoReview>?>(null)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            val loadedSpots = loadPhotoReviewsFromJsonInternal()
            _cachedPhotoReviews.value = loadedSpots
            println("Photo reviews loaded and cached successfully.")
        }
    }

    private suspend fun loadPhotoReviewsFromJsonInternal(): List<PhotoReview> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 먼저 internal storage에서 실제 업로드된 데이터 로드
                val internalJsonString = fileManager.loadJsonData(jsonFilePath)
                val internalData = if (internalJsonString != null) {
                    try {
                        json.decodeFromString<List<PhotoReview>>(internalJsonString)
                    } catch (e: Exception) {
                        android.util.Log.w("PhotoReviewRepository", "Error parsing internal data: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // 2. Assets에서 초기 더미 데이터 로드
                val assetsData = try {
                    val assetsJsonString = context.assets.open(jsonFilePath).bufferedReader().use { it.readText() }
                    json.decodeFromString<List<PhotoReview>>(assetsJsonString)
                } catch (e: Exception) {
                    android.util.Log.w("PhotoReviewRepository", "Error loading assets data: ${e.message}")
                    emptyList()
                }

                // 3. 두 데이터를 합치되, 중복 reviewId는 internal 데이터를 우선으로 함
                val combinedData = mutableMapOf<String, PhotoReview>()
                
                // 먼저 assets 데이터를 추가
                assetsData.forEach { review ->
                    combinedData[review.reviewId] = review
                }
                
                // 그 다음 internal 데이터를 추가 (중복 reviewId가 있으면 덮어씀)
                internalData.forEach { review ->
                    combinedData[review.reviewId] = review
                }

                val finalData = combinedData.values.toList()
                android.util.Log.d("PhotoReviewRepository", "Loaded ${finalData.size} photo reviews (assets: ${assetsData.size}, internal: ${internalData.size})")
                
                finalData
            } catch (e: Exception) {
                android.util.Log.e("PhotoReviewRepository", "Error loading photo reviews: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun getPhotoReviewsById(id: String): List<PhotoReview> {
        val photoReviews = _cachedPhotoReviews.first { it != null } ?: return emptyList()
        val filteredPhotoReview = photoReviews.filter { it.id == id }

        return filteredPhotoReview
    }
}