package com.stvalentin.weighttracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.firstOrNull
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
    
    // Сохраняем профиль (интеллектуально: вставляем или обновляем)
    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            // Получаем текущий профиль
            val currentProfile = repository.userProfile.firstOrNull()
            
            if (currentProfile != null) {
                // Обновляем существующий профиль с сохранением id
                val updatedProfile = currentProfile.copy(
                    name = profile.name,
                    heightCm = profile.heightCm,
                    gender = profile.gender,
                    birthDate = profile.birthDate,
                    targetWeightKg = profile.targetWeightKg,
                    startWeightKg = profile.startWeightKg,
                    activityLevel = profile.activityLevel
                )
                repository.update(updatedProfile)
            } else {
                // Вставляем новый профиль
                repository.insert(profile)
            }
        }
    }
}