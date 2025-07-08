package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.model.Profile
import com.example.shutterup.repository.ProfileRepository
import com.example.shutterup.repository.PhotoMetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val photoRepo: PhotoMetadataRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Detail 화면에서 관찰할 LiveData
    private val _profile = MutableLiveData<Profile?>()
    val profile: LiveData<Profile?> = _profile

    // 사용자가 찍은 사진 리스트용 LiveData 추가
    private val _photos = MutableLiveData<List<PhotoMetadata>>(emptyList())
    val photos: LiveData<List<PhotoMetadata>> = _photos

    // 뷰에서 호출 -> userId에 해당하는 Profile을 불러옴
    fun load(userId: String) {
        viewModelScope.launch {
            //1) 프로필 사진 불러오기
            _profile.value = repo.getProfileById(userId)
            //2) 해당 사용자가 찍은 사진 목록 불러오기
            _photos.value = photoRepo.getPhotoMetadataListByUserId(userId)
        }
    }
}