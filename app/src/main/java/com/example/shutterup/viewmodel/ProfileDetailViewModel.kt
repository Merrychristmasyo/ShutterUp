package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.Profile
import com.example.shutterup.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _profile = MutableLiveData<Profile?>()
    val profile: LiveData<Profile?> = _profile
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadData(userId: String) {
        if (_isLoading.value == true) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val fetchedProfile = profileRepository.getProfileById(userId)
                _profile.value = fetchedProfile
                
                if (fetchedProfile == null) {
                    _errorMessage.value = "프로필을 찾을 수 없습니다."
                }
            } catch (e: Exception) {
                _errorMessage.value = "프로필 로드 중 오류가 발생했습니다: ${e.message}"
                _profile.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}