package com.stvalentin.weighttracker

import kotlinx.coroutines.flow.Flow

class WeightRepository(private val weightDao: WeightDao) {
    
    val allEntries: Flow<List<WeightEntry>> = weightDao.getAllEntries()
    
    suspend fun getLatestEntry(): WeightEntry? {
        return weightDao.getLatestEntry()
    }
    
    suspend fun insert(entry: WeightEntry): Long {
        return weightDao.insert(entry)
    }
    
    suspend fun update(entry: WeightEntry) {
        weightDao.update(entry)
    }
    
    suspend fun delete(entry: WeightEntry) {
        weightDao.delete(entry)
    }
    
    suspend fun deleteById(id: Long) {
        weightDao.deleteById(id)
    }
    
    suspend fun deleteAll() {
        weightDao.deleteAll()
    }
    
    suspend fun getCount(): Int {
        return weightDao.getCount()
    }
}