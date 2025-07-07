package com.example.shutterup.repository
import android.content.Context
import com.example.shutterup.model.PhotoDetail
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
class PhotoDetailRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jsonFilePath = "photodetail.json"
    private val _cachedPhotoDetails = MutableStateFlow<List<PhotoDetail>?>(null)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            val loadedDetail = loadPhotoDetailsFromJsonInternal()
            _cachedPhotoDetails.value = loadedDetail
            println("Photo details loaded and cached successfully.")
        }
    }

    private suspend fun loadPhotoDetailsFromJsonInternal(): List<PhotoDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open(jsonFilePath).bufferedReader().use { it.readText() }
                json.decodeFromString<List<PhotoDetail>>(jsonString)
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

    suspend fun getPhotoDetailList(): List<PhotoDetail> {
        return _cachedPhotoDetails.first { it != null } ?: emptyList()
    }

    suspend fun getPhotoDetailById(id: String): PhotoDetail? {
        val photoDetails = _cachedPhotoDetails.first { it != null } ?: return PhotoDetail("unknown", "unknown", "unknown")
        return photoDetails.find { it.id == id }
    }
}