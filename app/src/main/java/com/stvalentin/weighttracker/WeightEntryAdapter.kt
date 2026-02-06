package com.stvalentin.weighttracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class WeightEntryAdapter(
    private var entries: List<WeightEntry> = emptyList(),
    private val onDeleteClick: (WeightEntry) -> Unit
) : RecyclerView.Adapter<WeightEntryAdapter.WeightEntryViewHolder>() {

    inner class WeightEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val weightTextView: TextView = itemView.findViewById(R.id.weightTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val contextTextView: TextView = itemView.findViewById(R.id.contextTextView)
        private val noteTextView: TextView = itemView.findViewById(R.id.noteTextView)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        
        private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        
        fun bind(entry: WeightEntry) {
            weightTextView.text = "${entry.weight} кг"
            dateTextView.text = dateFormatter.format(entry.dateTime)
            contextTextView.text = entry.context.displayName
            
            if (entry.note.isNotEmpty()) {
                noteTextView.text = entry.note
                noteTextView.visibility = View.VISIBLE
            } else {
                noteTextView.visibility = View.GONE
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(entry)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeightEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight_entry, parent, false)
        return WeightEntryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: WeightEntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }
    
    override fun getItemCount(): Int = entries.size
    
    fun updateEntries(newEntries: List<WeightEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}