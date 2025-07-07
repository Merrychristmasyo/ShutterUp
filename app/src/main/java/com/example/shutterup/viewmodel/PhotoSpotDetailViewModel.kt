package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.PhotoDetail
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.model.PhotoReview
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.repository.PhotoDetailRepository
import com.example.shutterup.repository.PhotoMetadataRepository
import com.example.shutterup.repository.PhotoReviewRepository
import com.example.shutterup.repository.PhotoSpotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PhotoSpotDetailViewModel
@Inject
constructor(
        private val photoSpotRepository: PhotoSpotRepository,
        private val photoMetadataRepository: PhotoMetadataRepository
) : ViewModel() {
    private val _photoSpot = MutableLiveData<PhotoSpot?>()
    val photoSpot: LiveData<PhotoSpot?> = _photoSpot

    private val _photoMetadataList = MutableLiveData<List<PhotoMetadata>>()
    val photoMetadataList: LiveData<List<PhotoMetadata>> = _photoMetadataList

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadData(photoId: String) {
        if (_isLoading.value == true) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val fetchedPhotoSpot = photoSpotRepository.getPhotoSpotById(photoId)
                _photoSpot.value = fetchedPhotoSpot

                if (fetchedPhotoSpot == null) {
                    _photoMetadataList.value = emptyList()
                    _photoSpot.value = null
                }
                else {
                    val fetchedPhotoMetadataList = photoMetadataRepository.getPhotoMetadataListByPhotoSpotId(fetchedPhotoSpot.id)
                    _photoMetadataList.value = fetchedPhotoMetadataList
                    _photoSpot.value = fetchedPhotoSpot
                }

            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photo spot detail data: ${e.message}"
                _photoMetadataList.value = emptyList()
                _photoSpot.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}
