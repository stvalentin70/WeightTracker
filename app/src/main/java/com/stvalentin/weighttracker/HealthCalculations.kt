package com.stvalentin.weighttracker

import android.content.Context
import androidx.core.content.ContextCompat
import java.util.*

object HealthCalculations {
    
    // Расчет ИМТ
    fun calculateBMI(weightKg: Double, heightCm: Int): Double {
        val heightMeters = heightCm / 100.0
        return weightKg / (heightMeters * heightMeters)
    }
    
    fun getBMICategory(bmi: Double): String {
        return when {
            bmi < 16.0 -> "Выраженный дефицит массы"
            bmi < 18.5 -> "Недостаточный вес"
            bmi < 25.0 -> "Нормальный вес"
            bmi < 30.0 -> "Избыточный вес"
            bmi < 35.0 -> "Ожирение I степени"
            bmi < 40.0 -> "Ожирение II степени"
            else -> "Ожирение III степени"
        }
    }
    
    // НОВАЯ ФУНКЦИЯ: Получение цвета для ИМТ
    fun getBMIColor(context: Context, bmi: Double): Int {
        return when {
            bmi < 18.5 -> ContextCompat.getColor(context, android.R.color.holo_orange_light)
            bmi < 25 -> ContextCompat.getColor(context, android.R.color.holo_green_light)
            bmi < 30 -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(context, android.R.color.holo_red_light)
        }
    }
    
    // Расчет нормы калорий (Миффлин-Сан Жеор)
    fun calculateDailyCalories(
        weightKg: Double,
        heightCm: Int,
        age: Int,
        gender: String,
        activityLevel: String
    ): Int {
        // Базальный метаболизм (BMR)
        val bmr = if (gender == "male") {
            10 * weightKg + 6.25 * heightCm - 5 * age + 5
        } else {
            10 * weightKg + 6.25 * heightCm - 5 * age - 161
        }
        
        // Коэффициент активности
        val activityMultiplier = when (activityLevel) {
            "sedentary" -> 1.2      // Сидячий образ жизни
            "light" -> 1.375         // Легкая активность (1-3 раза в неделю)
            "moderate" -> 1.55       // Умеренная активность (3-5 раз в неделю)
            "high" -> 1.725          // Высокая активность (6-7 раз в неделю)
            "extreme" -> 1.9         // Очень высокая активность
            else -> 1.55
        }
        
        return (bmr * activityMultiplier).toInt()
    }
    
    // Расчет возраста по дате рождения
    fun calculateAge(birthDate: Date): Int {
        val birthCalendar = Calendar.getInstance().apply { time = birthDate }
        val currentCalendar = Calendar.getInstance()
        
        var age = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        
        if (currentCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        
        return age
    }
    
    // Расчет процента жира (простая формула для приблизительной оценки)
    fun estimateBodyFatPercentage(
        weightKg: Double,
        heightCm: Int,
        age: Int,
        gender: String
    ): Double {
        val bmi = calculateBMI(weightKg, heightCm)
        
        return if (gender == "male") {
            (1.20 * bmi) + (0.23 * age) - 16.2
        } else {
            (1.20 * bmi) + (0.23 * age) - 5.4
        }
    }
    
    // Расчет идеального веса (формула Девайна)
    fun calculateIdealWeight(heightCm: Int, gender: String): Double {
        return if (gender == "male") {
            50 + 0.9 * (heightCm - 152)
        } else {
            45.5 + 0.9 * (heightCm - 152)
        }
    }
    
    // Расчет прогресса к цели
    fun calculateProgress(
        currentWeight: Double,
        startWeight: Double,
        targetWeight: Double
    ): ProgressResult {
        val totalRange = Math.abs(startWeight - targetWeight)
        
        if (totalRange == 0.0) {
            return ProgressResult(100, 0.0, "Цель достигнута!")
        }
        
        return when {
            startWeight > targetWeight -> { // Похудение
                val lost = startWeight - currentWeight
                val progressPercent = (lost / totalRange * 100).toInt().coerceIn(0, 100)
                val remaining = currentWeight - targetWeight
                ProgressResult(
                    progressPercent, 
                    remaining, 
                    String.format(Locale.getDefault(), "Осталось сбросить: %.1f кг", remaining)
                )
            }
            startWeight < targetWeight -> { // Набор массы
                val gained = currentWeight - startWeight
                val progressPercent = (gained / totalRange * 100).toInt().coerceIn(0, 100)
                val remaining = targetWeight - currentWeight
                ProgressResult(
                    progressPercent, 
                    remaining, 
                    String.format(Locale.getDefault(), "Осталось набрать: %.1f кг", remaining)
                )
            }
            else -> ProgressResult(0, 0.0, "Нет прогресса")
        }
    }
    
    data class ProgressResult(
        val progressPercent: Int,
        val remainingKg: Double,
        val message: String
    )
}