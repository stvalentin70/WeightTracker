package com.stvalentin.weighttracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
    private lateinit var importButton: Button
    
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
    
    private var progressDialog: AlertDialog? = null
    
    companion object {
        private const val REQUEST_CODE_IMPORT_CSV = 1001
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_enhanced)  // Используем enhanced версию
        
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
        
        // Убеждаемся, что кнопки имеют правильный текст с эмодзи
        ensureButtonsHaveEmoji()
    }
    
    private fun initializeViews() {
        weightTextView = findViewById(R.id.weightTextView)
        addButton = findViewById(R.id.addButton)
        historyButton = findViewById(R.id.historyButton)
        chartButton = findViewById(R.id.chartButton)
        profileButton = findViewById(R.id.settingsButton)
        importButton = findViewById(R.id.importButton)
        
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
            weightTextView.textSize = resources.getDimension(R.dimen.card_value_text_size) / resources.displayMetrics.scaledDensity
            bmiTextView.textSize = resources.getDimension(R.dimen.card_value_text_size) / resources.displayMetrics.scaledDensity
            caloriesTextView.textSize = resources.getDimension(R.dimen.card_value_text_size) / resources.displayMetrics.scaledDensity
            
            // Имя пользователя - 18sp
            userNameTextView.textSize = resources.getDimension(R.dimen.text_size_xlarge) / resources.displayMetrics.scaledDensity
            
            // Категории и пояснения - 10sp
            bmiCategoryTextView.textSize = resources.getDimension(R.dimen.card_detail_text_size) / resources.displayMetrics.scaledDensity
            progressTextView.textSize = resources.getDimension(R.dimen.card_detail_text_size) / resources.displayMetrics.scaledDensity
            
        } catch (e: Exception) {
            // При ошибке используем значения по умолчанию
            e.printStackTrace()
            
            // Значения по умолчанию
            weightTextView.textSize = 20f
            bmiTextView.textSize = 20f
            caloriesTextView.textSize = 20f
            userNameTextView.textSize = 18f
            bmiCategoryTextView.textSize = 10f
            progressTextView.textSize = 10f
        }
    }
    
    private fun ensureButtonsHaveEmoji() {
        // Явно устанавливаем тексты с эмодзи (на случай, если strings.xml не загрузился)
        addButton.text = "➕ Добавить"
        historyButton.text = "📋 История"
        chartButton.text = "📈 График"
        profileButton.text = "👤 Профиль"
        importButton.text = "📥 Импорт/Экспорт"
        
        // Устанавливаем размер шрифта для кнопок
        addButton.textSize = 12f
        historyButton.textSize = 12f
        chartButton.textSize = 12f
        profileButton.textSize = 12f
        importButton.textSize = 12f
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
        
        importButton.setOnClickListener {
            showImportDialog()
        }
    }
    
    private fun showImportDialog() {
        val items = arrayOf("Импорт из CSV", "Экспорт в CSV")
        
        AlertDialog.Builder(this)
            .setTitle("Импорт/Экспорт данных")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openFilePickerForImport()
                    1 -> exportToCSV()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun openFilePickerForImport() {
        try {
            // Пробуем сначала с ACTION_GET_CONTENT
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "text/*",
                    "application/*",
                    "image/*" // Некоторые файловые менеджеры могут требовать это
                ))
                // Разрешаем выбирать любые файлы
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
            
            startActivityForResult(Intent.createChooser(intent, "Выберите CSV файл"), REQUEST_CODE_IMPORT_CSV)
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при открытии файлового менеджера: ${e.message}")
            
            // Альтернативный вариант
            try {
                val fallbackIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(fallbackIntent, REQUEST_CODE_IMPORT_CSV)
            } catch (e2: Exception) {
                Toast.makeText(this, "Не удалось открыть файловый менеджер", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Ошибка в fallback: ${e2.message}")
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_IMPORT_CSV && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                Log.d(TAG, "Выбран файл: $uri")
                Log.d(TAG, "Путь файла: ${uri.path}")
                
                // Проверяем разрешения
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Не удалось получить разрешения для файла: ${e.message}")
                }
                
                importCSVFile(uri)
            } ?: run {
                Log.e(TAG, "URI файла равен null")
                Toast.makeText(this, "Ошибка: не выбран файл", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun importCSVFile(uri: Uri) {
        Log.d(TAG, "Начинаем импорт файла: $uri")
        
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "Не удалось открыть InputStream для файла")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка: не удалось прочитать файл",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                
                inputStream.use { stream ->
                    val fileInfo = CSVImportUtil.getCSVInfo(stream)
                    Log.d(TAG, "Информация о файле: $fileInfo")
                    
                    if (fileInfo.validLines == 0) {
                        runOnUiThread {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Ошибка")
                                .setMessage("Файл не содержит валидных данных. Проверьте формат файла.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        return@launch
                    }
                    
                    // Показываем диалог подтверждения
                    runOnUiThread {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Подтверждение импорта")
                            .setMessage("Найдено ${fileInfo.validLines} записей. Импортировать?")
                            .setPositiveButton("Импортировать") { _, _ ->
                                startImport(uri)
                            }
                            .setNegativeButton("Отмена", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка чтения файла: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка чтения файла: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun startImport(uri: Uri) {
        Log.d(TAG, "Запуск импорта из: $uri")
        
        lifecycleScope.launch {
            showProgressDialog("Импорт данных...")
            
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "Не удалось открыть InputStream для импорта")
                    runOnUiThread {
                        hideProgressDialog()
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка: не удалось прочитать файл",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                
                inputStream.use { stream ->
                    // Получаем репозиторий для импорта
                    val weightRepository = WeightRepository(WeightDatabase.getDatabase(this@MainActivity).weightDao())
                    
                    Log.d(TAG, "Начинаем импорт через CSVImportUtil")
                    val (successCount, errorCount) = CSVImportUtil.importFromCSV(
                        this@MainActivity,
                        stream,
                        weightRepository
                    )
                    
                    Log.d(TAG, "Импорт завершен: успешно=$successCount, ошибок=$errorCount")
                    
                    runOnUiThread {
                        hideProgressDialog()
                        
                        val message = if (successCount > 0) {
                            "Успешно импортировано: $successCount записей\n" +
                            (if (errorCount > 0) "Ошибок: $errorCount" else "")
                        } else {
                            "Не удалось импортировать данные. Проверьте формат файла."
                        }
                        
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(if (successCount > 0) "Импорт завершен" else "Ошибка импорта")
                            .setMessage(message)
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                // Обновляем данные
                                viewModel.allEntries.value?.let {
                                    // Данные обновятся через LiveData
                                }
                            }
                            .show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка импорта: ${e.message}", e)
                runOnUiThread {
                    hideProgressDialog()
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка импорта: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun exportToCSV() {
        lifecycleScope.launch {
            val entries = viewModel.allEntries.value ?: emptyList()
            
            if (entries.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Нет данных для экспорта",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }
            
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "weight_export_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.csv")
            }
            
            startActivityForResult(intent, REQUEST_CODE_IMPORT_CSV + 1)
        }
    }
    
    private fun showProgressDialog(message: String) {
        runOnUiThread {
            try {
                val view = layoutInflater.inflate(R.layout.dialog_progress, null)
                view.findViewById<TextView>(R.id.progressMessage).text = message
                
                progressDialog = AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create()
                
                progressDialog?.show()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при показе диалога прогресса: ${e.message}")
            }
        }
    }
    
    private fun hideProgressDialog() {
        runOnUiThread {
            try {
                progressDialog?.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при скрытии диалога прогресса: ${e.message}")
            } finally {
                progressDialog = null
            }
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
                val indicatorWidth = resources.getDimension(R.dimen.bmi_indicator_width).toInt()
                
                // Рассчитываем позицию индикатора
                var position = (scaleWidth * positionPercent / 100).toInt()
                
                // Ограничиваем позицию в пределах шкалы
                position = position.coerceIn(0, scaleWidth - indicatorWidth)
                
                // Устанавливаем позицию индикатора
                val layoutParams = bmiIndicator.layoutParams as RelativeLayout.LayoutParams
                layoutParams.marginStart = position
                bmiIndicator.layoutParams = layoutParams
                
                // Принудительно перерисовываем
                bmiIndicator.requestLayout()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun updateIndicatorColor(bmi: Double) {
        try {
            val color = HealthCalculations.getBmiIndicatorColor(this, bmi)
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