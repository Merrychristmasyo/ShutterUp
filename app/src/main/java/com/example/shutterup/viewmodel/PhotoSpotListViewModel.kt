package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.repository.PhotoSpotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoSpotListViewModel @Inject constructor(
    private val photoSpotRepository: PhotoSpotRepository
): ViewModel() {
    private val _photoSpots = MutableLiveData<List<PhotoSpot>>()
    val photoSpots: LiveData<List<PhotoSpot>> = _photoSpots

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadPhotoSpots()
    }

    fun loadPhotoSpots() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val fetchedPhotoSpots = photoSpotRepository.getPhotoSpotList()
                _photoSpots.value = fetchedPhotoSpots

            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photo spots : ${e.message}"
                _photoSpots.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPhotoSpotClicked(photoSpot: PhotoSpot) {
        // TODO : 상세 페이지로 이동
        println("Clicked on photo spot: ${photoSpot.name}")
    }

    override fun onCleared() {
        super.onCleared()
        println("PhotoSpotListViewModel cleared")
    }
}