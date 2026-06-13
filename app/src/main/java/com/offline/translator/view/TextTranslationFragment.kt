package com.offline.translator.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.offline.translator.databinding.FragmentTextTranslationBinding
import com.offline.translator.model.Language
import com.offline.translator.model.TranslationService
import kotlinx.coroutines.*

class TextTranslationFragment : Fragment() {
    private var _binding: FragmentTextTranslationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var translationService: TranslationService
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var sourceLanguages: List<Language> = emptyList()
    private var targetLanguages: List<Language> = emptyList()
    private var selectedSource = Language.DEFAULT_SOURCE
    private var selectedTarget = Language.DEFAULT_TARGET

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTextTranslationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        translationService = TranslationService(requireContext())
        setupUI()
        loadLanguages()
    }
    
    private fun setupUI() {
        binding.btnSwap.setOnClickListener {
            val temp = selectedSource
            selectedSource = selectedTarget
            selectedTarget = temp
            updateSpinnerSelections()
        }
        
        binding.btnTranslate.setOnClickListener {
            translateText()
        }
    }
    
    private fun loadLanguages() {
        scope.launch {
            val downloaded = translationService.getDownloadedModels()
            sourceLanguages = Language.SUPPORTED_LANGUAGES.map { it.copy(isDownloaded = downloaded.contains(it.code)) }
            targetLanguages = sourceLanguages.filter { it.code != selectedSource }
            
            val sourceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sourceLanguages.map { it.name })
            sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSource.adapter = sourceAdapter
            
            val targetAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, targetLanguages.map { it.name })
            targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerTarget.adapter = targetAdapter
            
            updateSpinnerSelections()
        }
    }
    
    private fun updateSpinnerSelections() {
        val sourceIndex = sourceLanguages.indexOfFirst { it.code == selectedSource }
        val targetIndex = targetLanguages.indexOfFirst { it.code == selectedTarget }
        if (sourceIndex >= 0) binding.spinnerSource.setSelection(sourceIndex)
        if (targetIndex >= 0) binding.spinnerTarget.setSelection(targetIndex)
    }
    
    private fun translateText() {
        val inputText = binding.editTextInput.text.toString().trim()
        if (inputText.isEmpty()) {
            Toast.makeText(context, "Digite um texto", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.btnTranslate.isEnabled = false
        binding.editTextOutput.setText("")
        
        scope.launch {
            val downloadResult = translationService.downloadLanguage(selectedSource, selectedTarget)
            if (downloadResult.isFailure) {
                Toast.makeText(context, "Erro: ${downloadResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                binding.btnTranslate.isEnabled = true
                return@launch
            }
            
            val result = translationService.translate(inputText, selectedSource, selectedTarget)
            binding.btnTranslate.isEnabled = true
            
            result.fold(
                onSuccess = { translated ->
                    binding.editTextOutput.setText(translated)
                },
                onFailure = { e ->
                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }
}
