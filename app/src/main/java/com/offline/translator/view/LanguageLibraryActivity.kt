package com.offline.translator.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.offline.translator.R
import com.offline.translator.databinding.ActivityLanguageLibraryBinding
import com.offline.translator.model.Language
import com.offline.translator.model.TranslationService
import kotlinx.coroutines.launch

class LanguageLibraryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLanguageLibraryBinding
    private lateinit var translationService: TranslationService
    private lateinit var adapter: LanguageAdapter
    
    private val languages = Language.SUPPORTED_LANGUAGES.toMutableList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        translationService = TranslationService(this)
        
        setupRecyclerView()
        loadDownloadedLanguages()
    }
    
    private fun setupRecyclerView() {
        adapter = LanguageAdapter(
            languages = languages,
            onDownloadClick = { downloadLanguage(it) },
            onDeleteClick = { showDeleteConfirmation(it) }
        )
        
        binding.recyclerLanguages.layoutManager = LinearLayoutManager(this)
        binding.recyclerLanguages.adapter = adapter
    }
    
    private fun loadDownloadedLanguages() {
        lifecycleScope.launch {
            val downloaded = translationService.getDownloadedModels()
            languages.forEachIndexed { index, language ->
                languages[index] = language.copy(isDownloaded = downloaded.contains(language.code))
            }
            adapter.notifyDataSetChanged()
        }
    }
    
    private fun downloadLanguage(language: Language) {
        val index = languages.indexOfFirst { it.code == language.code }
        if (index >= 0) {
            languages[index] = language.copy(isDownloading = true)
            adapter.notifyItemChanged(index)
        }
        
        lifecycleScope.launch {
            // Download both source and target models for this language
            val result = translationService.downloadLanguage(language.code, Language.DEFAULT_TARGET)
            
            val langIndex = languages.indexOfFirst { it.code == language.code }
            if (langIndex >= 0) {
                languages[langIndex] = languages[langIndex].copy(isDownloading = false)
                
                result.fold(
                    onSuccess = {
                        languages[langIndex] = languages[langIndex].copy(isDownloaded = true)
                        Toast.makeText(
                            this@LanguageLibraryActivity,
                            "${language.name} baixado!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            this@LanguageLibraryActivity,
                            "Erro: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
                adapter.notifyItemChanged(langIndex)
            }
        }
    }
    
    private fun showDeleteConfirmation(language: Language) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Idioma")
            .setMessage("Deseja excluir ${language.name}? Você precisará baixá-lo novamente.")
            .setPositiveButton("Excluir") { _, _ ->
                deleteLanguage(language)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun deleteLanguage(language: Language) {
        lifecycleScope.launch {
            val result = translationService.deleteLanguage(language.code)
            
            result.fold(
                onSuccess = {
                    val index = languages.indexOfFirst { it.code == language.code }
                    if (index >= 0) {
                        languages[index] = languages[index].copy(isDownloaded = false)
                        adapter.notifyItemChanged(index)
                    }
                    Toast.makeText(
                        this@LanguageLibraryActivity,
                        "${language.name} excluído",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { e ->
                    Toast.makeText(
                        this@LanguageLibraryActivity,
                        "Erro: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}