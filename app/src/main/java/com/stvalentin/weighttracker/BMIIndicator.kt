package com.stvalentin.weighttracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class BMIIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bmiValue: Float = 0f
    private val segmentColors = mutableListOf<Int>()
    private val segmentValues = listOf(16f, 18.5f, 25f, 30f, 35f, 40f)
    private val segmentPercentages = mutableListOf<Float>()
    
    private val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    init {
        initColors()
        initPaints()
        calculateSegmentPercentages()
    }
    
    private fun initColors() {
        segmentColors.clear()
        segmentColors.add(ContextCompat.getColor(context, R.color.bmi_scale_severe))   // <16
        segmentColors.add(ContextCompat.getColor(context, R.color.bmi_scale_thin))     // 16-18.5
        segmentColors.add(ContextCompat.getColor(context, R.color.bmi_scale_normal))   // 18.5-25
        segmentColors.add(ContextCompat.getColor(context, R.color.bmi_scale_overweight)) // 25-30
        segmentColors.add(ContextCompat.getColor(context, R.color.bmi_scale_obesity1))   // 30-35
        segmentColors.add(ContextCompat.getColor(context, R.color.bmi_scale_obesity2))   // 35-40
        segmentColors.add(ContextCompat.getColor(context, R.color.bmi_scale_obesity3))   // >40
    }
    
    private fun initPaints() {
        scalePaint.style = Paint.Style.FILL
        scalePaint.strokeWidth = 0f
        
        indicatorPaint.style = Paint.Style.FILL
        indicatorPaint.color = ContextCompat.getColor(context, R.color.indicator_current)
        indicatorPaint.strokeWidth = 4f
        
        textPaint.style = Paint.Style.FILL
        textPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        textPaint.textSize = resources.getDimension(R.dimen.bmi_label_text_size)
        textPaint.textAlign = Paint.Align.CENTER
        
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = ContextCompat.getColor(context, R.color.scale_background)
    }
    
    private fun calculateSegmentPercentages() {
        segmentPercentages.clear()
        var total = 0f
        
        // Рассчитываем проценты для каждого сегмента
        for (i in 0 until segmentValues.size + 1) {
            val percentage = when (i) {
                0 -> 5f  // <16
                1 -> 7.5f  // 16-18.5
                2 -> 20f   // 18.5-25
                3 -> 15f   // 25-30
                4 -> 15f   // 30-35
                5 -> 15f   // 35-40
                6 -> 22.5f // >40
                else -> 0f
            }
            segmentPercentages.add(percentage)
            total += percentage
        }
    }
    
    fun setBMI(value: Float) {
        this.bmiValue = value.coerceIn(0f, 100f)
        invalidate()
    }
    
    private fun getIndicatorPosition(): Float {
        return when {
            bmiValue < 16f -> 0f
            bmiValue > 40f -> 100f
            else -> ((bmiValue - 16) / (40 - 16) * 100).coerceIn(0f, 100f)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val scaleHeight = resources.getDimension(R.dimen.bmi_scale_height)
        
        // Рисуем фон шкалы
        val bgRect = RectF(0f, (height - scaleHeight) / 2, width, (height + scaleHeight) / 2)
        canvas.drawRoundRect(bgRect, 
            resources.getDimension(R.dimen.bmi_scale_corner_radius),
            resources.getDimension(R.dimen.bmi_scale_corner_radius),
            bgPaint
        )
        
        // Рисуем сегменты
        var currentX = 0f
        val segmentCount = segmentPercentages.size
        
        for (i in 0 until segmentCount) {
            val segmentWidth = width * segmentPercentages[i] / 100
            val segmentRect = RectF(
                currentX,
                (height - scaleHeight) / 2,
                currentX + segmentWidth,
                (height + scaleHeight) / 2
            )
            
            scalePaint.color = segmentColors[i]
            canvas.drawRect(segmentRect, scalePaint)
            
            currentX += segmentWidth
        }
        
        // Рисуем индикатор если есть значение ИМТ
        if (bmiValue > 0) {
            val indicatorPos = getIndicatorPosition()
            val indicatorX = width * indicatorPos / 100
            
            val indicatorHeight = resources.getDimension(R.dimen.bmi_indicator_height)
            val indicatorWidth = resources.getDimension(R.dimen.bmi_indicator_width)
            
            canvas.drawRect(
                indicatorX - indicatorWidth / 2,
                (height - indicatorHeight) / 2,
                indicatorX + indicatorWidth / 2,
                (height + indicatorHeight) / 2,
                indicatorPaint
            )
            
            // Рисуем подписи
            drawLabels(canvas, width, height)
        }
    }
    
    private fun drawLabels(canvas: Canvas, width: Float, height: Float) {
        val textY = height + resources.getDimension(R.dimen.bmi_label_text_size) + 8
        
        // Рисуем основные метки
        val labels = listOf("16", "18.5", "25", "30", "35", "40")
        val labelPositions = listOf(0f, 5f, 12.5f, 32.5f, 47.5f, 62.5f, 77.5f)
        
        for (i in labels.indices) {
            val x = width * labelPositions[i] / 100
            canvas.drawText(labels[i], x, textY, textPaint)
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = resources.getDimension(R.dimen.bmi_indicator_height) + 
                           resources.getDimension(R.dimen.bmi_label_text_size) * 2 + 16
        
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        val measuredHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> height
            MeasureSpec.AT_MOST -> desiredHeight.toInt().coerceAtMost(height)
            else -> desiredHeight.toInt()
        }
        
        setMeasuredDimension(width, measuredHeight)
    }
}