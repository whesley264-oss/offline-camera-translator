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
import com.offline.translator.databinding.ActivitySettingsBinding
import com.offline.translator.model.PreferencesManager
import com.offline.translator.service.ClipboardTranslationService

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager
    private var isInitializing = true

    private val fontFamiliesValues = arrayOf(
        "default", "sans-serif", "serif", "monospace", "cursive",
        "sans-serif-black", "sans-serif-condensed", "sans-serif-medium",
        "sans-serif-thin", "serif-monospace", "monospace",
        "sans-serif-light", "sans-serif-condensed-medium", "sans-serif-condensed-light"
    )
    private val fontFamilies = arrayOf(
        "Padrão (Roboto)", "Sans-serif", "Serif", "Monospace", "Cursiva",
        "Archivo Black", "Montserrat", "Poppins", "Oswald", "Lobster",
        "Pacifico", "Dancing Script", "Righteous", "Bebas Neue"
    )
    private val fontSizes = arrayOf("Pequeno", "Médio", "Grande", "Extra Grande")

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
        prefs = PreferencesManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                prefs.setDarkMode(isChecked)
                finish()
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        // Font family spinner
        val fontFamilyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontFamilies)
        fontFamilyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFontFamily.adapter = fontFamilyAdapter

        binding.spinnerFontFamily.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitializing && position < fontFamiliesValues.size) {
                    prefs.setFontFamily(fontFamiliesValues[position])
                    binding.txtFontFamily.text = fontFamilies[position]
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Font size spinner
        val fontSizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontSizes)
        fontSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFontSize.adapter = fontSizeAdapter

        binding.spinnerFontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitializing) {
                    prefs.setFontSize(position)
                    binding.txtFontSize.text = fontSizes[position]
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.switchNotificationSound.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                prefs.setNotificationSound(isChecked)
                Toast.makeText(this, if (isChecked) "Som ativado" else "Som desativado", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchNotificationVibrate.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                prefs.setNotificationVibrate(isChecked)
                Toast.makeText(this, if (isChecked) "Vibração ativada" else "Vibração desativada", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                prefs.setAnimations(isChecked)
                Toast.makeText(this, if (isChecked) "Animações ativadas" else "Animações desativadas", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchClipboard.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@setOnCheckedChangeListener
                        }
                    }
                    prefs.setClipboardTranslation(true)
                    startClipboardService()
                    Toast.makeText(this, "Copiar & Traduzir ativado", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.setClipboardTranslation(false)
                    stopClipboardService()
                    Toast.makeText(this, "Copiar & Traduzir desativado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.switchAutoDetect.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                prefs.setAutoDetect(isChecked)
                Toast.makeText(this, if (isChecked) "Auto-detectar ativado" else "Auto-detectar desativado", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLanguages.setOnClickListener {
            Toast.makeText(this, "Abrindo Biblioteca de Idiomas...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LanguageLibraryActivity::class.java))
        }

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
        isInitializing = true

        binding.switchTheme.isChecked = prefs.isDarkMode()

        val fontFamilyIndex = fontFamiliesValues.indexOf(prefs.getFontFamily())
        if (fontFamilyIndex >= 0 && fontFamilyIndex < fontFamilies.size) {
            binding.spinnerFontFamily.setSelection(fontFamilyIndex)
            binding.txtFontFamily.text = fontFamilies[fontFamilyIndex]
        }

        val savedFontSize = prefs.getFontSize().coerceIn(0, fontSizes.size - 1)
        binding.spinnerFontSize.setSelection(savedFontSize)
        binding.txtFontSize.text = fontSizes[savedFontSize]

        binding.switchNotificationSound.isChecked = prefs.isNotificationSoundEnabled()
        binding.switchNotificationVibrate.isChecked = prefs.isNotificationVibrateEnabled()
        binding.switchAnimations.isChecked = prefs.isAnimationsEnabled()
        binding.switchAutoDetect.isChecked = prefs.isAutoDetect()
        binding.switchClipboard.isChecked = prefs.isClipboardTranslationEnabled()

        if (prefs.isClipboardTranslationEnabled()) {
            startClipboardService()
        }

        isInitializing = false
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
