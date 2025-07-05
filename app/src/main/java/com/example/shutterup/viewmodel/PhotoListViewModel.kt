package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.repository.PhotoMetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PhotoListViewModel @Inject constructor(
    private val photoMetadataRepository: PhotoMetadataRepository
): ViewModel() {
    private val _photoMetadata = MutableLiveData<List<PhotoMetadata>>()
    val photos: LiveData<List<PhotoMetadata>> = _photoMetadata

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val photoMetadataList = photoMetadataRepository.getAllPhotoMetadata() // ✅ 메서드 호출 변경
                withContext(Dispatchers.Main) {
                    _photoMetadata.value = photoMetadataList
                }
            } catch (e: Exception) {
                _errorMessage.postValue("사진 메타데이터를 로드하는 데 실패했습니다: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}