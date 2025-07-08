package com.example.shutterup.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.model.PhotoDetail
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.model.PhotoUploadData
import com.example.shutterup.repository.PhotoSpotRepository
import com.example.shutterup.repository.PhotoDetailRepository
import com.example.shutterup.repository.PhotoMetadataRepository
import com.example.shutterup.utils.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PhotoUploadViewModel @Inject constructor(
    private val photoSpotRepository: PhotoSpotRepository,
    private val photoDetailRepository: PhotoDetailRepository,
    private val photoMetadataRepository: PhotoMetadataRepository,
    private val fileManager: FileManager
) : ViewModel() {
    private val _photoSpot = MutableLiveData<PhotoSpot?>(null)
    val photoSpot: LiveData<PhotoSpot?> = _photoSpot

    private val _photoDetail = MutableLiveData<PhotoDetail?>(null)
    val photoDetail: LiveData<PhotoDetail?> = _photoDetail

    private val _photoMetadata = MutableLiveData<PhotoMetadata?>(null)
    val photoMetadata: LiveData<PhotoMetadata?> = _photoMetadata

    private val _isUploading = MutableLiveData<Boolean>(false)
    val isUploading: LiveData<Boolean> = _isUploading

    private val _uploadResult = MutableLiveData<UploadResult?>(null)
    val uploadResult: LiveData<UploadResult?> = _uploadResult

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun uploadPhoto(uploadData: PhotoUploadData, imageUri: Uri?) {
        android.util.Log.d("PhotoUpload", "uploadPhoto called with data: $uploadData")
        val validationResult = uploadData.validate()
        if (validationResult is PhotoUploadData.ValidationResult.Error) {
            android.util.Log.e("PhotoUpload", "Validation failed: ${validationResult.messages}")
            _errorMessage.value = validationResult.messages.joinToString("\n")
            return
        }

        if (imageUri == null) {
            android.util.Log.e("PhotoUpload", "Image URI is null")
            _errorMessage.value = "이미지를 선택해주세요"
            return
        }

        android.util.Log.d("PhotoUpload", "Validation passed, starting upload...")
        _isUploading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val photoId = generateUniqueId()
                val currentTimestamp = getCurrentTimestamp()
                val filename = uploadData.generateFilename() + ".jpg"

                // 이미지 파일 저장
                android.util.Log.d("PhotoUpload", "Saving image file: $filename")
                val imageSaved = fileManager.saveImage(imageUri, filename)
                if (!imageSaved) {
                    android.util.Log.e("PhotoUpload", "Failed to save image file")
                    _uploadResult.value = UploadResult.Failure("이미지 파일 저장에 실패했습니다")
                    return@launch
                }

                // 썸네일 생성
                android.util.Log.d("PhotoUpload", "Creating thumbnail")
                val thumbnailCreated = fileManager.createAndSaveThumbnail(imageUri, filename)
                if (!thumbnailCreated) {
                    android.util.Log.w("PhotoUpload", "Failed to create thumbnail, but continuing...")
                }

                // 기존 PhotoSpot 확인 또는 생성
                val existingPhotoSpot = findExistingPhotoSpot(uploadData)
                val photoModels = createPhotoModels(uploadData, photoId, existingPhotoSpot, currentTimestamp, filename)
                
                // 모든 데이터 업로드
                android.util.Log.d("PhotoUpload", "Uploading data to repositories...")
                val uploadResults = uploadAllData(photoModels, existingPhotoSpot != null)
                android.util.Log.d("PhotoUpload", "Upload results: $uploadResults")
                
                if (uploadResults.all { it }) {
                    android.util.Log.d("PhotoUpload", "Upload successful!")
                    _uploadResult.value = UploadResult.Success(
                        UploadedPhotoData(photoModels.photoSpot, photoModels.photoDetail, photoModels.photoMetadata)
                    )
                } else {
                    android.util.Log.e("PhotoUpload", "Upload failed - some repositories returned false")
                    // 실패 시 이미지 파일 삭제
                    fileManager.deleteImage(filename)
                    _uploadResult.value = UploadResult.Failure("사진 업로드에 실패했습니다")
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoUpload", "Upload exception: ${e.message}", e)
                _uploadResult.value = UploadResult.Failure("업로드 중 오류가 발생했습니다: ${e.message}")
                _errorMessage.value = e.message
            } finally {
                android.util.Log.d("PhotoUpload", "Upload process finished")
                _isUploading.value = false
            }
        }
    }

    private suspend fun findExistingPhotoSpot(uploadData: PhotoUploadData): PhotoSpot? {
        return try {
            val allPhotoSpots = photoSpotRepository.getPhotoSpotList()
            allPhotoSpots.find { photoSpot ->
                photoSpot.name.trim() == uploadData.spotName.trim() &&
                photoSpot.latitude == uploadData.latitude &&
                photoSpot.longitude == uploadData.longitude
            }
        } catch (e: Exception) {
            android.util.Log.e("PhotoUpload", "Error finding existing photo spot: ${e.message}")
            null
        }
    }

    private fun createPhotoModels(
        uploadData: PhotoUploadData,
        photoId: String,
        existingPhotoSpot: PhotoSpot?,
        timestamp: String,
        filename: String
    ): PhotoModels {
        val photoSpot = existingPhotoSpot?.copy(
            photoCount = existingPhotoSpot.photoCount + 1
        ) ?: PhotoSpot(
            id = generateUniqueId(),
            name = uploadData.spotName.trim(),
            latitude = uploadData.latitude,
            longitude = uploadData.longitude,
            photoCount = 1
        )

        val photoDetail = PhotoDetail(
            id = photoId,
            method = uploadData.shootingMethod,
            timestamp = timestamp
        )

        val photoMetadata = PhotoMetadata(
            id = photoId,
            filename = filename,
            description = uploadData.description,
            tags = uploadData.tags,
            fNumber = uploadData.fNumber,
            focalLength = uploadData.focalLength,
            iso = uploadData.iso,
            shutterSpeed = uploadData.shutterSpeed,
            lensName = uploadData.lensName,
            cameraName = uploadData.cameraName,
            photoSpotId = photoSpot.id,
            shootingMethod = uploadData.shootingMethod
        )

        return PhotoModels(photoSpot, photoDetail, photoMetadata)
    }

    private suspend fun uploadAllData(photoModels: PhotoModels, isExistingSpot: Boolean): List<Boolean> {
        val photoSpotResult = if (isExistingSpot) {
            // 기존 PhotoSpot의 photoCount 업데이트
            photoSpotRepository.updatePhotoSpot(photoModels.photoSpot)
        } else {
            // 새 PhotoSpot 추가
            photoSpotRepository.addPhotoSpot(photoModels.photoSpot)
        }
        
        return listOf(
            photoSpotResult,
            photoDetailRepository.addPhotoDetail(photoModels.photoDetail),
            photoMetadataRepository.addPhotoMetadata(photoModels.photoMetadata)
        )
    }

    private fun generateUniqueId(): String {
        return "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }

    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    fun clearUploadResult() {
        _uploadResult.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private data class PhotoModels(
        val photoSpot: PhotoSpot,
        val photoDetail: PhotoDetail,
        val photoMetadata: PhotoMetadata
    )

    data class UploadedPhotoData(
        val photoSpot: PhotoSpot,
        val photoDetail: PhotoDetail,
        val photoMetadata: PhotoMetadata
    )

    sealed class UploadResult {
        data class Success(val uploadedData: UploadedPhotoData) : UploadResult()
        data class Failure(val message: String) : UploadResult()
    }
}
