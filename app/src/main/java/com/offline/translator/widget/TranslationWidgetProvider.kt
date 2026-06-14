package com.offline.translator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.offline.translator.R
import com.offline.translator.model.PreferencesManager
import com.offline.translator.view.MainActivity

class TranslationWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget added
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_translation)
            
            // Get saved preferences
            val prefs = PreferencesManager(context)
            val source = prefs.getSourceLanguage().uppercase()
            val target = prefs.getTargetLanguage().uppercase()
            
            views.setTextViewText(R.id.txtLanguagePair, "$source → $target")
            
            // Get last translation from SharedPreferences
            val lastTranslation = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                .getString("last_translation", "Toque para traduzir") ?: "Toque para traduzir"
            
            views.setTextViewText(R.id.txtLastTranslation, lastTranslation)
            
            // Create intent to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            views.setOnClickPendingIntent(R.id.btnOpen, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            // Swap languages button
            val swapIntent = Intent(context, TranslationWidgetProvider::class.java).apply {
                action = "SWAP_LANGUAGES"
            }
            val swapPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                swapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnSwap, swapPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        fun updateLastTranslation(context: Context, translation: String) {
            context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_translation", translation)
                .apply()
            
            // Update all widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                Intent(context, TranslationWidgetProvider::class.java).component
            )
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}