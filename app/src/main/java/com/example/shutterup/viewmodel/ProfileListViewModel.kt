package com.example.shutterup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shutterup.model.Profile
import com.example.shutterup.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
): ViewModel() {
    private val _profiles = MutableLiveData<List<Profile>>()
    val profiles: LiveData<List<Profile>> = _profiles

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val fetchedProfiles = profileRepository.getProfileList()
                _profiles.value = fetchedProfiles
                println("profile: $fetchedProfiles")

            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photo spots : ${e.message}"
                _profiles.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onProfileClicked(profile: Profile) {
        // TODO : 상세 페이지로 이동
        println("Clicked on photo spot: ${profile.userId}")
    }

    override fun onCleared() {
        super.onCleared()
        println("ProfileListViewModel cleared")
    }

    // 1) 찜한 userId 집합
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    // 2) 토글 메서드
    fun toggleFavorite(userId: String) {
        _favorites.update { current ->
            if (current.contains(userId)) current - userId
            else current + userId
        }
    }
}