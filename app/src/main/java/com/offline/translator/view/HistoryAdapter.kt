package com.offline.translator.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.offline.translator.databinding.ItemHistoryBinding
import com.offline.translator.model.TranslationHistoryEntry
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var items: List<TranslationHistoryEntry>,
    private val onSpeak: (String) -> Unit,
    private val onShare: (String) -> Unit,
    private val onCopy: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    
    inner class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        
        binding.txtLanguage.text = "${item.sourceLang.uppercase()} → ${item.targetLang.uppercase()}"
        binding.txtOriginal.text = item.originalText
        binding.txtTranslated.text = item.translatedText
        binding.txtType.text = if (item.type == "image") "📷" else "📝"
        binding.txtTime.text = formatTime(item.timestamp)
        
        binding.btnSpeak.setOnClickListener { onSpeak(item.translatedText) }
        binding.btnShare.setOnClickListener { onShare(item.translatedText) }
        binding.btnCopy.setOnClickListener { onCopy(item.translatedText) }
        binding.btnDelete.setOnClickListener { onDelete(item.id) }
    }
    
    override fun getItemCount() = items.size
    
    fun updateItems(newItems: List<TranslationHistoryEntry>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Agora"
            diff < 3600_000 -> "${diff / 60_000}m atrás"
            diff < 86400_000 -> "${diff / 3600_000}h atrás"
            diff < 604800_000 -> "${diff / 86400_000}d atrás"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
        }
    }
}