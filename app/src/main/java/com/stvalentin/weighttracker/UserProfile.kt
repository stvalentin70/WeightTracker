package com.stvalentin.weighttracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val heightCm: Int = 170,
    val gender: String = "male",
    val birthDate: Date = Date(),
    val targetWeightKg: Double = 70.0,
    val startWeightKg: Double = 80.0,
    val activityLevel: String = "moderate"
)