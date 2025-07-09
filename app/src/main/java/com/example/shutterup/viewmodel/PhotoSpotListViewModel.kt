package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.repository.PhotoMetadataRepository
import com.example.shutterup.repository.PhotoSpotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class PhotoSpotListViewModel @Inject constructor(
    private val photoSpotRepository: PhotoSpotRepository,
    private val photoMetadataRepository: PhotoMetadataRepository
): ViewModel() {
    private val _photoSpots = MutableLiveData<List<PhotoSpot>>()
    val photoSpots: LiveData<List<PhotoSpot>> = _photoSpots

    private val _thumbnailPhotoMetadataList = MutableLiveData<HashMap<String, PhotoMetadata>>()
    val thumbnailPhotoMetadataList: LiveData<HashMap<String, PhotoMetadata>> = _thumbnailPhotoMetadataList

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // 사용자 위치 정보
    private val _userLocation = MutableLiveData<Pair<Double, Double>?>(null)
    val userLocation: LiveData<Pair<Double, Double>?> = _userLocation

    // 거리순으로 정렬된 포토스팟 목록
    private val _sortedPhotoSpots = MutableLiveData<List<PhotoSpot>>()
    val sortedPhotoSpots: LiveData<List<PhotoSpot>> = _sortedPhotoSpots

    // 검색 기능
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _filteredPhotoSpots = MutableLiveData<List<PhotoSpot>>()
    val filteredPhotoSpots: LiveData<List<PhotoSpot>> = _filteredPhotoSpots

    init {
        loadPhotoSpots()
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        _userLocation.value = Pair(latitude, longitude)
        updateSortedPhotoSpots()
    }

    private fun updateSortedPhotoSpots() {
        val currentPhotoSpots = _photoSpots.value ?: emptyList()
        val currentUserLocation = _userLocation.value
        
        if (currentUserLocation != null) {
            val sorted = currentPhotoSpots.sortedBy { photoSpot ->
                calculateDistance(
                    currentUserLocation.first, currentUserLocation.second,
                    photoSpot.latitude, photoSpot.longitude
                )
            }
            _sortedPhotoSpots.value = sorted
        } else {
            _sortedPhotoSpots.value = currentPhotoSpots
        }
        
        // 정렬 후 검색 필터링도 업데이트
        applySearchFilter()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applySearchFilter()
    }

    private fun applySearchFilter() {
        val query = _searchQuery.value?.trim() ?: ""
        val currentSortedPhotoSpots = _sortedPhotoSpots.value ?: emptyList()
        
        _filteredPhotoSpots.value = if (query.isEmpty()) {
            currentSortedPhotoSpots
        } else {
            currentSortedPhotoSpots.filter { photoSpot ->
                photoSpot.name.contains(query, ignoreCase = true)
            }
        }
    }

    private fun loadPhotoSpots() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val fetchedPhotoSpots = photoSpotRepository.getPhotoSpotList()
                val fetchedThumbnailPhotoMetadataList = photoMetadataRepository.getThumbnailPhotoMetadataList()
                _photoSpots.value = fetchedPhotoSpots
                _thumbnailPhotoMetadataList.value = fetchedThumbnailPhotoMetadataList
                updateSortedPhotoSpots() // 포토스팟 로드 후 정렬 업데이트
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photo spots : ${e.message}"
                _photoSpots.value = emptyList()
                _thumbnailPhotoMetadataList.value = hashMapOf<String, PhotoMetadata>()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // 외부에서 호출할 수 있는 새로고침 메서드
    fun refreshPhotoSpots() {
        loadPhotoSpots()
    }

    // 두 지점 간의 직선 거리 계산 (Haversine 공식)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // 지구 반지름 (km)
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    // 새로운 포토 스팟 추가 함수
    fun addNewPhotoSpot(photoSpot: PhotoSpot) {
        viewModelScope.launch {
            try {
                val success = photoSpotRepository.addPhotoSpot(photoSpot)
                if (success) {
                    // 리스트 다시 로드
                    loadPhotoSpots()
                }
            } catch (e: Exception) {
                _errorMessage.value = "포토 스팟 추가 실패: ${e.message}"
            }
        }
    }
}