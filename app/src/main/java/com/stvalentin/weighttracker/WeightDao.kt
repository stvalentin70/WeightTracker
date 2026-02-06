package com.stvalentin.weighttracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    
    @Query("SELECT * FROM weight_entries ORDER BY dateTime DESC")
    fun getAllEntries(): Flow<List<WeightEntry>>
    
    @Query("SELECT * FROM weight_entries ORDER BY dateTime DESC LIMIT 1")
    suspend fun getLatestEntry(): WeightEntry?
    
    @Insert
    suspend fun insert(entry: WeightEntry): Long
    
    @Update
    suspend fun update(entry: WeightEntry)
    
    @Delete
    suspend fun delete(entry: WeightEntry)
    
    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM weight_entries")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM weight_entries")
    suspend fun getCount(): Int
}