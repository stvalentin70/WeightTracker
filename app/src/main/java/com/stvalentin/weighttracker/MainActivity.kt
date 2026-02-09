package com.stvalentin.weighttracker

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var profileButton: Button
    
    // НОВЫЕ ЭЛЕМЕНТЫ ДЛЯ ПРОФИЛЯ
    private lateinit var userNameTextView: TextView
    private lateinit var bmiTextView: TextView
    private lateinit var bmiCategoryTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var viewModel: WeightViewModel
    private lateinit var userProfileViewModel: UserProfileViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_enhanced)
        
        // Инициализация ViewModel для веса
        val repository = WeightRepository(WeightDatabase.getDatabase(this).weightDao())
        viewModel = ViewModelProvider(this, WeightViewModel.provideFactory(repository))
            .get(WeightViewModel::class.java)
        
        // Инициализация ViewModel для профиля
        val userProfileRepository = UserProfileRepository(WeightDatabase.getDatabase(this).userProfileDao())
        userProfileViewModel = ViewModelProvider(
            this, 
            UserProfileViewModelFactory(userProfileRepository)
        ).get(UserProfileViewModel::class.java)
        
        // Инициализация всех View элементов
        initializeViews()
        setupClickListeners()
        setupObservers()
    }
    
    private fun initializeViews() {
        weightTextView = findViewById(R.id.weightTextView)
        addButton = findViewById(R.id.addButton)
        historyButton = findViewById(R.id.historyButton)
        chartButton = findViewById(R.id.chartButton)
        profileButton = findViewById(R.id.settingsButton)
        
        // НОВЫЕ ЭЛЕМЕНТЫ
        userNameTextView = findViewById(R.id.userNameTextView)
        bmiTextView = findViewById(R.id.bmiTextView)
        bmiCategoryTextView = findViewById(R.id.bmiCategoryTextView)
        progressTextView = findViewById(R.id.progressTextView)
        caloriesTextView = findViewById(R.id.caloriesTextView)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupClickListeners() {
        addButton.setOnClickListener {
            showAddWeightDialog()
        }
        
        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
        
        chartButton.setOnClickListener {
            val intent = Intent(this, ChartActivity::class.java)
            startActivity(intent)
        }
        
        profileButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupObservers() {
        // Наблюдаем за последней записью веса
        viewModel.latestEntry.observe(this) { entry ->
            val currentWeight = entry?.weight
            weightTextView.text = if (currentWeight != null) 
                "${String.format(Locale.getDefault(), "%.1f", currentWeight)} кг" 
                else "-- кг"
            
            // При изменении веса обновляем расчеты
            userProfileViewModel.userProfile.value?.let { profile ->
                calculateAndDisplayMetrics(profile, currentWeight)
            }
        }
        
        // Наблюдаем за профилем пользователя
        userProfileViewModel.userProfile.observe(this) { profile ->
            profile?.let {
                // Отображаем имя пользователя
                userNameTextView.text = if (it.name.isNotEmpty()) it.name else "Гость"
                
                // Получаем текущий вес для расчетов
                val currentWeight = viewModel.latestEntry.value?.weight
                
                // Выполняем все расчеты
                calculateAndDisplayMetrics(it, currentWeight)
            }
        }
    }
    
    private fun calculateAndDisplayMetrics(profile: UserProfile, currentWeight: Double?) {
        // 1. Расчет ИМТ (если есть текущий вес и рост)
        if (currentWeight != null && profile.heightCm > 0) {
            val bmi = HealthCalculations.calculateBMI(currentWeight, profile.heightCm)
            val bmiCategory = HealthCalculations.getBMICategory(bmi)
            
            bmiTextView.text = String.format(Locale.getDefault(), "ИМТ: %.1f", bmi)
            bmiCategoryTextView.text = bmiCategory
            bmiCategoryTextView.setTextColor(HealthCalculations.getBMIColor(this, bmi))
        } else {
            bmiTextView.text = "ИМТ: --"
            bmiCategoryTextView.text = if (profile.heightCm <= 0) 
                "Введите рост в профиле" 
                else "Добавьте вес"
            bmiCategoryTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
        
        // 2. Прогресс к целевой массе
        if (currentWeight != null && profile.targetWeightKg > 0) {
            val progressResult = HealthCalculations.calculateProgress(
                currentWeight, 
                profile.startWeightKg, 
                profile.targetWeightKg
            )
            
            progressTextView.text = "Прогресс: ${progressResult.progressPercent}% (${progressResult.message})"
            progressBar.progress = progressResult.progressPercent
        } else {
            progressTextView.text = if (profile.targetWeightKg <= 0) 
                "Установите цель в профиле" 
                else "Добавьте вес"
            progressBar.progress = 0
        }
        
        // 3. Расчет суточной нормы калорий
        if (currentWeight != null && profile.heightCm > 0) {
            val age = HealthCalculations.calculateAge(profile.birthDate)
            val calories = HealthCalculations.calculateDailyCalories(
                weightKg = currentWeight,
                heightCm = profile.heightCm,
                age = age,
                gender = profile.gender,
                activityLevel = profile.activityLevel
            )
            caloriesTextView.text = String.format(Locale.getDefault(), "Норма: %d ккал/день", calories)
        } else {
            caloriesTextView.text = if (profile.heightCm <= 0) 
                "Заполните профиль" 
                else "Добавьте вес"
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
                val id = viewModel.addEntry(entry)
                val count = viewModel.getEntriesCount()
                
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                
                // Используем форматирование строки
                val message = String.format(
                    Locale.getDefault(),
                    "Добавлен вес: %.1f кг\n%s",
                    entry.weight,
                    sdf.format(entry.dateTime)
                )
                
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка при сохранении: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // При возвращении на экран обновляем данные
        userProfileViewModel.userProfile.value?.let { profile ->
            val currentWeight = viewModel.latestEntry.value?.weight
            calculateAndDisplayMetrics(profile, currentWeight)
        }
    }
}