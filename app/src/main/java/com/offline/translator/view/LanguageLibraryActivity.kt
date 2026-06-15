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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.offline.translator.R
import com.offline.translator.model.Language
import com.offline.translator.model.LanguageDownloadManager
import com.offline.translator.model.TranslationService
import kotlinx.coroutines.launch

class LanguageLibraryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var translationService: TranslationService
    private lateinit var downloadManager: LanguageDownloadManager
    private val languages = Language.SUPPORTED_LANGUAGES.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
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
            val downloaded = translationService.getDownloadedModels()
            languages.forEachIndexed { index, lang ->
                languages[index] = lang.copy(isDownloaded = downloaded.contains(lang.code))
            }
            progressBar.visibility = View.GONE
            recyclerView.adapter = LanguageAdapter(languages) { language ->
                if (language.isDownloaded) {
                    showDeleteDialog(language)
                } else {
                    downloadLanguage(language)
                }
            }
        }
    }

    private fun downloadLanguage(language: Language) {
        val index = languages.indexOfFirst { it.code == language.code }
        if (index >= 0) {
            languages[index] = language.copy(isDownloading = true)
            recyclerView.adapter?.notifyItemChanged(index)

            lifecycleScope.launch {
                val result = translationService.downloadLanguage(language.code, Language.DEFAULT_TARGET)
                val langIndex = languages.indexOfFirst { it.code == language.code }
                if (langIndex >= 0) {
                    languages[langIndex] = languages[langIndex].copy(isDownloading = false, isDownloaded = result.isSuccess)
                    recyclerView.adapter?.notifyItemChanged(langIndex)
                    if (result.isSuccess) {
                        downloadManager.saveDownloadedLanguage(language.code)
                        Toast.makeText(this@LanguageLibraryActivity, "${language.name} baixado!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LanguageLibraryActivity, "Erro: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showDeleteDialog(language: Language) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Idioma")
            .setMessage("Excluir ${language.name}?")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    translationService.deleteLanguage(language.code)
                    downloadManager.removeDownloadedLanguage(language.code)
                    val index = languages.indexOfFirst { it.code == language.code }
                    if (index >= 0) {
                        languages[index] = languages[index].copy(isDownloaded = false)
                        recyclerView.adapter?.notifyItemChanged(index)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

class LanguageAdapter(
    private val languages: List<Language>,
    private val onItemClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtLanguageName)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val btnAction: Button = view.findViewById(R.id.btnAction)
        val progress: ProgressBar = view.findViewById(R.id.progressDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lang = languages[position]
        holder.txtName.text = lang.name
        holder.progress.visibility = if (lang.isDownloading) View.VISIBLE else View.GONE
        holder.btnAction.visibility = if (lang.isDownloading) View.INVISIBLE else View.VISIBLE

        if (lang.isDownloaded) {
            holder.txtStatus.text = "Baixado"
            holder.txtStatus.setTextColor(0xFF4CAF50.toInt())
            holder.btnAction.text = "Remover"
            holder.btnAction.setBackgroundResource(R.drawable.btn_delete)
        } else {
            holder.txtStatus.text = "Toque para baixar"
            holder.txtStatus.setTextColor(0xFF666666.toInt())
            holder.btnAction.text = "Baixar"
            holder.btnAction.setBackgroundResource(R.drawable.btn_download)
        }

        holder.btnAction.setOnClickListener { onItemClick(lang) }
    }

    override fun getItemCount() = languages.size
}