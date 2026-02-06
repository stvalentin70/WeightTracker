package com.stvalentin.weighttracker

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var adapter: WeightEntryAdapter
    private lateinit var viewModel: WeightViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        // Настройка ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "История измерений"
        
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        
        // Инициализация ViewModel
        val repository = WeightRepository(WeightDatabase.getDatabase(this).weightDao())
        viewModel = ViewModelProvider(this, WeightViewModel.provideFactory(repository))
            .get(WeightViewModel::class.java)
        
        setupRecyclerView()
        setupObservers()
    }
    
    private fun setupRecyclerView() {
        adapter = WeightEntryAdapter(
            onDeleteClick = { entry ->
                showDeleteConfirmation(entry)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupObservers() {
        viewModel.allEntries.observe(this) { entries ->
            if (entries.isEmpty()) {
                recyclerView.visibility = android.view.View.GONE
                emptyStateTextView.visibility = android.view.View.VISIBLE
                emptyStateTextView.text = "История пуста\nДобавьте первую запись на главном экране"
            } else {
                recyclerView.visibility = android.view.View.VISIBLE
                emptyStateTextView.visibility = android.view.View.GONE
                adapter.updateEntries(entries)
            }
        }
    }
    
    private fun showDeleteConfirmation(entry: WeightEntry) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Удаление записи")
            .setMessage("Удалить запись ${entry.weight} кг от " +
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(entry.dateTime) + "?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteEntry(entry)
                Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}