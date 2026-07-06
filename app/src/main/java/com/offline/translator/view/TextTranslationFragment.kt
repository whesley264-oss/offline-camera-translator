package com.offline.translator.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
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
    private var isInitializing = true
    private var isTranslating = false

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
            Toast.makeText(context, "Idiomas trocados!", Toast.LENGTH_SHORT).show()
        }

        binding.spinnerSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializing) {
                    isInitializing = false
                    return
                }
                if (position < downloadedLanguages.size && !isTranslating) {
                    val newSource = downloadedLanguages[position].code
                    if (newSource == selectedTarget) {
                        selectedTarget = selectedSource
                        selectedSource = newSource
                        updateSpinnerSelections()
                        Toast.makeText(context, "Ajuste automático!", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedSource = newSource
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerTarget.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializing || isTranslating) return
                if (position < downloadedLanguages.size) {
                    val newTarget = downloadedLanguages[position].code
                    if (newTarget == selectedSource) {
                        selectedSource = selectedTarget
                        selectedTarget = newTarget
                        updateSpinnerSelections()
                        Toast.makeText(context, "Ajuste automático!", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedTarget = newTarget
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnTranslate.setOnClickListener {
            if (!isTranslating) {
                feedbackManager.animatePulse(it)
                translateText()
            }
        }

        binding.btnLibrary.setOnClickListener {
            startActivity(Intent(requireContext(), LanguageLibraryActivity::class.java))
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.editTextOutput.text.toString()
            if (text.isNotBlank()) {
                copyToClipboard(text)
            }
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
            try {
                val mlKitDownloaded = translationService.getDownloadedModels()
                val savedDownloaded = downloadManager.getDownloadedLanguages()
                val combinedDownloads = (mlKitDownloaded + savedDownloaded).toSet()

                if (combinedDownloads.isEmpty()) {
                    showNoLanguagesWarning()
                    return@launch
                }

                downloadedLanguages = Language.SUPPORTED_LANGUAGES.filter { combinedDownloads.contains(it.code) }

                if (downloadedLanguages.size < 2) {
                    showNoLanguagesWarning()
                    return@launch
                }

                binding.btnTranslate.isEnabled = true

                val ctx = requireContext()
                val sourceNames = downloadedLanguages.map { it.name }
                val targetNames = downloadedLanguages.map { it.name }

                val sourceAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, sourceNames)
                sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSource.adapter = sourceAdapter

                val targetAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, targetNames)
                targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTarget.adapter = targetAdapter

                isInitializing = true
                val sourceIndex = downloadedLanguages.indexOfFirst { it.code == selectedSource }.takeIf { it >= 0 } ?: 0
                val targetIndex = downloadedLanguages.indexOfFirst { it.code == selectedTarget }.takeIf { it >= 0 } ?: 1
                binding.spinnerSource.setSelection(sourceIndex)
                binding.spinnerTarget.setSelection(targetIndex)
                isInitializing = false

                Toast.makeText(context, "✓ ${downloadedLanguages.size} idiomas carregados!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading languages: ${e.message}")
                Toast.makeText(context, "Erro ao carregar idiomas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNoLanguagesWarning() {
        binding.btnTranslate.isEnabled = false
        Toast.makeText(context, "⚠️ Baixe idiomas na Biblioteca!", Toast.LENGTH_LONG).show()
    }

    private fun updateSpinnerSelections() {
        val sourceIndex = downloadedLanguages.indexOfFirst { it.code == selectedSource }
        val targetIndex = downloadedLanguages.indexOfFirst { it.code == selectedTarget }
        isInitializing = true
        if (sourceIndex >= 0) binding.spinnerSource.setSelection(sourceIndex)
        if (targetIndex >= 0) binding.spinnerTarget.setSelection(targetIndex)
        isInitializing = false
    }

    private fun translateText() {
        val inputText = binding.editTextInput.text.toString().trim()
        
        if (inputText.isEmpty()) {
            Toast.makeText(context, "Digite um texto", Toast.LENGTH_SHORT).show()
            return
        }

        if (downloadedLanguages.isEmpty()) {
            Toast.makeText(context, "Baixe idiomas na Biblioteca!", Toast.LENGTH_LONG).show()
            return
        }

        isTranslating = true
        binding.btnTranslate.isEnabled = false
        binding.btnTranslate.text = "Traduzindo..."

        scope.launch {
            try {
                val result = translationService.translate(inputText, selectedSource, selectedTarget)
                
                withContext(Dispatchers.Main) {
                    isTranslating = false
                    binding.btnTranslate.isEnabled = true
                    binding.btnTranslate.text = "TRADUZIR"
                }

                result.fold(
                    onSuccess = { translated ->
                        withContext(Dispatchers.Main) {
                            binding.editTextOutput.setText(translated)
                            feedbackManager.vibrateOnTranslate()
                            feedbackManager.animateSuccess(binding.editTextOutput)
                        }

                        TranslationWidgetProvider.updateLastTranslation(requireContext(), inputText, translated)

                        try {
                            val recordId = statsManager.saveTranslation(
                                inputText, translated, selectedSource, selectedTarget, TranslationType.TEXT
                            )
                            historyManager.saveTranslation(
                                inputText, translated, selectedSource, selectedTarget, "text"
                            )
                            showRatingDialog(recordId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving: ${e.message}")
                        }
                    },
                    onFailure = { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            feedbackManager.vibrateOnError()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isTranslating = false
                    binding.btnTranslate.isEnabled = true
                    binding.btnTranslate.text = "TRADUZIR"
                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showRatingDialog(recordId: Long) {
        if (context == null) return

        try {
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
                        dialog.dismiss()
                    }
                }
            }

            dialogView.findViewById<View>(R.id.btnSkip).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog: ${e.message}")
        }
    }

    private fun syncWithGitHub() {
        scope.launch {
            try {
                val pending = githubSync.getPendingCount()
                if (pending > 0) {
                    val result = githubSync.syncWithGitHub()
                    result.onSuccess {
                        Toast.makeText(context, "✓ Sincronizado!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync error: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadLanguages()
    }

    override fun onPause() {
        super.onPause()
        if (::tts.isInitialized) {
            tts.stop()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        if (::tts.isInitialized) {
            tts.shutdown()
        }
        _binding = null
    }

    companion object {
        private const val TAG = "TextTranslationFragment"
    }
}
