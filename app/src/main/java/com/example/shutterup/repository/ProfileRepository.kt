package com.example.shutterup.repository

import android.content.Context
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.model.Profile
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
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
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
                // 1. 먼저 internal storage에서 실제 업로드된 데이터 로드
                val internalJsonString = fileManager.loadJsonData(jsonFilePath)
                val internalData = if (internalJsonString != null) {
                    try {
                        json.decodeFromString<List<Profile>>(internalJsonString)
                    } catch (e: Exception) {
                        android.util.Log.w("ProfileRepository", "Error parsing internal data: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // 2. Assets에서 초기 더미 데이터 로드
                val assetsData = try {
                    val assetsJsonString = context.assets.open(jsonFilePath).bufferedReader().use { it.readText() }
                    json.decodeFromString<List<Profile>>(assetsJsonString)
                } catch (e: Exception) {
                    android.util.Log.w("ProfileRepository", "Error loading assets data: ${e.message}")
                    emptyList()
                }

                // 3. 두 데이터를 합치되, 중복 userId는 internal 데이터를 우선으로 함
                val combinedData = mutableMapOf<String, Profile>()
                
                // 먼저 assets 데이터를 추가
                assetsData.forEach { profile ->
                    combinedData[profile.userId] = profile
                }
                
                // 그 다음 internal 데이터를 추가 (중복 userId가 있으면 덮어씀)
                internalData.forEach { profile ->
                    combinedData[profile.userId] = profile
                }

                val finalData = combinedData.values.toList()
                android.util.Log.d("ProfileRepository", "Loaded ${finalData.size} profiles (assets: ${assetsData.size}, internal: ${internalData.size})")
                
                finalData
            } catch (e: Exception) {
                android.util.Log.e("ProfileRepository", "Error loading profiles: ${e.message}", e)
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