package com.stvalentin.weighttracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    
    @Insert
    suspend fun insert(profile: UserProfile)
    
    @Update
    suspend fun update(profile: UserProfile)
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>
    
    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}