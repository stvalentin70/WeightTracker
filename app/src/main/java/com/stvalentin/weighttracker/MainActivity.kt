package com.stvalentin.weighttracker

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    
    private lateinit var userNameTextView: TextView
    private lateinit var bmiTextView: TextView
    private lateinit var bmiCategoryTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var progressBar: ProgressBar
    
    // Элементы шкалы ИМТ
    private lateinit var bmiScaleContainer: LinearLayout
    private lateinit var bmiIndicator: View
    
    private lateinit var viewModel: WeightViewModel
    private lateinit var userProfileViewModel: UserProfileViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_enhanced)
        
        val repository = WeightRepository(WeightDatabase.getDatabase(this).weightDao())
        viewModel = ViewModelProvider(this, WeightViewModel.provideFactory(repository))
            .get(WeightViewModel::class.java)
        
        val userProfileRepository = UserProfileRepository(WeightDatabase.getDatabase(this).userProfileDao())
        userProfileViewModel = ViewModelProvider(
            this, 
            UserProfileViewModelFactory(userProfileRepository)
        ).get(UserProfileViewModel::class.java)
        
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
        
        userNameTextView = findViewById(R.id.userNameTextView)
        bmiTextView = findViewById(R.id.bmiTextView)
        bmiCategoryTextView = findViewById(R.id.bmiCategoryTextView)
        progressTextView = findViewById(R.id.progressTextView)
        caloriesTextView = findViewById(R.id.caloriesTextView)
        progressBar = findViewById(R.id.progressBar)
        
        // Инициализация элементов шкалы ИМТ
        bmiScaleContainer = findViewById(R.id.bmiScaleContainer)
        bmiIndicator = findViewById(R.id.bmiIndicator)
        
        // Устанавливаем оптимизированные размеры шрифтов
        applyOptimizedFontSizes()
    }
    
    private fun applyOptimizedFontSizes() {
        try {
            // Используем ресурсы из dimens.xml для согласованности
            // Основные значения (вес, ИМТ, калории) - 22sp
            weightTextView.textSize = resources.getDimension(R.dimen.card_value_text_size) / resources.displayMetrics.scaledDensity
            bmiTextView.textSize = resources.getDimension(R.dimen.card_value_text_size) / resources.displayMetrics.scaledDensity
            caloriesTextView.textSize = resources.getDimension(R.dimen.card_value_text_size) / resources.displayMetrics.scaledDensity
            
            // Имя пользователя - 20sp
            userNameTextView.textSize = resources.getDimension(R.dimen.text_size_xlarge) / resources.displayMetrics.scaledDensity
            
            // Категории и пояснения - 12sp (ИЗМЕНЕНО: было 14sp)
            bmiCategoryTextView.textSize = resources.getDimension(R.dimen.card_detail_text_size) / resources.displayMetrics.scaledDensity
            progressTextView.textSize = resources.getDimension(R.dimen.card_detail_text_size) / resources.displayMetrics.scaledDensity
            
            // Кнопки - 12sp с эмодзи
            addButton.textSize = resources.getDimension(R.dimen.card_small_text_size) / resources.displayMetrics.scaledDensity
            historyButton.textSize = resources.getDimension(R.dimen.card_small_text_size) / resources.displayMetrics.scaledDensity
            chartButton.textSize = resources.getDimension(R.dimen.card_small_text_size) / resources.displayMetrics.scaledDensity
            profileButton.textSize = resources.getDimension(R.dimen.card_small_text_size) / resources.displayMetrics.scaledDensity
            
            // Устанавливаем тексты кнопок из строковых ресурсов
            addButton.text = getString(R.string.add_weight)
            historyButton.text = getString(R.string.view_history)
            chartButton.text = getString(R.string.view_chart)
            profileButton.text = getString(R.string.view_profile)
            
        } catch (e: Exception) {
            // При ошибке используем значения по умолчанию
            e.printStackTrace()
            
            // Значения по умолчанию
            weightTextView.textSize = 22f
            bmiTextView.textSize = 22f
            caloriesTextView.textSize = 22f
            userNameTextView.textSize = 20f
            bmiCategoryTextView.textSize = 12f  // ИЗМЕНЕНО: было 14f
            progressTextView.textSize = 12f      // ИЗМЕНЕНО: было 14f
            addButton.textSize = 12f
            historyButton.textSize = 12f
            chartButton.textSize = 12f
            profileButton.textSize = 12f
            
            // Тексты по умолчанию
            addButton.text = "➕ Добавить"
            historyButton.text = "📋 История"
            chartButton.text = "📈 График"
            profileButton.text = "👤 Профиль"
        }
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
        viewModel.latestEntry.observe(this) { entry ->
            val currentWeight = entry?.weight
            weightTextView.text = if (currentWeight != null) 
                "${String.format(Locale.getDefault(), "%.1f", currentWeight)} кг" 
                else "-- кг"
            
            userProfileViewModel.userProfile.value?.let { profile ->
                calculateAndDisplayMetrics(profile, currentWeight)
            }
        }
        
        userProfileViewModel.userProfile.observe(this) { profile ->
            profile?.let {
                userNameTextView.text = if (it.name.isNotEmpty()) it.name else "Гость"
                
                val currentWeight = viewModel.latestEntry.value?.weight
                
                calculateAndDisplayMetrics(it, currentWeight)
            }
        }
    }
    
    private fun calculateAndDisplayMetrics(profile: UserProfile, currentWeight: Double?) {
        // 1. Расчет ИМТ (если есть текущий вес и рост)
        if (currentWeight != null && profile.heightCm > 0) {
            val bmi = HealthCalculations.calculateBMI(currentWeight, profile.heightCm)
            val bmiCategory = HealthCalculations.getBMICategory(bmi)
            
            bmiTextView.text = String.format(Locale.getDefault(), "%.1f", bmi)
            bmiCategoryTextView.text = bmiCategory
            bmiCategoryTextView.setTextColor(HealthCalculations.getBMIColor(this, bmi))
            
            // Обновляем шкалу ИМТ
            updateBMIScale(bmi)
        } else {
            bmiTextView.text = "--"
            bmiCategoryTextView.text = if (profile.heightCm <= 0) 
                "Введите рост" 
                else "Добавьте вес"
            bmiCategoryTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            
            // Скрываем индикатор на шкале
            bmiIndicator.visibility = View.GONE
        }
        
        // 2. Прогресс к целевой массе
        if (currentWeight != null && profile.targetWeightKg > 0) {
            val progressResult = HealthCalculations.calculateProgress(
                currentWeight, 
                profile.startWeightKg, 
                profile.targetWeightKg
            )
            
            progressTextView.text = String.format(Locale.getDefault(), "%d%% (%s", 
                progressResult.progressPercent, 
                progressResult.message.replace("Осталось ", "").replace(": ", ": "))
            progressBar.progress = progressResult.progressPercent
        } else {
            progressTextView.text = if (profile.targetWeightKg <= 0) 
                "Установите цель" 
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
            caloriesTextView.text = String.format(Locale.getDefault(), "%d", calories)
        } else {
            caloriesTextView.text = "--"
        }
    }
    
    private fun updateBMIScale(bmi: Double) {
        try {
            // Показываем индикатор
            bmiIndicator.visibility = View.VISIBLE
            
            // Рассчитываем позицию индикатора на шкале (0-100%)
            val positionPercent = when {
                bmi < 16 -> 0f
                bmi > 40 -> 100f
                else -> ((bmi - 16) / (40 - 16) * 100).toFloat()
            }
            
            // Обновляем позицию индикатора
            updateIndicatorPosition(positionPercent)
            
            // Обновляем цвет индикатора в зависимости от категории ИМТ
            updateIndicatorColor(bmi)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun updateIndicatorPosition(positionPercent: Float) {
        // Используем post для получения актуальных размеров после отрисовки
        bmiScaleContainer.post {
            try {
                val scaleWidth = bmiScaleContainer.width
                val indicatorWidth = bmiIndicator.layoutParams.width
                
                // Рассчитываем позицию индикатора
                var position = (scaleWidth * positionPercent / 100).toInt()
                
                // Ограничиваем позицию в пределах шкалы
                position = position.coerceIn(0, scaleWidth - indicatorWidth)
                
                // Устанавливаем позицию индикатора
                val layoutParams = bmiIndicator.layoutParams as LinearLayout.LayoutParams
                layoutParams.marginStart = position
                bmiIndicator.layoutParams = layoutParams
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun updateIndicatorColor(bmi: Double) {
        try {
            val color = when {
                bmi < 16 -> ContextCompat.getColor(this, R.color.bmi_scale_severe)
                bmi < 18.5 -> ContextCompat.getColor(this, R.color.bmi_scale_thin)
                bmi < 25 -> ContextCompat.getColor(this, R.color.bmi_scale_normal)
                bmi < 30 -> ContextCompat.getColor(this, R.color.bmi_scale_overweight)
                bmi < 35 -> ContextCompat.getColor(this, R.color.bmi_scale_obesity1)
                bmi < 40 -> ContextCompat.getColor(this, R.color.bmi_scale_obesity2)
                else -> ContextCompat.getColor(this, R.color.bmi_scale_obesity3)
            }
            bmiIndicator.setBackgroundColor(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun showAddWeightDialog() {
        try {
            val dialog = AddWeightDialogFragment()
            dialog.setOnWeightAddedListener(this)
            dialog.show(supportFragmentManager, "AddWeightDialog")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onWeightAdded(entry: WeightEntry) {
        lifecycleScope.launch {
            try {
                val id = viewModel.addEntry(entry)
                
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                
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
        userProfileViewModel.userProfile.value?.let { profile ->
            val currentWeight = viewModel.latestEntry.value?.weight
            calculateAndDisplayMetrics(profile, currentWeight)
        }
    }
}