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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            "SWAP_LANGUAGES" -> {
                val prefs = PreferencesManager(context)
                val source = prefs.getSourceLanguage()
                val target = prefs.getTargetLanguage()
                prefs.setSourceLanguage(target)
                prefs.setTargetLanguage(source)
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    Intent(context, TranslationWidgetProvider::class.java).component
                )
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            "OPEN_CAMERA" -> {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("open_tab", "camera")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(openIntent)
            }
            "OPEN_TEXT" -> {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("open_tab", "text")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(openIntent)
            }
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
            
            val prefs = PreferencesManager(context)
            val source = prefs.getSourceLanguage().uppercase()
            val target = prefs.getTargetLanguage().uppercase()
            
            views.setTextViewText(R.id.txtLanguagePair, "$source → $target")
            
            val widgetPrefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val lastSource = widgetPrefs.getString("last_source", null)
            val lastTranslation = widgetPrefs.getString("last_translation", "Toque para traduzir") ?: "Toque para traduzir"
            
            views.setTextViewText(R.id.txtSourceText, lastSource ?: "Texto original")
            views.setTextViewText(R.id.txtLastTranslation, lastTranslation)
            
            // Open app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnOpen, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent)
            views.setOnClickPendingIntent(R.id.translationContainer, openPendingIntent)
            
            // Swap languages
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
            
            // Open camera
            val cameraIntent = Intent(context, TranslationWidgetProvider::class.java).apply {
                action = "OPEN_CAMERA"
            }
            val cameraPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                cameraIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnCamera, cameraPendingIntent)
            
            // Open text
            val textIntent = Intent(context, TranslationWidgetProvider::class.java).apply {
                action = "OPEN_TEXT"
            }
            val textPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                textIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnText, textPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        fun updateLastTranslation(context: Context, sourceText: String, translatedText: String) {
            context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_source", sourceText)
                .putString("last_translation", translatedText)
                .apply()
            
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
