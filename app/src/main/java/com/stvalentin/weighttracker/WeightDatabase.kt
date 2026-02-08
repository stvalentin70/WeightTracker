package com.stvalentin.weighttracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WeightEntry::class, UserProfile::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WeightDatabase : RoomDatabase() {
    
    abstract fun weightDao(): WeightDao
    abstract fun userProfileDao(): UserProfileDao
    
    companion object {
        @Volatile
        private var INSTANCE: WeightDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Создаём таблицу с правильными NOT NULL ограничениями
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        heightCm INTEGER NOT NULL,
                        gender TEXT NOT NULL,
                        birthDate INTEGER NOT NULL,
                        targetWeightKg REAL NOT NULL,
                        startWeightKg REAL NOT NULL,
                        activityLevel TEXT NOT NULL
                    )
                """)
                
                // Вставляем начальную запись с дефолтными значениями
                database.execSQL("""
                    INSERT INTO user_profile (
                        name, heightCm, gender, birthDate, 
                        targetWeightKg, startWeightKg, activityLevel
                    ) VALUES (
                        '', 
                        170, 
                        'male', 
                        ${System.currentTimeMillis()}, 
                        70.0, 
                        80.0, 
                        'moderate'
                    )
                """)
            }
        }
        
        fun getDatabase(context: Context): WeightDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeightDatabase::class.java,
                    "weight_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}