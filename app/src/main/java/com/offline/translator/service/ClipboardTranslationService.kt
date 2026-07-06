package com.offline.translator.service

import android.app.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.offline.translator.R
import com.offline.translator.model.PreferencesManager
import com.offline.translator.view.MainActivity
import kotlinx.coroutines.*

class ClipboardTranslationService : Service() {
    
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var translationService: TranslationService
    private lateinit var textRecognitionService: TextRecognitionService
    private lateinit var preferencesManager: PreferencesManager
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var lastClipboardText: String = ""
    private var isProcessing = false
    
    companion object {
        const val CHANNEL_ID = "pelko_clipboard_channel"
        const val NOTIFICATION_ID = 1001
        const val FOREGROUND_NOTIFICATION_ID = 1000
        
        const val ACTION_TRANSLATE = "com.offline.translator.ACTION_TRANSLATE"
        const val ACTION_COPY_RESULT = "com.offline.translator.ACTION_COPY_RESULT"
        const val ACTION_SWAP = "com.offline.translator.ACTION_SWAP"
        const val ACTION_STOP = "com.offline.translator.ACTION_STOP"
        
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        
        createNotificationChannel()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        translationService = TranslationService(this)
        textRecognitionService = TextRecognitionService()
        preferencesManager = PreferencesManager(this)
        
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        
        // Show foreground notification
        showForegroundNotification()
    }
    
    private fun showForegroundNotification() {
        val view = RemoteViews(packageName, R.layout.notification_translation_collapsed).apply {
            setTextViewText(R.id.txtTitle, "🔤 Copiar & Traduzir ativo")
            setTextViewText(R.id.txtSource, "Copie qualquer texto para traduzir")
            setTextViewText(R.id.txtLanguages, "")
        }
        
        val stopIntent = Intent(this, ClipboardTranslationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_notification, "Parar", stopPendingIntent)
            .build()
        
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Copiar & Traduzir",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tradução automática de textos copiados"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (isProcessing) return@OnPrimaryClipChangedListener
        
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrBlank() && text != lastClipboardText && text.length > 1) {
                lastClipboardText = text
                translateAndNotify(text)
            }
        }
    }

    private fun translateAndNotify(text: String) {
        isProcessing = true
        
        val source = preferencesManager.getSourceLanguage()
        val target = preferencesManager.getTargetLanguage()
        
        // Show "translating" notification first
        showTranslatingNotification(text, source, target)
        
        scope.launch {
            try {
                val result = translationService.translate(text, source, target)
                result.fold(
                    onSuccess = { translated ->
                        showTranslationNotification(text, translated, source, target)
                    },
                    onFailure = {
                        showErrorNotification(text)
                    }
                )
            } catch (e: Exception) {
                showErrorNotification(text)
            }
            isProcessing = false
        }
    }

    private fun showTranslatingNotification(text: String, source: String, target: String) {
        val collapsedView = RemoteViews(packageName, R.layout.notification_translation_collapsed).apply {
            setTextViewText(R.id.txtTitle, "🔤 Traduzindo...")
            setTextViewText(R.id.txtSource, text.take(50) + if (text.length > 50) "..." else "")
            setTextViewText(R.id.txtLanguages, "${source.uppercase()} → ${target.uppercase()}")
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(collapsedView)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showTranslationNotification(sourceText: String, translatedText: String, source: String, target: String) {
        val collapsedView = RemoteViews(packageName, R.layout.notification_translation_collapsed).apply {
            setTextViewText(R.id.txtTitle, "✅ Tradução Pronta")
            setTextViewText(R.id.txtSource, sourceText.take(50) + if (sourceText.length > 50) "..." else "")
            setTextViewText(R.id.txtLanguages, "${source.uppercase()} → ${target.uppercase()}")
        }
        
        val expandedView = RemoteViews(packageName, R.layout.notification_translation_expanded).apply {
            setTextViewText(R.id.txtTitle, "🔤 ${source.uppercase()} → ${target.uppercase()}")
            setTextViewText(R.id.txtSourceLabel, "Original:")
            setTextViewText(R.id.txtSource, sourceText)
            setTextViewText(R.id.txtResultLabel, "Tradução:")
            setTextViewText(R.id.txtResult, translatedText)
            
            // Set click intents
            val copyIntent = Intent(this@ClipboardTranslationService, ClipboardTranslationService::class.java).apply {
                action = ACTION_COPY_RESULT
                putExtra("translated_text", translatedText)
            }
            val copyPendingIntent = PendingIntent.getService(
                this@ClipboardTranslationService, 0, copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setOnClickPendingIntent(R.id.btnCopy, copyPendingIntent)
            
            val swapIntent = Intent(this@ClipboardTranslationService, ClipboardTranslationService::class.java).apply {
                action = ACTION_SWAP
            }
            val swapPendingIntent = PendingIntent.getService(
                this@ClipboardTranslationService, 1, swapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setOnClickPendingIntent(R.id.btnSwap, swapPendingIntent)
            
            val openIntent = Intent(this@ClipboardTranslationService, MainActivity::class.java)
            val openPendingIntent = PendingIntent.getActivity(
                this@ClipboardTranslationService, 2, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setOnClickPendingIntent(R.id.btnOpen, openPendingIntent)
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(text: String) {
        val view = RemoteViews(packageName, R.layout.notification_translation_collapsed).apply {
            setTextViewText(R.id.txtTitle, "⚠️ Tradução indisponível")
            setTextViewText(R.id.txtSource, "Baixe os idiomas na Biblioteca")
            setTextViewText(R.id.txtLanguages, "")
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_COPY_RESULT -> {
                val translatedText = intent.getStringExtra("translated_text") ?: return START_STICKY
                copyToClipboard(translatedText)
            }
            ACTION_SWAP -> {
                swapLanguages()
                if (lastClipboardText.isNotBlank()) {
                    translateAndNotify(lastClipboardText)
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun copyToClipboard(text: String) {
        val clip = android.content.ClipData.newPlainText("Tradução Pelko", text)
        clipboardManager.setPrimaryClip(clip)
        
        // Show brief toast
        val toastNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("✓ Tradução copiada!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, toastNotification)
    }

    private fun swapLanguages() {
        val source = preferencesManager.getSourceLanguage()
        val target = preferencesManager.getTargetLanguage()
        preferencesManager.setSourceLanguage(target)
        preferencesManager.setTargetLanguage(source)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
    }
}
