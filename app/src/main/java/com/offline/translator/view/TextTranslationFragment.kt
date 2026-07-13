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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.offline.translator.R
import com.offline.translator.databinding.FragmentTextTranslationBinding
import com.offline.translator.model.*
import com.offline.translator.widget.TranslationWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
            Toast.makeText(context, "Idiomas trocados!", Toast.LENGTH_SHORT).show()
        }

        binding.spinnerSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializing) {
                    isInitializing = false
                    return
                }
                if (position < downloadedLanguages.size) {
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
                if (isInitializing) return
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
            translateText()
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

        Log.d(TAG, "Translating: '$inputText' from $selectedSource to $selectedTarget")

        binding.btnTranslate.isEnabled = false
        binding.btnTranslate.text = "Traduzindo..."

        scope.launch {
            try {
                Log.d(TAG, "Starting translation...")
                val result = translationService.translate(inputText, selectedSource, selectedTarget)
                Log.d(TAG, "Translation completed, updating UI...")
                
                result.fold(
                    onSuccess = { translated ->
                        Log.d(TAG, "Translation result: '$translated'")
                        binding.editTextOutput.setText(translated)
                        binding.btnTranslate.isEnabled = true
                        binding.btnTranslate.text = "TRADUZIR"
                        Toast.makeText(context, "✓ Tradução completa!", Toast.LENGTH_SHORT).show()

                        TranslationWidgetProvider.updateLastTranslation(requireContext(), inputText, translated)

                        try {
                            val recordId = statsManager.saveTranslation(
                                inputText, translated, selectedSource, selectedTarget, TranslationType.TEXT
                            )
                            historyManager.saveTranslation(
                                inputText, translated, selectedSource, selectedTarget, "text"
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving: ${e.message}")
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Translation failed: ${e.message}")
                        binding.btnTranslate.isEnabled = true
                        binding.btnTranslate.text = "TRADUZIR"
                        Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}")
                binding.btnTranslate.isEnabled = true
                binding.btnTranslate.text = "TRADUZIR"
                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
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
