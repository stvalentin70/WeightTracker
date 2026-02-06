package com.stvalentin.weighttracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), AddWeightDialogFragment.OnWeightAddedListener {
    
    private lateinit var weightTextView: TextView
    private lateinit var addButton: Button
    private lateinit var historyButton: Button
    private lateinit var chartButton: Button
    private lateinit var settingsButton: Button
    
    private lateinit var viewModel: WeightViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Инициализация ViewModel
        val repository = WeightRepository(WeightDatabase.getDatabase(this).weightDao())
        viewModel = ViewModelProvider(this, WeightViewModel.provideFactory(repository))
            .get(WeightViewModel::class.java)
        
        weightTextView = findViewById(R.id.weightTextView)
        addButton = findViewById(R.id.addButton)
        historyButton = findViewById(R.id.historyButton)
        chartButton = findViewById(R.id.chartButton)
        settingsButton = findViewById(R.id.settingsButton)
        
        setupClickListeners()
        setupObservers()
    }
    
    private fun setupClickListeners() {
        addButton.setOnClickListener {
            showAddWeightDialog()
        }
        
        historyButton.setOnClickListener {
            // Запускаем HistoryActivity
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
        
        chartButton.setOnClickListener {
            // Запускаем ChartActivity
            val intent = Intent(this, ChartActivity::class.java)
            startActivity(intent)
        }
        
        settingsButton.setOnClickListener {
            showSettings()
        }
    }
    
    private fun setupObservers() {
        // Наблюдаем за последней записью
        viewModel.latestEntry.observe(this) { entry ->
            if (entry != null) {
                weightTextView.text = "${entry.weight} кг"
            } else {
                weightTextView.text = "-- кг"
            }
        }
    }
    
    private fun showAddWeightDialog() {
        val dialog = AddWeightDialogFragment()
        dialog.setOnWeightAddedListener(this)
        dialog.show(supportFragmentManager, "AddWeightDialog")
    }
    
    override fun onWeightAdded(entry: WeightEntry) {
        lifecycleScope.launch {
            try {
                // Сохраняем запись через ViewModel
                val id = viewModel.addEntry(entry)
                
                // Для отладки: проверить количество записей
                val count = viewModel.getEntriesCount()
                
                // Показываем уведомление
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Добавлен вес: ${entry.weight} кг\n${sdf.format(entry.dateTime)}\nID: $id\nВсего записей: $count",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("MainActivity", "Запись добавлена. ID: $id, Всего записей: $count")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка при сохранении: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("MainActivity", "Ошибка сохранения", e)
                }
            }
        }
    }
    
    private fun showSettings() {
        lifecycleScope.launch {
            val count = viewModel.getEntriesCount()
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Настройки профиля\nВсего записей в базе: $count",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}