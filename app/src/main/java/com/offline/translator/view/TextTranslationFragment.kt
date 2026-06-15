package com.offline.translator.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.offline.translator.R
import com.offline.translator.databinding.FragmentTextTranslationBinding
import com.offline.translator.model.*
import com.offline.translator.widget.TranslationWidgetProvider
import kotlinx.coroutines.*

class TextTranslationFragment : Fragment() {
    private var _binding: FragmentTextTranslationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var translationService: TranslationService
    private lateinit var statsManager: StatsManager
    private lateinit var githubSync: GitHubStatsSync
    private lateinit var historyManager: HistoryManager
    private lateinit var feedbackManager: FeedbackManager
    private lateinit var tts: TextToSpeechService
    private lateinit var downloadManager: LanguageDownloadManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var downloadedLanguages: List<Language> = emptyList()
    private var selectedSource = "en"
    private var selectedTarget = "pt"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTextTranslationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        translationService = TranslationService(requireContext())
        statsManager = StatsManager(requireContext())
        githubSync = GitHubStatsSync(requireContext())
        historyManager = HistoryManager(requireContext())
        feedbackManager = FeedbackManager(requireContext(), PreferencesManager(requireContext()))
        tts = TextToSpeechService(requireContext())
        downloadManager = LanguageDownloadManager(requireContext())
        setupUI()
        loadLanguages()
    }

    private fun setupUI() {
        binding.btnSwap.setOnClickListener {
            val temp = selectedSource
            selectedSource = selectedTarget
            selectedTarget = temp
            updateSpinnerSelections()
            feedbackManager.animatePulse(it)
        }
        
        binding.btnTranslate.setOnClickListener {
            feedbackManager.animatePulse(it)
            translateText()
        }

        binding.btnLibrary.setOnClickListener {
            startActivity(Intent(requireContext(), LanguageLibraryActivity::class.java))
        }
        
        binding.btnCopy.setOnClickListener {
            val text = binding.editTextOutput.text.toString()
            if (text.isNotBlank()) copyToClipboard(text)
        }
        
        binding.btnShare.setOnClickListener {
            val text = binding.editTextOutput.text.toString()
            if (text.isNotBlank()) shareText(text)
        }
        
        binding.btnSpeak.setOnClickListener {
            val text = binding.editTextOutput.text.toString()
            if (text.isNotBlank()) speakText(text)
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tradução", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "✓ Copiado!", Toast.LENGTH_SHORT).show()
        TranslationWidgetProvider.updateLastTranslation(requireContext(), text)
    }
    
    private fun shareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Tradução")
        }
        startActivity(Intent.createChooser(shareIntent, "Compartilhar via"))
    }
    
    private fun speakText(text: String) {
        if (tts.isAvailable()) {
            tts.speakTranslation(text, selectedTarget)
            Toast.makeText(context, "🔊 Reproduzindo...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "TTS não disponível", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadLanguages() {
        scope.launch {
            val downloaded = translationService.getDownloadedModels()
            
            // Use ML Kit models if available, otherwise use saved downloads
            val savedDownloads = downloadManager.getDownloadedLanguages()
            val combinedDownloads = if (downloaded.isNotEmpty()) downloaded else savedDownloads
            
            downloadedLanguages = Language.SUPPORTED_LANGUAGES.filter { combinedDownloads.contains(it.code) }
            
            if (downloadedLanguages.size < 2) {
                Toast.makeText(context, "Baixe idiomas na Biblioteca primeiro!", Toast.LENGTH_LONG).show()
                binding.btnTranslate.isEnabled = false
                return@launch
            }
            
            binding.btnTranslate.isEnabled = true
            
            val sourceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, downloadedLanguages.map { it.name })
            sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSource.adapter = sourceAdapter
            
            val targetAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, downloadedLanguages.map { it.name })
            targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerTarget.adapter = targetAdapter
            
            val sourceIndex = downloadedLanguages.indexOfFirst { it.code == "en" }
            val targetIndex = downloadedLanguages.indexOfFirst { it.code == "pt" }
            if (sourceIndex >= 0) binding.spinnerSource.setSelection(sourceIndex)
            if (targetIndex >= 0) binding.spinnerTarget.setSelection(targetIndex)
            selectedSource = "en"
            selectedTarget = "pt"
        }
    }
    
    private fun updateSpinnerSelections() {
        val sourceIndex = downloadedLanguages.indexOfFirst { it.code == selectedSource }
        val targetIndex = downloadedLanguages.indexOfFirst { it.code == selectedTarget }
        if (sourceIndex >= 0) binding.spinnerSource.setSelection(sourceIndex)
        if (targetIndex >= 0) binding.spinnerTarget.setSelection(targetIndex)
    }
    
    private fun translateText() {
        val inputText = binding.editTextInput.text.toString().trim()
        if (inputText.isEmpty()) {
            Toast.makeText(context, "Digite um texto", Toast.LENGTH_SHORT).show()
            feedbackManager.animateError(binding.editTextInput)
            return
        }
        
        if (downloadedLanguages.isEmpty()) {
            Toast.makeText(context, "Baixe idiomas primeiro!", Toast.LENGTH_LONG).show()
            return
        }
        
        val sourcePos = binding.spinnerSource.selectedItemPosition
        val targetPos = binding.spinnerTarget.selectedItemPosition
        if (sourcePos >= 0 && sourcePos < downloadedLanguages.size) {
            selectedSource = downloadedLanguages[sourcePos].code
        }
        if (targetPos >= 0 && targetPos < downloadedLanguages.size) {
            selectedTarget = downloadedLanguages[targetPos].code
        }
        
        binding.btnTranslate.isEnabled = false
        
        scope.launch {
            val result = translationService.translate(inputText, selectedSource, selectedTarget)
            binding.btnTranslate.isEnabled = true
            
            result.fold(
                onSuccess = { translated ->
                    binding.editTextOutput.setText(translated)
                    feedbackManager.vibrateOnTranslate()
                    feedbackManager.animateSuccess(binding.editTextOutput)
                    
                    // Save to local stats
                    val recordId = statsManager.saveTranslation(
                        inputText, translated, selectedSource, selectedTarget, TranslationType.TEXT
                    )
                    
                    // Save to history
                    historyManager.saveTranslation(
                        inputText, translated, selectedSource, selectedTarget, "text"
                    )
                    
                    // Show rating dialog
                    showRatingDialog(recordId)
                },
                onFailure = { e ->
                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                    feedbackManager.vibrateOnError()
                    feedbackManager.animateError(binding.editTextOutput)
                }
            )
        }
    }
    
    private fun showRatingDialog(recordId: Long) {
        if (context == null) return  // Safety check
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_rating, null)
        val dialog = android.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        var currentRating = 0
        val stars = listOf(
            dialogView.findViewById<ImageButton>(R.id.star1),
            dialogView.findViewById<ImageButton>(R.id.star2),
            dialogView.findViewById<ImageButton>(R.id.star3),
            dialogView.findViewById<ImageButton>(R.id.star4),
            dialogView.findViewById<ImageButton>(R.id.star5)
        )
        val txtLabel = dialogView.findViewById<android.widget.TextView>(R.id.txtRatingLabel)

        fun updateStars(rating: Int) {
            currentRating = rating
            stars.forEachIndexed { index, star ->
                star.setImageResource(if (index < rating) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
            }
            txtLabel.text = when (rating) {
                1 -> "Muito ruim"
                2 -> "Ruim"
                3 -> "Regular"
                4 -> "Bom"
                5 -> "Excelente!"
                else -> "Toque para avaliar"
            }
        }

        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                updateStars(index + 1)
                if (currentRating > 0) {
                    val rating = when (currentRating) {
                        5 -> TranslationRating.EXCELLENT
                        4 -> TranslationRating.GOOD
                        3 -> TranslationRating.AVERAGE
                        2 -> TranslationRating.POOR
                        else -> TranslationRating.BAD
                    }
                    statsManager.rateTranslation(recordId, rating)
                    syncWithGitHub()
                }
                dialog.dismiss()
            }
        }

        dialogView.findViewById<View>(R.id.btnSkip).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    
    private fun syncWithGitHub() {
        scope.launch {
            val pending = githubSync.getPendingCount()
            if (pending > 0) {
                val result = githubSync.syncWithGitHub()
                result.onSuccess {
                    Toast.makeText(context, "✓ Estatísticas sincronizadas!", Toast.LENGTH_SHORT).show()
                }
                // Se falhar, os dados ficam na fila para próxima tentativa
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadLanguages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }
}
