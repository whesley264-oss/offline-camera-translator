package com.offline.translator.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.offline.translator.R
import com.offline.translator.databinding.ActivitySettingsBinding
import com.offline.translator.model.PreferencesManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager

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
    }
}