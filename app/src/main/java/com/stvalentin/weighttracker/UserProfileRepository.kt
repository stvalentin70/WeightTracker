package com.stvalentin.weighttracker

import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val userProfileDao: UserProfileDao) {
    
    // Получаем профиль пользователя как Flow
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    
    // Вставляем новый профиль
    suspend fun insert(profile: UserProfile) {
        userProfileDao.insert(profile)
    }
    
    // Обновляем существующий профиль
    suspend fun update(profile: UserProfile) {
        userProfileDao.update(profile)
    }
    
    // Удаляем все профили (можно использовать для отладки)
    suspend fun deleteAll() {
        userProfileDao.deleteAll()
    }
}