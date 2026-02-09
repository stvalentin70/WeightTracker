package com.stvalentin.weighttracker

import androidx.room.TypeConverter
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromWeightContext(value: WeightContext?): String? {
        return value?.name
    }

    @TypeConverter
    fun toWeightContext(value: String?): WeightContext? {
        return value?.let { WeightContext.valueOf(it) }
    }
    
    // НЕ НУЖНО добавлять конвертеры для String - Room умеет работать с ними напрямую
}