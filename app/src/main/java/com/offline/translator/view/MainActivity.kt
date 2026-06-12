package com.offline.translator.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.offline.translator.R
import com.offline.translator.databinding.ActivityMainBinding
import com.offline.translator.model.Language
import com.offline.translator.model.TextRecognitionService
import com.offline.translator.model.TranslationService
import com.offline.translator.presenter.TranslationPresenter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TranslationView {
    private lateinit var binding: ActivityMainBinding
    private lateinit var presenter: TranslationPresenter
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var translationService: TranslationService
    private lateinit var textRecognitionService: TextRecognitionService
    
    private var selectedSourceLang = Language.DEFAULT_SOURCE
    private var selectedTargetLang = Language.DEFAULT_TARGET
    private var sourceLanguages: List<Language> = emptyList()
    private var targetLanguages: List<Language> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        translationService = TranslationService(this)
        textRecognitionService = TextRecognitionService()
        presenter = TranslationPresenter(this, translationService, textRecognitionService)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        
        // Start downloading base languages (en, pt) in background
        downloadBaseLanguages()
        
        if (allPermissionsGranted()) {
            startCameraLifecycle()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }
    
    private fun downloadBaseLanguages() {
        binding.txtTranslationOverlay.text = "Baixando idiomas..."
        
        // Show a brief message about downloading
        Toast.makeText(this, "Baixando modelos de tradução (en→pt)...", Toast.LENGTH_LONG).show()
    }
    
    private fun setupUI() {
        binding.btnDownloadSource.setOnClickListener {
            showDownloadDialog(true)
        }
        
        binding.btnDownloadTarget.setOnClickListener {
            showDownloadDialog(false)
        }
        
        binding.btnSwapLanguages.setOnClickListener {
            val temp = selectedSourceLang
            selectedSourceLang = selectedTargetLang
            selectedTargetLang = temp
            updateSpinnerSelections()
            presenter.onSourceLanguageChanged(selectedSourceLang)
            presenter.onTargetLanguageChanged(selectedTargetLang)
        }
    }
    
    private fun showDownloadDialog(isSource: Boolean) {
        val languages = if (isSource) sourceLanguages else targetLanguages
        // Filter out bundled languages and already downloaded languages
        val downloadableLanguages = languages.filter { 
            !translationService.baseLanguages.contains(it.code) && 
            !it.isDownloaded && 
            !it.isDownloading 
        }
        
        if (downloadableLanguages.isEmpty()) {
            Toast.makeText(this, "Todos os idiomas já estão disponíveis!", Toast.LENGTH_SHORT).show()
            return
        }
        
        val langNames = downloadableLanguages.map { "${it.name} (Toque para baixar)" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle(if (isSource) "Baixar Idioma de Origem" else "Baixar Idioma de Destino")
            .setItems(langNames) { _, which ->
                val selectedLang = downloadableLanguages[which]
                presenter.downloadLanguage(selectedLang.code)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun setupSpinners() {
        // Source spinner
        binding.spinnerSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < sourceLanguages.size) {
                    selectedSourceLang = sourceLanguages[position].code
                    presenter.onSourceLanguageChanged(selectedSourceLang)
                    presenter.updateLanguageLists()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Target spinner
        binding.spinnerTarget.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < targetLanguages.size) {
                    selectedTargetLang = targetLanguages[position].code
                    presenter.onTargetLanguageChanged(selectedTargetLang)
                    presenter.updateLanguageLists()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun updateSpinnerSelections() {
        val sourceIndex = sourceLanguages.indexOfFirst { it.code == selectedSourceLang }
        val targetIndex = targetLanguages.indexOfFirst { it.code == selectedTargetLang }
        
        if (sourceIndex >= 0) binding.spinnerSource.setSelection(sourceIndex)
        if (targetIndex >= 0) binding.spinnerTarget.setSelection(targetIndex)
    }

    private fun startCameraLifecycle() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (xc: Exception) {
                Log.e("MainActivity", "Camera binding crash averted", xc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        presenter.processFrame(bitmap)
        imageProxy.close()
    }

    override fun showTranslation(translatedText: String) {
        binding.txtTranslationOverlay.text = translatedText
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } 

    override fun getSelectedSourceLanguage(): String = selectedSourceLang
    
    override fun getSelectedTargetLanguage(): String = selectedTargetLang

    override fun updateLanguageLists(sourceLanguages: List<Language>, targetLanguages: List<Language>) {
        this.sourceLanguages = sourceLanguages
        this.targetLanguages = targetLanguages
        
        val sourceAdapter = ArrayAdapter(
            this, 
            android.R.layout.simple_spinner_item,
            sourceLanguages.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        val targetAdapter = ArrayAdapter(
            this, 
            android.R.layout.simple_spinner_item,
            targetLanguages.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        binding.spinnerSource.adapter = sourceAdapter
        binding.spinnerTarget.adapter = targetAdapter
        
        if (::presenter.isInitialized) {
            setupSpinners()
        }
        
        updateSpinnerSelections()
    }
    
    override fun updateDownloadProgress(language: Language, isDownloading: Boolean) {
        presenter.updateLanguageLists()
    }
    
    override fun showDownloadSuccess(language: Language) {
        Toast.makeText(this, "${language.name} baixado com sucesso!", Toast.LENGTH_SHORT).show()
    }
    
    override fun showDownloadError(language: Language, error: String) {
        Toast.makeText(this, "Erro ao baixar ${language.name}: $error", Toast.LENGTH_LONG).show()
    }
    
    override fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.cleanup()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    } 
}