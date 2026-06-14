package com.offline.translator.view

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.offline.translator.R
import com.offline.translator.databinding.FragmentImageTranslationBinding
import com.offline.translator.model.*
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ImageTranslationFragment : Fragment() {
    private var _binding: FragmentImageTranslationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var translationService: TranslationService
    private lateinit var textRecognitionService: TextRecognitionService
    private lateinit var statsManager: StatsManager
    private lateinit var githubSync: GitHubStatsSync
    private lateinit var historyManager: HistoryManager
    private lateinit var feedbackManager: FeedbackManager
    private lateinit var tts: TextToSpeechService
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var downloadedLanguages: List<Language> = emptyList()
    private var selectedSource = "en"
    private var selectedTarget = "pt"
    private var lastDetectedText: String = ""
    private var lastTranslatedText: String = ""

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentImageTranslationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        translationService = TranslationService(requireContext())
        textRecognitionService = TextRecognitionService()
        statsManager = StatsManager(requireContext())
        githubSync = GitHubStatsSync(requireContext())
        historyManager = HistoryManager(requireContext())
        feedbackManager = FeedbackManager(requireContext(), PreferencesManager(requireContext()))
        tts = TextToSpeechService(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupUI()
        loadLanguages()
        
        if (hasCameraPermission()) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    private fun setupUI() {
        binding.btnSwap.setOnClickListener {
            val temp = selectedSource
            selectedSource = selectedTarget
            selectedTarget = temp
            updateSpinnerSelections()
            feedbackManager.animatePulse(it)
        }
        
        binding.btnCapture.setOnClickListener {
            feedbackManager.animatePulse(it)
            captureAndTranslate()
        }
        
        binding.btnSelectArea.setOnClickListener {
            binding.selectionOverlay.visibility = 
                if (binding.selectionOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.btnLibrary.setOnClickListener {
            startActivity(Intent(requireContext(), LanguageLibraryActivity::class.java))
        }
        
        binding.btnCopy.setOnClickListener {
            if (lastTranslatedText.isNotBlank()) copyToClipboard(lastTranslatedText)
        }
        
        binding.btnShare.setOnClickListener {
            if (lastTranslatedText.isNotBlank()) shareText(lastTranslatedText)
        }
        
        binding.btnSpeak.setOnClickListener {
            if (lastTranslatedText.isNotBlank()) speakText(lastTranslatedText)
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tradução", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "✓ Copiado!", Toast.LENGTH_SHORT).show()
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
            downloadedLanguages = Language.SUPPORTED_LANGUAGES.filter { downloaded.contains(it.code) }
            
            if (downloadedLanguages.isEmpty()) {
                Toast.makeText(context, "Baixe idiomas na Biblioteca primeiro!", Toast.LENGTH_LONG).show()
                binding.btnCapture.isEnabled = false
                return@launch
            }
            
            binding.btnCapture.isEnabled = true
            
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
    
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(context, "Erro na câmera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun captureAndTranslate() {
        if (downloadedLanguages.isEmpty()) {
            Toast.makeText(context, "Baixe idiomas primeiro!", Toast.LENGTH_LONG).show()
            return
        }

        val capture = imageCapture ?: return
        
        val sourcePos = binding.spinnerSource.selectedItemPosition
        val targetPos = binding.spinnerTarget.selectedItemPosition
        if (sourcePos >= 0 && sourcePos < downloadedLanguages.size) {
            selectedSource = downloadedLanguages[sourcePos].code
        }
        if (targetPos >= 0 && targetPos < downloadedLanguages.size) {
            selectedTarget = downloadedLanguages[targetPos].code
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.txtResult.visibility = View.GONE
        
        capture.takePicture(ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                image.close()
                processImage(bitmap)
            }
            override fun onError(exception: ImageCaptureException) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Erro: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun processImage(bitmap: Bitmap) {
        scope.launch {
            val rect = binding.selectionOverlay.getSelectionRect()
            val finalBitmap = if (rect.width() > 50 && rect.height() > 50) {
                val scaleX = bitmap.width.toFloat() / binding.viewFinder.width
                val scaleY = bitmap.height.toFloat() / binding.viewFinder.height
                Bitmap.createBitmap(bitmap, 
                    (rect.left * scaleX).toInt().coerceIn(0, bitmap.width),
                    (rect.top * scaleY).toInt().coerceIn(0, bitmap.height),
                    (rect.width() * scaleX).toInt().coerceIn(1, bitmap.width),
                    (rect.height() * scaleY).toInt().coerceIn(1, bitmap.height))
            } else bitmap
            
            val ocrResult = textRecognitionService.recognizeText(finalBitmap)
            ocrResult.fold(
                onSuccess = { text ->
                    if (text.isBlank()) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Nenhum texto detectado", Toast.LENGTH_SHORT).show()
                        return@fold
                    }
                    lastDetectedText = text
                    val translateResult = translationService.translate(text, selectedSource, selectedTarget)
                    binding.progressBar.visibility = View.GONE
                    translateResult.fold(
                        onSuccess = { translated ->
                            lastTranslatedText = translated
                            binding.txtResult.text = translated
                            binding.txtResult.visibility = View.VISIBLE
                            
                            feedbackManager.vibrateOnTranslate()
                            feedbackManager.animateSlideUp(binding.txtResult)
                            
                            val recordId = statsManager.saveTranslation(
                                text, translated, selectedSource, selectedTarget, TranslationType.IMAGE
                            )
                            
                            historyManager.saveTranslation(
                                text, translated, selectedSource, selectedTarget, "image"
                            )
                            
                            showRatingDialog(recordId)
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            feedbackManager.vibrateOnError()
                        }
                    )
                },
                onFailure = { e ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Erro OCR: ${e.message}", Toast.LENGTH_LONG).show()
                    feedbackManager.vibrateOnError()
                }
            )
        }
    }
    
    private fun showRatingDialog(recordId) {
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
                    
                    
                    // Try to sync with GitHub
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
        cameraExecutor.shutdown()
        _binding = null
    }
}
