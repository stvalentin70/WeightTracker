package com.stvalentin.weighttracker

import androidx.lifecycle.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class WeightViewModel(private val repository: WeightRepository) : ViewModel() {
    
    // LiveData для наблюдения за всеми записями
    val allEntries = repository.allEntries.asLiveData()
    
    // LiveData для последней записи
    val latestEntry = allEntries.map { entries ->
        entries.firstOrNull()
    }
    
    // Добавление новой записи
    suspend fun addEntry(entry: WeightEntry): Long {
        return repository.insert(entry)
    }
    
    // Удаление записи
    fun deleteEntry(entry: WeightEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
    
    // Получение количества записей
    suspend fun getEntriesCount(): Int {
        return repository.getCount()
    }
    
    companion object {
        fun provideFactory(repository: WeightRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WeightViewModel(repository) as T
                }
            }
        }
    }
}