package com.stvalentin.weighttracker

import android.os.Bundle
import android.util.Log
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
    private lateinit var viewModel: WeightViewModel
    
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val fullDateTimeFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    companion object {
        private const val TAG = "ChartActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)
        
        // Настройка ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "График веса"
        
        simpleLineChart = findViewById(R.id.simpleLineChart)
        chartTitleTextView = findViewById(R.id.chartTitleTextView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        averageWeightTextView = findViewById(R.id.averageWeightTextView)
        
        // Инициализация ViewModel
        val repository = WeightRepository(WeightDatabase.getDatabase(this).weightDao())
        viewModel = ViewModelProvider(this, WeightViewModel.provideFactory(repository))
            .get(WeightViewModel::class.java)
        
        // Начинаем наблюдать за данными
        setupObservers()
    }
    
    override fun onResume() {
        super.onResume()
        loadChartData()
    }
    
    private fun setupObservers() {
        // Наблюдаем за всеми записями
        viewModel.allEntries.observe(this) { allEntries ->
            Log.d(TAG, "Данные получены через LiveData. Всего записей: ${allEntries.size}")
            
            if (allEntries.isEmpty()) {
                Log.d(TAG, "Нет данных для графика")
                showEmptyState("Нет данных для графика")
                return@observe
            }
            
            // Выводим все записи для отладки
            allEntries.forEachIndexed { index, entry ->
                Log.d(TAG, "Запись $index: ${entry.weight} кг, ${fullDateTimeFormatter.format(entry.dateTime)}")
            }
            
            // Сортируем по дате (от старых к новым для графика)
            val sortedEntries = allEntries.sortedBy { it.dateTime }
            
            // Берем все записи (или можно ограничить, например, последние 10)
            val entriesToShow = sortedEntries // Все записи
            
            Log.d(TAG, "Записей для графика: ${entriesToShow.size}")
            
            if (entriesToShow.size < 2) {
                Log.d(TAG, "Недостаточно данных для графика")
                showEmptyState("Недостаточно данных для графика\nНужно минимум 2 записи")
                return@observe
            }
            
            // Преобразуем вес в Float для графика
            val weights = entriesToShow.map { it.weight.toFloat() }
            val dates = entriesToShow.map { it.dateTime }
            
            Log.d(TAG, "Веса для графика: $weights")
            Log.d(TAG, "Даты для графика: ${dates.map { fullDateTimeFormatter.format(it) }}")
            
            // Устанавливаем данные в график
            simpleLineChart.setData(weights, dates)
            
            // Вычисляем статистику
            val averageWeight = weights.average()
            val minWeight = weights.min()
            val maxWeight = weights.max()
            val firstDate = entriesToShow.first().dateTime
            val lastDate = entriesToShow.last().dateTime
            
            // Определяем период
            val periodText = if (isSameDay(firstDate, lastDate)) {
                "За ${dateFormatter.format(firstDate)}"
            } else {
                "С ${dateFormatter.format(firstDate)} по ${dateFormatter.format(lastDate)}"
            }
            
            // Отображаем статистику
            val statsText = """
                $periodText
                Средний вес: ${String.format(Locale.getDefault(), "%.1f", averageWeight)} кг
                Мин: ${String.format(Locale.getDefault(), "%.1f", minWeight)} кг
                Макс: ${String.format(Locale.getDefault(), "%.1f", maxWeight)} кг
                Всего записей: ${entriesToShow.size}
            """.trimIndent()
            
            runOnUiThread {
                averageWeightTextView.text = statsText
                
                // Обновляем заголовок
                val title = if (entriesToShow.size == allEntries.size) {
                    "Все записи (${allEntries.size})"
                } else {
                    "Последние ${entriesToShow.size} записей"
                }
                chartTitleTextView.text = title
                
                // Показываем график
                Log.d(TAG, "Показываем график")
                showChart()
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
        simpleLineChart.visibility = android.view.View.VISIBLE
        chartTitleTextView.visibility = android.view.View.VISIBLE
        averageWeightTextView.visibility = android.view.View.VISIBLE
        emptyStateTextView.visibility = android.view.View.GONE
    }
    
    private fun showEmptyState(message: String) {
        runOnUiThread {
            simpleLineChart.visibility = android.view.View.GONE
            chartTitleTextView.visibility = android.view.View.GONE
            averageWeightTextView.visibility = android.view.View.GONE
            emptyStateTextView.visibility = android.view.View.VISIBLE
            emptyStateTextView.text = message
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}