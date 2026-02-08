package com.stvalentin.weighttracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class UserProfileActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "UserProfileActivity"
    }
    
    private lateinit var nameEditText: EditText
    private lateinit var heightEditText: EditText
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var genderMaleRadio: RadioButton
    private lateinit var genderFemaleRadio: RadioButton
    private lateinit var birthDateTextView: TextView
    private lateinit var targetWeightEditText: EditText
    private lateinit var startWeightEditText: EditText
    private lateinit var activityLevelSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    
    private val viewModel: UserProfileViewModel by viewModels {
        UserProfileViewModelFactory(
            UserProfileRepository(
                WeightDatabase.getDatabase(this).userProfileDao()
            )
        )
    }
    
    private var selectedBirthDate = Date()
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        
        Log.d(TAG, "UserProfileActivity создана")
        
        // Настройка ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Профиль пользователя"
        
        // Инициализация UI элементов
        initializeViews()
        setupSpinner()
        setupClickListeners()
        loadUserProfile()
    }
    
    private fun initializeViews() {
        nameEditText = findViewById(R.id.nameEditText)
        heightEditText = findViewById(R.id.heightEditText)
        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        genderMaleRadio = findViewById(R.id.genderMaleRadio)
        genderFemaleRadio = findViewById(R.id.genderFemaleRadio)
        birthDateTextView = findViewById(R.id.birthDateTextView)
        targetWeightEditText = findViewById(R.id.targetWeightEditText)
        startWeightEditText = findViewById(R.id.startWeightEditText)
        activityLevelSpinner = findViewById(R.id.activityLevelSpinner)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
    }
    
    private fun setupSpinner() {
        // Уровни активности
        val activityLevels = arrayOf(
            "Сидячий",
            "Легкая активность",
            "Умеренная активность",
            "Высокая активность",
            "Экстремальная активность"
        )
        
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            activityLevels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        activityLevelSpinner.adapter = adapter
    }
    
    private fun setupClickListeners() {
        // Выбор даты рождения
        birthDateTextView.setOnClickListener {
            showDatePicker()
        }
        
        findViewById<Button>(R.id.selectDateButton).setOnClickListener {
            showDatePicker()
        }
        
        // Сохранение
        saveButton.setOnClickListener {
            saveProfile()
        }
        
        // Отмена
        cancelButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadUserProfile() {
        // Наблюдаем за изменениями профиля
        viewModel.userProfile.observe(this) { profile ->
            Log.d(TAG, "Получен профиль из ViewModel: $profile")
            profile?.let {
                fillForm(it)
            }
        }
        
        // Для отладки: проверим что в базе
        lifecycleScope.launch {
            val repository = UserProfileRepository(
                WeightDatabase.getDatabase(this@UserProfileActivity).userProfileDao()
            )
            val currentProfile = repository.getProfileSync()
            Log.d(TAG, "Текущий профиль в базе: $currentProfile")
        }
    }
    
    private fun fillForm(profile: UserProfile) {
        Log.d(TAG, "Заполняем форму данными профиля: $profile")
        
        nameEditText.setText(profile.name)
        heightEditText.setText(profile.heightCm.toString())
        
        // Пол
        if (profile.gender == "male") {
            genderMaleRadio.isChecked = true
        } else {
            genderFemaleRadio.isChecked = true
        }
        
        // Дата рождения
        selectedBirthDate = profile.birthDate
        birthDateTextView.text = dateFormatter.format(selectedBirthDate)
        
        // Веса
        targetWeightEditText.setText(String.format(Locale.getDefault(), "%.1f", profile.targetWeightKg))
        startWeightEditText.setText(String.format(Locale.getDefault(), "%.1f", profile.startWeightKg))
        
        // Уровень активности
        val activityLevelIndex = when (profile.activityLevel) {
            "sedentary" -> 0
            "light" -> 1
            "moderate" -> 2
            "high" -> 3
            "extreme" -> 4
            else -> 2 // умеренная по умолчанию
        }
        activityLevelSpinner.setSelection(activityLevelIndex)
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedBirthDate }
        
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedBirthDate = calendar.time
                birthDateTextView.text = dateFormatter.format(selectedBirthDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun saveProfile() {
        Log.d(TAG, "Начало сохранения профиля")
        
        // Валидация
        if (!validateForm()) {
            return
        }
        
        val name = nameEditText.text.toString().trim()
        val height = heightEditText.text.toString().toIntOrNull() ?: 170
        val gender = if (genderMaleRadio.isChecked) "male" else "female"
        val targetWeight = targetWeightEditText.text.toString().toDoubleOrNull() ?: 70.0
        val startWeight = startWeightEditText.text.toString().toDoubleOrNull() ?: 80.0
        
        // Преобразуем уровень активности в значение для базы данных
        val activityLevelPosition = activityLevelSpinner.selectedItemPosition
        val activityLevel = when (activityLevelPosition) {
            0 -> "sedentary"
            1 -> "light"
            2 -> "moderate"
            3 -> "high"
            4 -> "extreme"
            else -> "moderate"
        }
        
        val profile = UserProfile(
            name = name,
            heightCm = height,
            gender = gender,
            birthDate = selectedBirthDate,
            targetWeightKg = targetWeight,
            startWeightKg = startWeight,
            activityLevel = activityLevel
        )
        
        Log.d(TAG, "Создан профиль для сохранения: $profile")
        
        // ВАЖНО: Используем saveProfile вместо updateProfile!
        viewModel.saveProfile(profile)
        
        Toast.makeText(this, "Профиль сохранен", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Toast показан, профиль сохранен")
        
        // Ждем немного и закрываем
        lifecycleScope.launch {
            kotlinx.coroutines.delay(800) // Даем время на сохранение
            Log.d(TAG, "Закрываем Activity")
            finish()
        }
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        // Проверка имени (необязательно)
        
        // Проверка роста
        val heightText = heightEditText.text.toString()
        if (heightText.isNotEmpty()) {
            val height = heightText.toIntOrNull()
            if (height == null || height < 100 || height > 250) {
                heightEditText.error = "Введите рост от 100 до 250 см"
                isValid = false
            }
        }
        
        // Проверка целевого веса
        val targetWeightText = targetWeightEditText.text.toString()
        if (targetWeightText.isNotEmpty()) {
            val targetWeight = targetWeightText.toDoubleOrNull()
            if (targetWeight == null || targetWeight < 30 || targetWeight > 300) {
                targetWeightEditText.error = "Введите вес от 30 до 300 кг"
                isValid = false
            }
        }
        
        // Проверка стартового веса
        val startWeightText = startWeightEditText.text.toString()
        if (startWeightText.isNotEmpty()) {
            val startWeight = startWeightText.toDoubleOrNull()
            if (startWeight == null || startWeight < 30 || startWeight > 300) {
                startWeightEditText.error = "Введите вес от 30 до 300 кг"
                isValid = false
            }
        }
        
        return isValid
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}