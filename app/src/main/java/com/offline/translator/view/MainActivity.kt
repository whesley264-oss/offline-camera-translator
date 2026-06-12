package com.offline.translator.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
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
    private var imageCapture: ImageCapture? = null
    
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
        
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }
    
    private fun setupUI() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, LanguageLibraryActivity::class.java))
        }
        
        binding.btnSwapLanguages.setOnClickListener {
            val temp = selectedSourceLang
            selectedSourceLang = selectedTargetLang
            selectedTargetLang = temp
            updateSpinnerSelections()
        }
        
        binding.btnQuality.setOnClickListener { showQualitySettings() }
        
        binding.btnSelectArea.setOnClickListener {
            binding.selectionOverlay.visibility = 
                if (binding.selectionOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        
        // Capture button - take photo and translate
        binding.btnCapture.setOnClickListener {
            captureAndTranslate()
        }
        
        binding.selectionOverlay.onSelectionChanged = { rect ->
            processSelectedArea(rect)
        }
    }
    
    private fun captureAndTranslate() {
        val imageCapture = imageCapture ?: return
        
        showLoading(true)
        binding.txtTranslationOverlay.visibility = View.GONE
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()
                    
                    // Check if user selected an area
                    val selectionRect = binding.selectionOverlay.getSelectionRect()
                    if (selectionRect.width() > 50 && selectionRect.height() > 50) {
                        processSelectedArea(selectionRect)
                    } else {
                        // Process full image
                        presenter.processFrame(bitmap)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    showLoading(false)
                    showError("Erro ao capturar imagem: ${exception.message}")
                }
            }
        )
    }
    
    private fun processSelectedArea(rect: RectF) {
        val bitmap = binding.viewFinder.bitmap ?: return
        
        val scaleX = bitmap.width.toFloat() / binding.viewFinder.width
        val scaleY = bitmap.height.toFloat() / binding.viewFinder.height
        
        val cropX = (rect.left * scaleX).toInt().coerceIn(0, bitmap.width)
        val cropY = (rect.top * scaleY).toInt().coerceIn(0, bitmap.height)
        val cropWidth = (rect.width() * scaleX).toInt().coerceIn(1, bitmap.width - cropX)
        val cropHeight = (rect.height() * scaleY).toInt().coerceIn(1, bitmap.height - cropY)
        
        if (cropWidth > 10 && cropHeight > 10) {
            val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
            presenter.processFrame(croppedBitmap)
        }
    }
    
    private fun showQualitySettings() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quality_settings, null)
        
        AlertDialog.Builder(this)
            .setTitle("Configurações de Imagem")
            .setView(dialogView)
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (xc: Exception) {
                Log.e("MainActivity", "Camera binding failed", xc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun showTranslation(translatedText: String) {
        showLoading(false)
        binding.txtTranslationOverlay.text = translatedText
        binding.txtTranslationOverlay.visibility = View.VISIBLE
    }

    override fun showError(message: String) {
        showLoading(false)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } 

    override fun getSelectedSourceLanguage(): String = selectedSourceLang
    override fun getSelectedTargetLanguage(): String = selectedTargetLang

    override fun updateLanguageLists(sourceLanguages: List<Language>, targetLanguages: List<Language>) {
        this.sourceLanguages = sourceLanguages
        this.targetLanguages = targetLanguages
        
        val sourceAdapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            sourceLanguages.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        val targetAdapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            targetLanguages.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        binding.spinnerSource.adapter = sourceAdapter
        binding.spinnerTarget.adapter = targetAdapter
        updateSpinnerSelections()
    }
    
    private fun updateSpinnerSelections() {
        val sourceIndex = sourceLanguages.indexOfFirst { it.code == selectedSourceLang }
        val targetIndex = targetLanguages.indexOfFirst { it.code == selectedTargetLang }
        if (sourceIndex >= 0) binding.spinnerSource.setSelection(sourceIndex)
        if (targetIndex >= 0) binding.spinnerTarget.setSelection(targetIndex)
    }
    
    override fun updateDownloadProgress(language: Language, isDownloading: Boolean) {}
    override fun showDownloadSuccess(language: Language) {}
    override fun showDownloadError(language: Language, error: String) {}
    
    override fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnCapture.isEnabled = !isLoading
    }
    
    override fun showDetectedTextBoxes(boxes: List<RectF>) {
        binding.selectionOverlay.setDetectedTextBoxes(boxes)
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