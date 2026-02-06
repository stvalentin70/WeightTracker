package com.stvalentin.weighttracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

class AddWeightDialogFragment : DialogFragment() {
    
    interface OnWeightAddedListener {
        fun onWeightAdded(entry: WeightEntry)
    }
    
    private var listener: OnWeightAddedListener? = null
    private lateinit var dateTimeTextView: TextView
    private var selectedDate = Calendar.getInstance().time
    
    private val dateTimeFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    fun setOnWeightAddedListener(listener: OnWeightAddedListener) {
        this.listener = listener
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Устанавливаем текущую дату
        selectedDate = Calendar.getInstance().time
        
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_add_weight_with_date, null)
        
        val weightEditText = view.findViewById<EditText>(R.id.weightEditText)
        dateTimeTextView = view.findViewById<TextView>(R.id.dateTimeTextView)
        val selectDateTimeButton = view.findViewById<Button>(R.id.selectDateTimeButton)
        val contextSpinner = view.findViewById<Spinner>(R.id.contextSpinner)
        val noteEditText = view.findViewById<EditText>(R.id.noteEditText)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        
        // Обновляем отображение даты
        updateDateTimeDisplay()
        
        // Обработчик кнопки выбора даты и времени
        selectDateTimeButton.setOnClickListener {
            showDateTimePicker()
        }
        
        // Настройка спиннера с контекстами
        val contexts = WeightContext.values().map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            contexts
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        contextSpinner.adapter = adapter
        
        // Создаем диалог
        val builder = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Добавить запись о весе")
        
        val dialog = builder.create()
        
        // Обработчики кнопок
        saveButton.setOnClickListener {
            val weightText = weightEditText.text.toString()
            val contextIndex = contextSpinner.selectedItemPosition
            val note = noteEditText.text.toString()
            
            if (weightText.isNotEmpty()) {
                try {
                    val weight = weightText.toDouble()
                    if (weight > 0 && weight < 300) { // Валидация разумных значений
                        val weightContext = WeightContext.values()[contextIndex]
                        val newEntry = WeightEntry(
                            weight = weight,
                            dateTime = selectedDate, // Используем выбранную дату
                            context = weightContext,
                            note = note
                        )
                        
                        listener?.onWeightAdded(newEntry)
                        dialog.dismiss()
                    } else {
                        weightEditText.error = "Введите корректный вес (0-300 кг)"
                    }
                } catch (e: NumberFormatException) {
                    weightEditText.error = "Введите число"
                }
            } else {
                weightEditText.error = "Введите вес"
            }
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        return dialog
    }
    
    private fun updateDateTimeDisplay() {
        dateTimeTextView.text = dateTimeFormatter.format(selectedDate)
    }
    
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedDate }
        
        // Сначала выбираем дату
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                
                // Затем выбираем время
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        selectedDate = calendar.time
                        updateDateTimeDisplay()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}