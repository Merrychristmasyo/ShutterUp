package com.example.shutterup.repository
import android.content.Context
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
class PhotoSpotRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jsonFilePath = "photospot.json"
    private val _cachedPhotoSpots = MutableStateFlow<List<PhotoSpot>?>(null)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            val loadedSpots = loadPhotoSpotsFromJsonInternal()
            _cachedPhotoSpots.value = loadedSpots
            println("Photo spots loaded and cached successfully.")
        }
    }

    private suspend fun loadPhotoSpotsFromJsonInternal(): List<PhotoSpot> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open(jsonFilePath).bufferedReader().use { it.readText() }
                json.decodeFromString<List<PhotoSpot>>(jsonString)
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

    suspend fun getPhotoSpotList(): List<PhotoSpot> {
        return _cachedPhotoSpots.first { it != null } ?: emptyList()
    }

    suspend fun getPhotoSpotById(id: String): PhotoSpot? {
        val photoSpots = _cachedPhotoSpots.first { it != null } ?: return null
        return photoSpots.find { it.id == id }
    }
}