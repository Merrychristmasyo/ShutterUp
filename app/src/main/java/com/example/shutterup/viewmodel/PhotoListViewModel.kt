package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.repository.PhotoMetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoListViewModel @Inject constructor(
    private val photoMetadataRepository: PhotoMetadataRepository
): ViewModel() {
    private val _photoMetadata = MutableLiveData<List<PhotoMetadata>>()
    val photoMetadata: LiveData<List<PhotoMetadata>> = _photoMetadata

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadData()
    }

    private fun loadData() {
        if (_isLoading.value == true) return // 중복 호출 방지

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val fetchedPhotoMetadata = photoMetadataRepository.getAllPhotoMetadata()

                _photoMetadata.value = fetchedPhotoMetadata

            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photo detail data: ${e.message}"
                _photoMetadata.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

}