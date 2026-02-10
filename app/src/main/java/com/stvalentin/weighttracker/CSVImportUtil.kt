package com.stvalentin.weighttracker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class CSVImportUtil {
    
    companion object {
        private const val TAG = "CSVImportUtil"
        private val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        private val dateOnlyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        /**
         * Импортировать данные из CSV файла
         */
        suspend fun importFromCSV(
            context: Context,
            inputStream: InputStream,
            repository: WeightRepository
        ): Pair<Int, Int> {
            return withContext(Dispatchers.IO) {
                var successCount = 0
                var errorCount = 0
                
                try {
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    var line: String?
                    var lineNumber = 0
                    
                    // Читаем заголовок
                    val header = reader.readLine()
                    lineNumber++
                    Log.d(TAG, "Заголовок CSV: $header")
                    
                    while (reader.readLine().also { line = it } != null) {
                        lineNumber++
                        
                        try {
                            line?.let { csvLine ->
                                Log.d(TAG, "Строка $lineNumber: $csvLine")
                                val entry = parseCSVLine(csvLine, lineNumber)
                                if (entry != null) {
                                    Log.d(TAG, "Успешно распарсена строка $lineNumber: weight=${entry.weight}, date=${entry.dateTime}")
                                    val insertedId = repository.insert(entry)
                                    Log.d(TAG, "Запись добавлена в базу с ID: $insertedId")
                                    successCount++
                                } else {
                                    errorCount++
                                    Log.w(TAG, "Ошибка парсинга строки $lineNumber: пустая запись")
                                }
                            }
                        } catch (e: Exception) {
                            errorCount++
                            Log.e(TAG, "Ошибка обработки строки $lineNumber: ${e.message}", e)
                        }
                    }
                    
                    reader.close()
                    Log.d(TAG, "Импорт завершен. Успешно: $successCount, Ошибок: $errorCount")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при чтении CSV файла: ${e.message}", e)
                    errorCount++
                }
                
                Pair(successCount, errorCount)
            }
        }
        
        /**
         * Парсинг строки CSV
         */
        private fun parseCSVLine(line: String, lineNumber: Int): WeightEntry? {
            try {
                Log.d(TAG, "Начинаем парсинг строки $lineNumber: $line")
                
                // Очищаем строку
                val cleanedLine = line.trim()
                Log.d(TAG, "Очищенная строка: $cleanedLine")
                
                // Убираем кавычки в начале и конце, если они есть
                val trimmedLine = if (cleanedLine.startsWith("\"") && cleanedLine.endsWith("\"")) {
                    cleanedLine.substring(1, cleanedLine.length - 1)
                } else {
                    cleanedLine
                }
                
                // Разбиваем по запятой
                val parts = trimmedLine.split(",")
                Log.d(TAG, "Части строки (${parts.size}): $parts")
                
                if (parts.size < 4) {
                    Log.w(TAG, "Строка $lineNumber имеет недостаточно частей: ${parts.size}")
                    return null
                }
                
                // Парсим ID (может быть пустым)
                val id = parts[0].toLongOrNull()
                Log.d(TAG, "ID: $id")
                
                // Парсим вес (заменяем запятую на точку если нужно)
                val weightStr = parts[1].replace(',', '.')
                val weight = weightStr.toDoubleOrNull()
                if (weight == null) {
                    Log.w(TAG, "Строка $lineNumber: неверный формат веса: $weightStr")
                    return null
                }
                Log.d(TAG, "Вес: $weight")
                
                // Парсим дату и время
                val dateTimeStr = parts[2].trim()
                val dateTime = parseDateTime(dateTimeStr)
                if (dateTime == null) {
                    Log.w(TAG, "Строка $lineNumber: неверный формат даты: $dateTimeStr")
                    return null
                }
                Log.d(TAG, "Дата: $dateTime")
                
                // Парсим контекст
                val contextStr = parts[3].trim()
                val context = parseWeightContext(contextStr)
                if (context == null) {
                    Log.w(TAG, "Строка $lineNumber: неверный контекст: $contextStr")
                    return null
                }
                Log.d(TAG, "Контекст: $context")
                
                // Парсим заметку (может быть пустой)
                val note = if (parts.size > 4) {
                    parts.subList(4, parts.size).joinToString(",")
                        .replace("\"\"", "\"") // Заменяем двойные кавычки
                        .trim()
                } else {
                    ""
                }
                Log.d(TAG, "Заметка: $note")
                
                return WeightEntry(
                    id = id,
                    weight = weight,
                    dateTime = dateTime,
                    context = context,
                    note = note
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка парсинга строки $lineNumber: ${e.message}", e)
                return null
            }
        }
        
        /**
         * Парсинг даты и времени
         */
        private fun parseDateTime(dateTimeStr: String): Date? {
            return try {
                // Пробуем парсить с временем
                dateTimeFormatter.parse(dateTimeStr)
            } catch (e: Exception) {
                try {
                    // Пробуем парсить только дату
                    dateOnlyFormatter.parse(dateTimeStr)
                } catch (e2: Exception) {
                    Log.e(TAG, "Ошибка парсинга даты: $dateTimeStr", e2)
                    null
                }
            }
        }
        
        /**
         * Парсинг контекста веса
         */
        private fun parseWeightContext(contextStr: String): WeightContext? {
            return try {
                WeightContext.valueOf(contextStr)
            } catch (e: Exception) {
                // Пробуем найти по displayName
                WeightContext.values().find { it.displayName == contextStr }
            }
        }
        
        /**
         * Проверка валидности CSV файла
         */
        fun validateCSVFormat(inputStream: InputStream): Boolean {
            return try {
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val header = reader.readLine()
                reader.close()
                
                // Проверяем заголовок
                header?.contains("weight,dateTime,context") == true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Получить информацию о CSV файле
         */
        fun getCSVInfo(inputStream: InputStream): CSVFileInfo {
            return try {
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                var lineCount = 0
                var validLines = 0
                var line: String?
                
                // Читаем заголовок
                val header = reader.readLine()
                lineCount++
                Log.d(TAG, "Заголовок при проверке: $header")
                
                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    try {
                        line?.let {
                            Log.d(TAG, "Проверка строки $lineCount: $it")
                            if (parseCSVLine(it, lineCount) != null) {
                                validLines++
                                Log.d(TAG, "Строка $lineCount валидна")
                            } else {
                                Log.d(TAG, "Строка $lineCount невалидна")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при проверке строки $lineCount", e)
                    }
                }
                
                reader.close()
                
                Log.d(TAG, "Информация о файле: totalLines=$lineCount, validLines=$validLines, hasHeader=${header?.isNotEmpty() == true}")
                
                CSVFileInfo(
                    totalLines = lineCount - 1, // минус заголовок
                    validLines = validLines,
                    hasHeader = header?.isNotEmpty() == true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении информации о файле", e)
                CSVFileInfo(0, 0, false)
            }
        }
        
        data class CSVFileInfo(
            val totalLines: Int,
            val validLines: Int,
            val hasHeader: Boolean
        )
    }
}