package com.stvalentin.weighttracker

import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChartActivity : AppCompatActivity() {
    
    private lateinit var simpleLineChart: SimpleLineChartView
    private lateinit var chartTitleTextView: TextView
    private lateinit var emptyStateTextView: TextView
    private lateinit var averageWeightTextView: TextView
    private lateinit var periodSpinner: android.widget.Spinner
    private lateinit var viewModel: WeightViewModel
    private lateinit var hintContainer: View
    private lateinit var statsCard: View
    private lateinit var emptyStateCard: View
    
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val fullDateTimeFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val dayOfWeekFormatter = SimpleDateFormat("EEEE", Locale.getDefault())
    
    private var allEntries: List<WeightEntry> = emptyList()
    private var selectedPeriod = 0 // 0: все, 1: 7 дней, 2: 30 дней, 3: 90 дней
    private var currentFilteredEntries: List<WeightEntry> = emptyList()
    
    private var popupWindow: PopupWindow? = null
    
    companion object {
        private const val TAG = "ChartActivity"
        private const val PERIOD_ALL = 0
        private const val PERIOD_7_DAYS = 1
        private const val PERIOD_30_DAYS = 2
        private const val PERIOD_90_DAYS = 3
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)
        
        // Настройка ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.chart_title)
        
        simpleLineChart = findViewById(R.id.simpleLineChart)
        chartTitleTextView = findViewById(R.id.chartTitleTextView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        averageWeightTextView = findViewById(R.id.averageWeightTextView)
        periodSpinner = findViewById(R.id.periodSpinner)
        hintContainer = findViewById(R.id.hintContainer)
        statsCard = findViewById(R.id.statsCard)
        emptyStateCard = findViewById(R.id.emptyStateCard)
        
        // Инициализация ViewModel
        val repository = WeightRepository(WeightDatabase.getDatabase(this).weightDao())
        viewModel = ViewModelProvider(this, WeightViewModel.provideFactory(repository))
            .get(WeightViewModel::class.java)
        
        // Настройка Spinner
        setupPeriodSpinner()
        
        // Настройка слушателя кликов по графику
        setupChartTouchListener()
        
        // Начинаем наблюдать за данными
        setupObservers()
    }
    
    override fun onResume() {
        super.onResume()
        loadChartData()
    }
    
    override fun onPause() {
        super.onPause()
        // Закрываем попап при уходе с экрана
        popupWindow?.dismiss()
        popupWindow = null
    }
    
    private fun setupPeriodSpinner() {
        // Получаем массив периодов из ресурсов
        val periods = resources.getStringArray(R.array.chart_periods)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter
        
        periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPeriod = position
                Log.d(TAG, "Выбран период: $position - ${periods[position]}")
                applyPeriodFilter()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ничего не выбрано
            }
        }
    }
    
    private fun setupChartTouchListener() {
        simpleLineChart.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Получаем координаты касания
                    val touchX = event.x
                    val touchY = event.y
                    
                    // Ищем ближайшую точку
                    val nearestIndex = simpleLineChart.findNearestPoint(touchX, touchY)
                    
                    nearestIndex?.let { index ->
                        if (index in currentFilteredEntries.indices) {
                            val entry = currentFilteredEntries[index]
                            val point = simpleLineChart.getDataPoints()[index]
                            showTooltip(point.x, point.y, entry)
                        }
                    }
                    
                    // Скрываем подсказку после первого касания
                    hintContainer.visibility = View.GONE
                    
                    true
                }
                else -> false
            }
        }
    }
    
    private fun showTooltip(x: Float, y: Float, entry: WeightEntry) {
        // Закрываем предыдущий попап
        popupWindow?.dismiss()
        
        // Создаем содержимое попапа
        val popupView = layoutInflater.inflate(R.layout.popup_chart_point, null)
        
        val dateText = popupView.findViewById<TextView>(R.id.popupDateText)
        val weightText = popupView.findViewById<TextView>(R.id.popupWeightText)
        val contextText = popupView.findViewById<TextView>(R.id.popupContextText)
        val noteText = popupView.findViewById<TextView>(R.id.popupNoteText)
        val noteContainer = popupView.findViewById<View>(R.id.noteContainer)
        
        // Форматируем дату
        val dayOfWeek = dayOfWeekFormatter.format(entry.dateTime)
        val date = dateFormatter.format(entry.dateTime)
        dateText.text = getString(R.string.popup_date, date, dayOfWeek)
        
        // Вес
        weightText.text = getString(R.string.popup_weight, entry.weight)
        
        // Контекст
        contextText.text = getString(R.string.popup_context, entry.context.displayName)
        
        // Заметка (если есть)
        if (entry.note.isNotEmpty()) {
            noteText.text = getString(R.string.popup_note, entry.note)
            noteContainer.visibility = View.VISIBLE
        } else {
            noteContainer.visibility = View.GONE
        }
        
        // Измеряем размеры контента
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        // Создаем попап
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            // Настройки стиля
            setBackgroundDrawable(getDrawable(R.drawable.popup_background))
            elevation = resources.getDimension(R.dimen.popup_elevation)
            
            // Показываем попап
            val location = IntArray(2)
            simpleLineChart.getLocationOnScreen(location)
            val chartX = location[0]
            val chartY = location[1]
            
            // Корректируем позицию, чтобы попап был около точки
            val popupX = chartX + x - popupView.measuredWidth / 2
            var popupY = chartY + y - popupView.measuredHeight - 50
            
            // Если попап выходит за верхнюю границу, показываем снизу
            if (popupY < 0) {
                popupY = chartY + y + 50
            }
            
            // Гарантируем, что попап не выходит за экран справа
            val maxX = resources.displayMetrics.widthPixels - popupView.measuredWidth - 20
            val finalX = popupX.coerceIn(20f, maxX.toFloat())
            
            showAtLocation(simpleLineChart, android.view.Gravity.NO_GRAVITY, finalX.toInt(), popupY.toInt())
            
            // Автоматически закрываем через 3 секунды
            popupView.postDelayed({
                dismiss()
                popupWindow = null
            }, 3000)
        }
        
        // Закрываем попап при клике
        popupView.setOnClickListener {
            popupWindow?.dismiss()
            popupWindow = null
        }
    }
    
    private fun setupObservers() {
        // Наблюдаем за всеми записями
        viewModel.allEntries.observe(this) { entries ->
            Log.d(TAG, "Данные получены через LiveData. Всего записей: ${entries.size}")
            allEntries = entries
            applyPeriodFilter()
        }
    }
    
    private fun applyPeriodFilter() {
        if (allEntries.isEmpty()) {
            Log.d(TAG, "Нет данных для фильтрации")
            showEmptyState(getString(R.string.empty_state_no_data_for_period))
            return
        }
        
        // Сортируем по дате (от старых к новым)
        val sortedEntries = allEntries.sortedBy { it.dateTime }
        
        // Фильтруем по выбранному периоду
        currentFilteredEntries = when (selectedPeriod) {
            PERIOD_ALL -> sortedEntries
            PERIOD_7_DAYS -> getEntriesForPeriod(sortedEntries, 7)
            PERIOD_30_DAYS -> getEntriesForPeriod(sortedEntries, 30)
            PERIOD_90_DAYS -> getEntriesForPeriod(sortedEntries, 90)
            else -> sortedEntries
        }
        
        Log.d(TAG, "После фильтрации: ${currentFilteredEntries.size} записей")
        
        if (currentFilteredEntries.isEmpty()) {
            Log.d(TAG, "Нет данных для выбранного периода")
            showEmptyState(getString(R.string.empty_state_no_data_for_period))
            return
        }
        
        if (currentFilteredEntries.size < 2) {
            Log.d(TAG, "Недостаточно данных для графика")
            showEmptyState(getString(R.string.chart_not_enough_data))
            return
        }
        
        // Обновляем график
        updateChart(currentFilteredEntries)
    }
    
    private fun getEntriesForPeriod(entries: List<WeightEntry>, days: Int): List<WeightEntry> {
        if (entries.isEmpty()) return emptyList()
        
        val calendar = Calendar.getInstance()
        val endDate = entries.last().dateTime // Самая последняя дата
        calendar.time = endDate
        calendar.add(Calendar.DAY_OF_YEAR, -days + 1) // -days + 1 чтобы включить последний день
        val startDate = calendar.time
        
        Log.d(TAG, "Фильтр: с ${dateFormatter.format(startDate)} по ${dateFormatter.format(endDate)}")
        
        // Фильтруем записи за период
        val periodEntries = entries.filter { it.dateTime >= startDate }
        
        // Если для какого-то дня нет записи, берем ближайшую доступную
        val result = mutableListOf<WeightEntry>()
        calendar.time = startDate
        
        for (i in 0 until days) {
            val currentDay = calendar.time
            
            // Ищем запись для этого дня
            val dayEntry = periodEntries.filter { isSameDay(it.dateTime, currentDay) }
                .sortedByDescending { it.dateTime } // Берем последнюю запись за день
                .firstOrNull()
            
            // Если для дня нет записи, ищем ближайшую
            if (dayEntry == null && periodEntries.isNotEmpty()) {
                val nearestEntry = findNearestEntryForDay(periodEntries, currentDay)
                nearestEntry?.let { result.add(it) }
            } else if (dayEntry != null) {
                result.add(dayEntry)
            }
            
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return result.distinctBy { isSameDay(it.dateTime, Date()) } // Убираем дубликаты за один день
    }
    
    private fun findNearestEntryForDay(entries: List<WeightEntry>, targetDate: Date): WeightEntry? {
        return entries.minByOrNull { Math.abs(it.dateTime.time - targetDate.time) }
    }
    
    private fun updateChart(entries: List<WeightEntry>) {
        // Преобразуем вес в Float для графика
        val weights = entries.map { it.weight.toFloat() }
        val dates = entries.map { it.dateTime }
        
        Log.d(TAG, "Веса для графика: $weights")
        Log.d(TAG, "Даты для графика: ${dates.map { fullDateTimeFormatter.format(it) }}")
        
        // Устанавливаем данные в график
        simpleLineChart.setData(weights, dates)
        
        // Вычисляем статистику
        val averageWeight = weights.average()
        val minWeight = weights.min()
        val maxWeight = weights.max()
        val firstDate = entries.first().dateTime
        val lastDate = entries.last().dateTime
        
        // Определяем период
        val periodText = when (selectedPeriod) {
            PERIOD_ALL -> getString(R.string.period_all)
            PERIOD_7_DAYS -> getString(R.string.period_7_days)
            PERIOD_30_DAYS -> getString(R.string.period_30_days)
            PERIOD_90_DAYS -> getString(R.string.period_90_days)
            else -> getString(R.string.chart_title)
        }
        
        // Форматируем статистику
        val statsText = """
            ${getString(R.string.chart_stats_period, periodText)}
            ${getString(R.string.chart_stats_average, averageWeight)}
            ${getString(R.string.chart_stats_min, minWeight)}
            ${getString(R.string.chart_stats_max, maxWeight)}
            ${getString(R.string.chart_stats_count, entries.size)}
            ${getString(R.string.chart_stats_date_range, dateFormatter.format(firstDate), dateFormatter.format(lastDate))}
        """.trimIndent()
        
        runOnUiThread {
            averageWeightTextView.text = statsText
            
            // Обновляем заголовок
            chartTitleTextView.text = getString(R.string.chart_title)
            
            // Показываем график
            Log.d(TAG, "Показываем график")
            showChart()
            
            // Показываем подсказку только если есть данные
            if (entries.size >= 2) {
                hintContainer.visibility = View.VISIBLE
            }
        }
    }
    
    private fun loadChartData() {
        lifecycleScope.launch {
            Log.d(TAG, "Начинаем загрузку данных для графика...")
            
            // Для отладки: получаем количество записей напрямую
            val count = viewModel.getEntriesCount()
            Log.d(TAG, "Количество записей в базе (прямой запрос): $count")
            
            // Запрашиваем обновление данных - LiveData автоматически обновит наблюдателя
            // Можно также принудительно обновить данные, если они не приходят:
            if (count > 0 && viewModel.allEntries.value.isNullOrEmpty()) {
                Log.w(TAG, "LiveData пуста, но записи есть в базе. Пробуем обновить...")
            }
        }
    }
    
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
    
    private fun showChart() {
        simpleLineChart.visibility = View.VISIBLE
        chartTitleTextView.visibility = View.VISIBLE
        statsCard.visibility = View.VISIBLE
        periodSpinner.visibility = View.VISIBLE
        emptyStateCard.visibility = View.GONE
    }
    
    private fun showEmptyState(message: String) {
        runOnUiThread {
            simpleLineChart.visibility = View.GONE
            chartTitleTextView.visibility = View.VISIBLE
            statsCard.visibility = View.GONE
            periodSpinner.visibility = View.VISIBLE
            hintContainer.visibility = View.GONE
            
            emptyStateCard.visibility = View.VISIBLE
            emptyStateTextView.text = message
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}