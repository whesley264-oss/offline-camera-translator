package com.offline.translator.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.offline.translator.databinding.ActivityHistoryBinding
import com.offline.translator.model.HistoryManager
import com.offline.translator.model.TextToSpeechService

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyManager: HistoryManager
    private lateinit var tts: TextToSpeechService
    private lateinit var adapter: HistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        historyManager = HistoryManager(this)
        tts = TextToSpeechService(this)
        
        setupUI()
        loadHistory()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Limpar Histórico")
                .setMessage("Tem certeza que deseja apagar todo o histórico?")
                .setPositiveButton("Limpar") { _, _ ->
                    historyManager.clearHistory()
                    loadHistory()
                    Toast.makeText(this, "Histórico limpo!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                filterHistory(query)
            }
        })
        
        adapter = HistoryAdapter(
            items = emptyList(),
            onSpeak = { text -> speakText(text) },
            onShare = { text -> shareText(text) },
            onCopy = { text -> copyText(text) },
            onDelete = { id -> deleteItem(id) }
        )
        
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter
    }
    
    private fun loadHistory() {
        val history = historyManager.getHistory()
        updateUI(history)
    }
    
    private fun filterHistory(query: String) {
        val history = if (query.isBlank()) {
            historyManager.getHistory()
        } else {
            historyManager.searchHistory(query)
        }
        updateUI(history)
    }
    
    private fun updateUI(history: List<com.offline.translator.model.TranslationHistoryEntry>) {
        if (history.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerHistory.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerHistory.visibility = View.VISIBLE
            adapter.updateItems(history)
        }
    }
    
    private fun speakText(text: String) {
        if (tts.isAvailable()) {
            tts.speakTranslation(text, "pt")
        } else {
            Toast.makeText(this, "TTS não disponível", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartilhar"))
    }
    
    private fun copyText(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tradução", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "✓ Copiado!", Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteItem(id: String) {
        historyManager.deleteEntry(id)
        loadHistory()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}