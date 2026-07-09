package com.offline.translator.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.offline.translator.R
import com.offline.translator.model.Language
import com.offline.translator.model.LanguageDownloadManager
import com.offline.translator.model.PreferencesManager
import com.offline.translator.model.TranslationService
import kotlinx.coroutines.launch

class LanguageLibraryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var translationService: TranslationService
    private lateinit var downloadManager: LanguageDownloadManager
    private val languages = Language.SUPPORTED_LANGUAGES.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferencesManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_library)

        translationService = TranslationService(this)
        downloadManager = LanguageDownloadManager(this)
        recyclerView = findViewById(R.id.recyclerLanguages)
        progressBar = findViewById(R.id.progressBar)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadLanguages()
    }

    private fun loadLanguages() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val mlKitDownloaded = translationService.getDownloadedModels()
            val savedDownloaded = downloadManager.getDownloadedLanguages()
            val combinedDownloads = (mlKitDownloaded + savedDownloaded).toSet()

            languages.forEach { lang ->
                lang.isDownloaded = combinedDownloads.contains(lang.code)
            }

            progressBar.visibility = View.GONE
            recyclerView.adapter = LanguageAdapter(languages) { language, isDownloading ->
                if (language.isDownloaded) {
                    showDeleteDialog(language)
                } else {
                    downloadLanguage(language)
                }
            }
        }
    }

    private fun downloadLanguage(language: Language) {
        language.isDownloading = true
        recyclerView.adapter?.notifyDataSetChanged()

        lifecycleScope.launch {
            val result = translationService.downloadLanguage("en", language.code)
            result.fold(
                onSuccess = {
                    language.isDownloaded = true
                    language.isDownloading = false
                    Toast.makeText(this@LanguageLibraryActivity, "✓ ${language.name} baixado!", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    language.isDownloading = false
                    Toast.makeText(this@LanguageLibraryActivity, "Erro: ${it.message}", Toast.LENGTH_LONG).show()
                }
            )
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun showDeleteDialog(language: Language) {
        AlertDialog.Builder(this)
            .setTitle("Remover Idioma")
            .setMessage("Deseja remover ${language.name}?")
            .setPositiveButton("Remover") { _, _ ->
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
                    language.isDownloaded = false
                    Toast.makeText(this@LanguageLibraryActivity, "${language.name} removido", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(this@LanguageLibraryActivity, "Erro: ${it.message}", Toast.LENGTH_LONG).show()
                }
            )
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    class LanguageAdapter(
        private val languages: List<Language>,
        private val onClick: (Language, Boolean) -> Unit
    ) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.txtLanguageName)
            val nativeText: TextView = view.findViewById(R.id.txtNativeName)
            val downloadBtn: Button = view.findViewById(R.id.btnDownload)
            val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val language = languages[position]
            holder.nameText.text = language.name
            holder.nativeText.text = language.nativeName

            holder.progressBar.visibility = if (language.isDownloading) View.VISIBLE else View.GONE
            holder.downloadBtn.visibility = if (language.isDownloading) View.GONE else View.VISIBLE

            if (language.isDownloaded) {
                holder.downloadBtn.text = "Remover"
                holder.downloadBtn.setBackgroundResource(R.drawable.btn_downloaded)
            } else {
                holder.downloadBtn.text = "Baixar"
                holder.downloadBtn.setBackgroundResource(R.drawable.btn_download)
            }

            holder.downloadBtn.setOnClickListener {
                onClick(language, language.isDownloading)
            }
        }

        override fun getItemCount() = languages.size
    }
}
