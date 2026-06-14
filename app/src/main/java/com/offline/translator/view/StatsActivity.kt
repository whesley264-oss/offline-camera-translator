package com.offline.translator.view

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.offline.translator.R
import com.offline.translator.model.StatsManager
import com.offline.translator.model.TranslationStats

class StatsActivity : AppCompatActivity() {
    private lateinit var statsManager: StatsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        statsManager = StatsManager(this)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnClear).setOnClickListener { showClearDialog() }

        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun loadStats() {
        val stats = statsManager.getStats()

        findViewById<TextView>(R.id.txtTotalCount).text = stats.totalTranslations.toString()
        findViewById<TextView>(R.id.txtRatedCount).text = stats.ratedCount.toString()
        findViewById<TextView>(R.id.txtSuccessRate).text = "%.1f%%".format(stats.successRate)
        findViewById<TextView>(R.id.txtAvgRating).text = "%.1f".format(stats.averageRating)
        findViewById<TextView>(R.id.txtTextCount).text = stats.textTranslations.toString()
        findViewById<TextView>(R.id.txtImageCount).text = stats.imageTranslations.toString()

        // Rating distribution
        updateProgressBar(R.id.progress5, R.id.txtCount5, stats.excellentCount, stats.ratedCount)
        updateProgressBar(R.id.progress4, R.id.txtCount4, stats.goodCount, stats.ratedCount)
        updateProgressBar(R.id.progress3, R.id.txtCount3, stats.averageCount, stats.ratedCount)
        updateProgressBar(R.id.progress2, R.id.txtCount2, stats.poorCount, stats.ratedCount)
        updateProgressBar(R.id.progress1, R.id.txtCount1, stats.badCount, stats.ratedCount)
    }

    private fun updateProgressBar(progressId: Int, countId: Int, count: Int, total: Int) {
        findViewById<ProgressBar>(progressId).progress = if (total > 0) (count * 100 / total) else 0
        findViewById<TextView>(countId).text = count.toString()
    }

    private fun showClearDialog() {
        AlertDialog.Builder(this)
            .setTitle("Limpar Estatísticas")
            .setMessage("Tem certeza que deseja apagar todas as estatísticas?")
            .setPositiveButton("Limpar") { _, _ ->
                statsManager.clearAll()
                loadStats()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}