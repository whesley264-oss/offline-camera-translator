package com.offline.translator.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.offline.translator.R
import com.offline.translator.databinding.ActivitySettingsBinding
import com.offline.translator.model.PreferencesManager
import com.offline.translator.service.ClipboardTranslationService

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager
    
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            prefs.setClipboardTranslation(true)
            startClipboardService()
            binding.switchClipboard.isChecked = true
        } else {
            binding.switchClipboard.isChecked = false
            Toast.makeText(this, "Permissão necessária para notificações", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // Theme toggle
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.setDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Haptic toggle
        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            prefs.setHaptic(isChecked)
        }

        // Animations toggle
        binding.switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAnimations(isChecked)
        }
        
        // Clipboard translation toggle
        binding.switchClipboard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }
                prefs.setClipboardTranslation(true)
                startClipboardService()
            } else {
                prefs.setClipboardTranslation(false)
                stopClipboardService()
            }
        }

        // Font size spinner
        val fontSizes = arrayOf("Pequeno", "Médio", "Grande", "Extra Grande")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontSizes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFontSize.adapter = adapter

        binding.spinnerFontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.setFontSize(position)
                binding.txtFontSize.text = fontSizes[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Auto-detect toggle
        binding.switchAutoDetect.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAutoDetect(isChecked)
        }
        
        // Auto-start clipboard service if enabled
        if (prefs.isClipboardTranslationEnabled()) {
            startClipboardService()
        }

        // Languages button
        binding.btnLanguages.setOnClickListener {
            startActivity(Intent(this, LanguageLibraryActivity::class.java))
        }

        // Widgets button
        binding.btnAddWidget.setOnClickListener {
            Toast.makeText(this, "Adicione o widget pela tela inicial do Android", Toast.LENGTH_LONG).show()
        }

        // Clear cache
        binding.btnClearCache.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Limpar Cache")
                .setMessage("Isso removerá os idiomas baixados. Você precisará baixá-los novamente.")
                .setPositiveButton("Limpar") { _, _ ->
                    val modelDir = getDir("models", MODE_PRIVATE)
                    modelDir.deleteRecursively()
                    Toast.makeText(this, "Cache limpo!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun loadSettings() {
        // Theme
        binding.switchTheme.isChecked = prefs.isDarkMode()
        
        // Haptic
        binding.switchHaptic.isChecked = prefs.isHapticEnabled()
        
        // Animations
        binding.switchAnimations.isChecked = prefs.isAnimationsEnabled()

        // Font size
        binding.spinnerFontSize.setSelection(prefs.getFontSize())
        val fontSizes = arrayOf("Pequeno", "Médio", "Grande", "Extra Grande")
        binding.txtFontSize.text = fontSizes[prefs.getFontSize()]

        // Auto-detect
        binding.switchAutoDetect.isChecked = prefs.isAutoDetect()
        
        // Clipboard translation
        binding.switchClipboard.isChecked = prefs.isClipboardTranslationEnabled()
    }
    
    private fun startClipboardService() {
        val intent = Intent(this, ClipboardTranslationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun stopClipboardService() {
        val intent = Intent(this, ClipboardTranslationService::class.java)
        intent.action = ClipboardTranslationService.ACTION_STOP
        startService(intent)
    }
}