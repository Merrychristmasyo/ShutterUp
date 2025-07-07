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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    private val photoDetailRepository: PhotoDetailRepository,
    private val photoReviewRepository: PhotoReviewRepository,
    private val photoSpotRepository: PhotoSpotRepository,
    private val photoMetadataRepository: PhotoMetadataRepository
): ViewModel() {
    private val _photoDetail = MutableLiveData<PhotoDetail?>()
    val photoDetail: LiveData<PhotoDetail?> = _photoDetail

    private val _photoReviews = MutableLiveData<List<PhotoReview>>()
    val photoReviews: LiveData<List<PhotoReview>> = _photoReviews

    private val _photoSpot = MutableLiveData<PhotoSpot?>()
    val photoSpot: LiveData<PhotoSpot?> = _photoSpot

    private val _photoMetadata = MutableLiveData<PhotoMetadata?>()
    val photoMetadata: LiveData<PhotoMetadata?> = _photoMetadata

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
                val fetchedPhotoDetail = photoDetailRepository.getPhotoDetailById(photoId)
                val fetchedPhotoMetadata = photoMetadataRepository.getPhotoMetadataById(photoId)
                val fetchedPhotoReviews = photoReviewRepository.getPhotoReviewsById(photoId)
                if (fetchedPhotoMetadata != null) {
                    val fetchedPhotoSpot = photoSpotRepository.getPhotoSpotById(fetchedPhotoMetadata.photoSpotId)
                    _photoSpot.value = fetchedPhotoSpot
                    println("data : $fetchedPhotoDetail, $fetchedPhotoMetadata, $fetchedPhotoSpot, $fetchedPhotoReviews")

                }
                else {
                    _photoSpot.value = null
                    println("data : $fetchedPhotoDetail, $fetchedPhotoMetadata, null, $fetchedPhotoReviews")
                }

                _photoDetail.value = fetchedPhotoDetail
                _photoReviews.value = fetchedPhotoReviews
                _photoMetadata.value = fetchedPhotoMetadata
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photo detail data: ${e.message}"
                _photoDetail.value = null
                _photoReviews.value = emptyList()
                _photoSpot.value = null
                _photoMetadata.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

}