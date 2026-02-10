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
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "text/*",
                    "application/*",
                    "image/*"
                ))
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
            
            startActivityForResult(Intent.createChooser(intent, "Выберите CSV файл"), REQUEST_CODE_IMPORT_CSV)
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при открытии файлового менеджера: ${e.message}")
            
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
                importCSVFile(uri)
            } ?: run {
                Toast.makeText(this, "Ошибка: не выбран файл", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun importCSVFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
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
        lifecycleScope.launch {
            showProgressDialog("Импорт данных...")
            
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
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
                    val weightRepository = WeightRepository(WeightDatabase.getDatabase(this@MainActivity).weightDao())
                    
                    val (successCount, errorCount) = CSVImportUtil.importFromCSV(
                        this@MainActivity,
                        stream,
                        weightRepository
                    )
                    
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
                            }
                            .show()
                    }
                }
            } catch (e: Exception) {
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
        // 1. Расчет ИМТ
        if (currentWeight != null && profile.heightCm > 0) {
            val bmi = HealthCalculations.calculateBMI(currentWeight, profile.heightCm)
            val bmiCategory = HealthCalculations.getBMICategory(bmi)
            
            bmiTextView.text = String.format(Locale.getDefault(), "%.1f", bmi)
            bmiCategoryTextView.text = bmiCategory
            bmiCategoryTextView.setTextColor(HealthCalculations.getBMIColor(this, bmi))
            
            updateBMIScale(bmi)
        } else {
            bmiTextView.text = "--"
            bmiCategoryTextView.text = if (profile.heightCm <= 0) 
                "Введите рост" 
                else "Добавьте вес"
            bmiCategoryTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
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
            bmiIndicator.visibility = View.VISIBLE
            
            val positionPercent = when {
                bmi < 16 -> 0f
                bmi > 40 -> 100f
                else -> ((bmi - 16) / (40 - 16) * 100).toFloat()
            }
            
            updateIndicatorPosition(positionPercent)
            updateIndicatorColor(bmi)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun updateIndicatorPosition(positionPercent: Float) {
        bmiScaleContainer.post {
            try {
                val scaleWidth = bmiScaleContainer.width
                val indicatorWidth = resources.getDimension(R.dimen.bmi_indicator_width).toInt()
                
                var position = (scaleWidth * positionPercent / 100).toInt()
                position = position.coerceIn(0, scaleWidth - indicatorWidth)
                
                val layoutParams = bmiIndicator.layoutParams as RelativeLayout.LayoutParams
                layoutParams.marginStart = position
                bmiIndicator.layoutParams = layoutParams
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