package com.example.shutterup.repository
import android.content.Context
import com.example.shutterup.model.PhotoReview
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
class PhotoReviewRepository @Inject constructor(
    @ApplicationContext private val context: Context
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
                val jsonString = context.assets.open(jsonFilePath).bufferedReader().use { it.readText() }
                json.decodeFromString<List<PhotoReview>>(jsonString)
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

    suspend fun getPhotoReviewsById(id: String): List<PhotoReview> {
        val photoReviews = _cachedPhotoReviews.first { it != null } ?: return emptyList()
        val filteredPhotoReview = photoReviews.filter { it.id == id }

        return filteredPhotoReview
    }
}