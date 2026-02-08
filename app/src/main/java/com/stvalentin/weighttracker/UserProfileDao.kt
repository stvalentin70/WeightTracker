package com.stvalentin.weighttracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    
    // ИСПРАВЛЕНО: Добавлена стратегия замены
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfile)
    
    @Update
    suspend fun update(profile: UserProfile)
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>
    
    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
    
    // ДОБАВЛЕНО: Метод для получения профиля без Flow (для отладки)
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfileSync(): UserProfile?
}