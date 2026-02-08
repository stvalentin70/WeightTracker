package com.stvalentin.weighttracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.launch

class UserProfileViewModel(private val repository: UserProfileRepository) : ViewModel() {
    
    // LiveData для наблюдения за профилем пользователя в UI
    val userProfile = repository.userProfile.asLiveData()
    
    // Вставляем новый профиль
    fun insertProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.insert(profile)
        }
    }
    
    // Обновляем существующий профиль
    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.update(profile)
        }
    }
}