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

    init {
        loadPhotoSpots()
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
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photo spots : ${e.message}"
                _photoSpots.value = emptyList()
                _thumbnailPhotoMetadataList.value = hashMapOf<String, PhotoMetadata>()
            } finally {
                _isLoading.value = false
            }
        }
    }
}