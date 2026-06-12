package com.offline.translator.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.offline.translator.databinding.ActivityMainBinding
import com.offline.translator.model.NativeTranslationEngine
import com.offline.translator.model.TranslationModelImpl
import com.offline.translator.presenter.TranslationPresenter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TranslationView {
    private lateinit var binding: ActivityMainBinding
    private lateinit var presenter: TranslationPresenter
    private lateinit var cameraExecutor: ExecutorService
    private var selectedDictPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val engine = NativeTranslationEngine()
        val model = TranslationModelImpl(engine)
        presenter = TranslationPresenter(this, model)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCameraLifecycle()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        presenter.scanAvailableDictionaries(filesDir)
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
                        val mockDetectedFrameText = "hello text world binary"
                        runOnUiThread {
                            presenter.processCapturedText(mockDetectedFrameText)
                        } 
                        imageProxy.close()
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

    override fun showTranslation(translatedText: String) {
        binding.txtTranslationOverlay.text = translatedText
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } 

    override fun getSelectedDictionaryPath(): String = selectedDictPath

    override fun updateDictionaryList(files: List<String>) {
        if (files.isEmpty()) {
            binding.spinnerDict.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Nenhum selecionado"))
            return
        }
        val shortNames = files.map { it.substringAfterLast("/") }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shortNames)
        binding.spinnerDict.adapter = adapter
        selectedDictPath = files[0]
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    } 
}