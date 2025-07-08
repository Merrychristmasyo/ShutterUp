package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.Profile
import com.example.shutterup.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ★ Detail 화면에서 관찰할 LiveData
    private val _profile = MutableLiveData<Profile?>()
    val profile: LiveData<Profile?> = _profile

    // 뷰에서 호출 -> userId에 해당하는 Profile을 불러옴
    fun load(userId: String) {
        viewModelScope.launch {
            _profile.value = repo.getProfileById(userId)
        }
    }
}