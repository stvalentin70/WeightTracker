package com.stvalentin.weighttracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

/**
 * Data class representing a weight entry in the application.
 * @property id Unique identifier (null for new entries)
 * @property weight Weight value in kilograms
 * @property dateTime Date and time of the measurement
 * @property context Measurement context (morning, evening, etc.)
 * @property note Additional note/comment
 */
@Entity(tableName = "weight_entries")
@TypeConverters(Converters::class)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val weight: Double,
    val dateTime: Date,
    val context: WeightContext,
    val note: String = ""
)

/**
 * Enum representing the context of weight measurement.
 */
enum class WeightContext(val displayName: String) {
    MORNING_BEFORE_MEAL("Утро, до еды"),
    MORNING_AFTER_MEAL("Утро, после еды"),
    MORNING_AFTER_TOILET("Утро, после туалета"),
    DAY_BEFORE_MEAL("День, до еды"),
    DAY_AFTER_MEAL("День, после еды"),
    DAY_AFTER_TOILET("День, после туалета"),
    EVENING_BEFORE_MEAL("Вечер, до еды"),
    EVENING_AFTER_MEAL("Вечер, после еды"),
    EVENING_AFTER_TOILET("Вечер, после туалета"),
    OTHER("Не важно")
}