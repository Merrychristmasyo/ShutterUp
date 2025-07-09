package com.example.shutterup.repository

import android.content.Context
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.model.Profile
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
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jsonFilePath = "profile.json"
    private val _cachedProfiles = MutableStateFlow<List<Profile>>(emptyList())
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            val loadedSpots = loadProfileFromJsonInternal()
            _cachedProfiles.value = loadedSpots
            println("User profile loaded and cached successfully.")
        }
    }

    private suspend fun loadProfileFromJsonInternal(): List<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open(jsonFilePath).bufferedReader().use { it.readText() }
                json.decodeFromString<List<Profile>>(jsonString)
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

    suspend fun getProfileList(): List<Profile> {
        return _cachedProfiles.first { it.isNotEmpty()}
        // 여기가 원래는 null이였는데, 이걸 it.isNotEmpty()로 바꾸니까 '사용자 정보가 없습니다.'
        // 에서 의도한 대로 사용자 정보를 불러올 수 있었고, 사용자 이름을 검색할 수도 있었음.
    }

    suspend fun getProfileById(id: String): Profile? {
        val profiles = _cachedProfiles.first { it != null } ?: return null
        return profiles.find { it.userId == id }
    }
}