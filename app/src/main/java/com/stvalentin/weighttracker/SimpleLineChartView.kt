package com.stvalentin.weighttracker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.max
import kotlin.math.min
import java.text.SimpleDateFormat
import java.util.*

class SimpleLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "SimpleLineChartView"
    }
    
    private val dataPoints = mutableListOf<Float>()
    private val dates = mutableListOf<Date>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val emptyStatePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var minValue: Float = 0f
    private var maxValue: Float = 100f
    private var padding: Float = 80f
    
    private val pointPositions = mutableListOf<PointF>()
    
    // Форматтеры дат
    private val dateFormatter = SimpleDateFormat("dd.MM", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullDateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    init {
        Log.d(TAG, "SimpleLineChartView initialized")
        
        // Настройка рисования линии
        paint.color = Color.parseColor("#2196F3")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        
        // Настройка рисования точек
        pointPaint.color = Color.parseColor("#2196F3")
        pointPaint.style = Paint.Style.FILL
        
        // Настройка рисования сетки
        gridPaint.color = Color.parseColor("#E0E0E0")
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f
        
        // Настройка рисования осей
        axisPaint.color = Color.BLACK
        axisPaint.style = Paint.Style.STROKE
        axisPaint.strokeWidth = 2f
        
        // Настройка текста
        textPaint.color = Color.DKGRAY
        textPaint.textSize = 32f
        textPaint.textAlign = Paint.Align.CENTER
        
        // Настройка для сообщения об отсутствии данных
        emptyStatePaint.color = Color.GRAY
        emptyStatePaint.textSize = 36f
        emptyStatePaint.textAlign = Paint.Align.CENTER
    }
    
    fun setData(weights: List<Float>, entryDates: List<Date>) {
        Log.d(TAG, "setData called with ${weights.size} weights and ${entryDates.size} dates")
        Log.d(TAG, "Weights: $weights")
        
        if (weights.isEmpty()) {
            Log.e(TAG, "ОШИБКА: Пустой список весов!")
        }
    
        if (entryDates.isEmpty()) {
            Log.e(TAG, "ОШИБКА: Пустой список дат!")
        }

        if (entryDates.isNotEmpty()) {
            Log.d(TAG, "Dates: ${entryDates.map { fullDateFormatter.format(it) }}")
        }
        
        dataPoints.clear()
        dates.clear()
        pointPositions.clear()
        
        dataPoints.addAll(weights)
        dates.addAll(entryDates)
        
        if (weights.isNotEmpty()) {
            // Добавляем небольшой запас сверху и снизу
            val minWeight = weights.min()
            val maxWeight = weights.max()
            val range = maxWeight - minWeight
            
            Log.d(TAG, "Min weight: $minWeight, Max weight: $maxWeight, Range: $range")
            
            minValue = minWeight - range * 0.1f
            maxValue = maxWeight + range * 0.1f
            
            // Гарантируем, что значения не будут отрицательными
            if (minValue < 0) minValue = 0f
            
            // Если все значения одинаковые, делаем небольшой диапазон
            if (maxValue - minValue < 0.5f) {
                minValue = minWeight - 0.5f
                maxValue = maxWeight + 0.5f
            }
            
            Log.d(TAG, "Min value: $minValue, Max value: $maxValue")
        } else {
            minValue = 0f
            maxValue = 100f
            Log.d(TAG, "No weights, using default values: min=$minValue, max=$maxValue")
        }
        
        Log.d(TAG, "Invalidating view...")
        invalidate()
    }
    
    fun getDataPoints(): List<PointF> {
        return pointPositions.toList()
    }
    
    fun getDataAtPosition(position: Int): Pair<Float, Date>? {
        return if (position in dataPoints.indices && position in dates.indices) {
            Pair(dataPoints[position], dates[position])
        } else {
            null
        }
    }
    
    fun findNearestPoint(x: Float, y: Float): Int? {
        if (pointPositions.isEmpty()) return null
        
        var nearestIndex: Int? = null
        var minDistance = Float.MAX_VALUE
        
        pointPositions.forEachIndexed { index, point ->
            val distance = kotlin.math.sqrt(
                (point.x - x) * (point.x - x) + (point.y - y) * (point.y - y)
            )
            
            if (distance < minDistance && distance < 100f) {
                minDistance = distance
                nearestIndex = index
            }
        }
        
        return nearestIndex
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        Log.d(TAG, "onDraw called. View dimensions: ${width}x${height}")
        Log.d(TAG, "dataPoints size: ${dataPoints.size}, dates size: ${dates.size}")
        
        if (dataPoints.size < 2) {
            // Рисуем сообщение об отсутствии данных
            Log.d(TAG, "Not enough data points: ${dataPoints.size}. Showing empty state message.")
            
            // Рисуем фон
            canvas.drawColor(Color.WHITE)
            
            // Сообщение о недостатке данных
            canvas.drawText(
                "Недостаточно данных для графика",
                width / 2f,
                height / 2f - 20f,
                emptyStatePaint
            )
            
            emptyStatePaint.textSize = 28f
            canvas.drawText(
                "Нужно минимум 2 записи",
                width / 2f,
                height / 2f + 40f,
                emptyStatePaint
            )
            return
        }
        
        Log.d(TAG, "Drawing chart with ${dataPoints.size} points")
        
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        val valueRange = maxValue - minValue
        
        Log.d(TAG, "Chart dimensions: width=$chartWidth, height=$chartHeight")
        Log.d(TAG, "Value range: min=$minValue, max=$maxValue, range=$valueRange")
        
        // Рисуем фон
        canvas.drawColor(Color.WHITE)
        
        // Рисуем сетку
        drawGrid(canvas, chartWidth, chartHeight)
        
        // Рисуем оси
        drawAxes(canvas, chartWidth, chartHeight)
        
        // Рисуем ось Y (вес)
        drawYAxis(canvas, chartHeight)
        
        // Рисуем ось X (даты)
        drawXAxis(canvas, chartWidth, chartHeight)
        
        // Очищаем список позиций точек
        pointPositions.clear()
        
        // Рассчитываем точки
        for (i in dataPoints.indices) {
            val x = padding + (i * chartWidth / (dataPoints.size - 1))
            val normalizedY = (dataPoints[i] - minValue) / valueRange
            val y = padding + chartHeight - (normalizedY * chartHeight)
            pointPositions.add(PointF(x, y))
            
            Log.d(TAG, "Point $i: weight=${dataPoints[i]}, x=$x, y=$y, normalizedY=$normalizedY")
        }
        
        // Рисуем линию
        if (pointPositions.size >= 2) {
            Log.d(TAG, "Drawing line connecting ${pointPositions.size} points")
            for (i in 0 until pointPositions.size - 1) {
                canvas.drawLine(
                    pointPositions[i].x,
                    pointPositions[i].y,
                    pointPositions[i + 1].x,
                    pointPositions[i + 1].y,
                    paint
                )
            }
        }
        
        // Рисуем точки
        pointPositions.forEachIndexed { index, point ->
            Log.d(TAG, "Drawing point at (${point.x}, ${point.y})")
            // Большая внешняя точка
            canvas.drawCircle(point.x, point.y, 12f, pointPaint)
            // Маленькая внутренняя белая точка
            canvas.drawCircle(point.x, point.y, 6f, Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            })
            
            // Подпись значения над точкой
            textPaint.textSize = 24f
            textPaint.color = Color.parseColor("#2196F3")
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                String.format(Locale.getDefault(), "%.1f", dataPoints[index]),
                point.x,
                point.y - 20f,
                textPaint
            )
        }
        
        Log.d(TAG, "Chart drawing completed")
    }
    
    private fun drawGrid(canvas: Canvas, chartWidth: Float, chartHeight: Float) {
        Log.d(TAG, "Drawing grid")
        // Горизонтальные линии (основные)
        for (i in 0..5) {
            val y = padding + (i * chartHeight / 5)
            canvas.drawLine(padding, y, padding + chartWidth, y, gridPaint)
        }
        
        // Вертикальные линии (основные)
        for (i in 0..5) {
            val x = padding + (i * chartWidth / 5)
            canvas.drawLine(x, padding, x, padding + chartHeight, gridPaint)
        }
    }
    
    private fun drawAxes(canvas: Canvas, chartWidth: Float, chartHeight: Float) {
        Log.d(TAG, "Drawing axes")
        // Ось X
        canvas.drawLine(
            padding,
            padding + chartHeight,
            padding + chartWidth,
            padding + chartHeight,
            axisPaint
        )
        
        // Ось Y
        canvas.drawLine(
            padding,
            padding,
            padding,
            padding + chartHeight,
            axisPaint
        )
    }
    
    private fun drawYAxis(canvas: Canvas, chartHeight: Float) {
        Log.d(TAG, "Drawing Y axis")
        textPaint.textSize = 28f
        textPaint.color = Color.BLACK
        textPaint.textAlign = Paint.Align.RIGHT
        
        // Подписи оси Y (вес)
        for (i in 0..5) {
            val value = minValue + (i * (maxValue - minValue) / 5)
            val y = padding + chartHeight - (i * chartHeight / 5)
            
            canvas.drawText(
                String.format(Locale.getDefault(), "%.1f", value),
                padding - 15f,
                y + 10f,
                textPaint
            )
        }
        
        // Заголовок оси Y
        textPaint.textSize = 32f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.save()
        canvas.rotate(-90f, padding - 50f, height / 2f)
        canvas.drawText("Вес (кг)", padding - 50f, height / 2f, textPaint)
        canvas.restore()
    }
    
    private fun drawXAxis(canvas: Canvas, chartWidth: Float, chartHeight: Float) {
        Log.d(TAG, "Drawing X axis")
        textPaint.textSize = 28f
        textPaint.color = Color.BLACK
        textPaint.textAlign = Paint.Align.CENTER
        
        if (dates.isNotEmpty()) {
            Log.d(TAG, "Dates available for X axis: ${dates.size}")
            // Проверяем, все ли даты в один день
            val firstDate = dates.first()
            val allSameDay = dates.all { 
                val cal1 = Calendar.getInstance().apply { time = firstDate }
                val cal2 = Calendar.getInstance().apply { time = it }
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
            }
            
            Log.d(TAG, "All dates same day: $allSameDay")
            
            if (allSameDay) {
                Log.d(TAG, "Showing time for single day")
                // Показываем время для одного дня
                if (dates.size <= 5) {
                    for (i in dates.indices) {
                        val x = padding + (i * chartWidth / (dates.size - 1))
                        val y = padding + chartHeight + 40f
                        canvas.drawText(timeFormatter.format(dates[i]), x, y, textPaint)
                    }
                } else {
                    for (i in 0..4) {
                        val index = i * (dates.size - 1) / 4
                        val x = padding + (i * chartWidth / 4)
                        val y = padding + chartHeight + 40f
                        canvas.drawText(timeFormatter.format(dates[index]), x, y, textPaint)
                    }
                }
            } else {
                Log.d(TAG, "Showing dates")
                // Показываем даты
                if (dates.size <= 5) {
                    for (i in dates.indices) {
                        val x = padding + (i * chartWidth / (dates.size - 1))
                        val y = padding + chartHeight + 40f
                        canvas.drawText(dateFormatter.format(dates[i]), x, y, textPaint)
                    }
                } else {
                    for (i in 0..4) {
                        val index = i * (dates.size - 1) / 4
                        val x = padding + (i * chartWidth / 4)
                        val y = padding + chartHeight + 40f
                        canvas.drawText(dateFormatter.format(dates[index]), x, y, textPaint)
                    }
                }
            }
        } else {
            Log.d(TAG, "No dates available for X axis")
        }
        
        // Заголовок оси X
        textPaint.textSize = 32f
        val xAxisTitle = if (dates.isNotEmpty() && dates.all { 
            val cal = Calendar.getInstance().apply { time = it }
            val calNow = Calendar.getInstance()
            cal.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
        }) "Время" else "Даты"
        
        Log.d(TAG, "X axis title: $xAxisTitle")
        canvas.drawText(
            xAxisTitle,
            padding + chartWidth / 2,
            height - 20f,
            textPaint
        )
    }
}