# Architecture

## Overview

The Offline Camera Translator follows a **Clean Architecture** pattern adapted for Android:

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐│
│  │ MainActivity │  │  Settings    │  │  LanguageLibrary    ││
│  │             │  │  Activity    │  │  Activity           ││
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘│
│         │                │                     │            │
│  ┌──────┴────────────────┴─────────────────────┴──────────┐│
│  │              Fragments (ViewPager)                       ││
│  │   TextTranslationFragment   │   ImageTranslationFragment ││
│  └──────────────────────────────┴───────────────────────────┘│
└────────────────────────────┬──────────────────────────────────┘
                             │
┌────────────────────────────┴──────────────────────────────────┐
│                      DOMAIN                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │TranslationService│  │TextRecognition│  │LanguageDownload │  │
│  │              │  │  Service     │  │  Manager           │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                    Models                               │  │
│  │  Language │ TranslationStats │ HistoryManager         │  │
│  └─────────────────────────────────────────────────────────┘  │
└────────────────────────────┬──────────────────────────────────┘
                             │
┌────────────────────────────┴──────────────────────────────────┐
│                      DATA                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │SharedPrefs  │  │   ML Kit    │  │   Native Engine     │  │
│  │(Preferences)│  │(Translation)│  │   (NDK/JNI)         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
```

---

## Components

### Presentation Layer

| Component | Responsibility |
|-----------|---------------|
| `MainActivity` | Main container with ViewPager |
| `TextTranslationFragment` | Text-to-text translation UI |
| `ImageTranslationFragment` | Camera/image translation UI |
| `SettingsActivity` | App settings and preferences |
| `HistoryActivity` | Translation history view |
| `LanguageLibraryActivity` | Language pack management |

### Domain Layer

| Component | Responsibility |
|-----------|---------------|
| `TranslationService` | Core translation logic |
| `TextRecognitionService` | OCR processing |
| `LanguageDownloadManager` | Language pack downloads |
| `PreferencesManager` | User preferences |
| `HistoryManager` | Translation history |

### Data Layer

| Component | Responsibility |
|-----------|---------------|
| `ML Kit Translation` | Neural Machine Translation |
| `ML Kit Text Recognition` | OCR |
| `SharedPreferences` | Local storage |
| `NativeTranslationEngine` | JNI bridge to C code |

---

## Data Flow

### Translation Flow (Text)

```
User Input → TextTranslationFragment
              ↓
        TranslationService.translate()
              ↓
         ML Kit (NMT)
              ↓
        TranslationResult
              ↓
        Update UI → Save to History
```

### Translation Flow (Camera)

```
Camera Capture → ImageTranslationFragment
       ↓
  TextRecognitionService.recognizeText()
       ↓
  ML Kit (OCR)
       ↓
  Extracted Text
       ↓
  TranslationService.translate()
       ↓
  ML Kit (NMT)
       ↓
  Translated Text Overlay
```

---

## State Management

The app uses **ViewBinding** for UI updates and **Coroutines** for async operations:

```kotlin
// ViewModel-like pattern in Fragments
private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

scope.launch {
    val result = translationService.translate(text, source, target)
    result.fold(
        onSuccess = { updateUI(it) },
        onFailure = { showError(it) }
    )
}
```

---

## Service Architecture

### Clipboard Translation Service

A foreground service that monitors clipboard changes:

```
Clipboard Change → ClipboardTranslationService
                          ↓
                    Check if enabled
                          ↓
                    Translate text
                          ↓
                    Show notification
```

---

## Database Schema

### SharedPreferences Keys

| Key | Type | Description |
|-----|------|-------------|
| `dark_mode` | Boolean | Dark theme enabled |
| `font_size` | Int | Font size (0-3) |
| `font_family` | String | Font family name |
| `source_lang` | String | Default source language |
| `target_lang` | String | Default target language |
| `clipboard_translation` | Boolean | Clipboard monitoring |
| `notification_sound` | Boolean | Sound enabled |
| `notification_vibrate` | Boolean | Vibration enabled |

### History Storage

Stored in app's private directory as JSON:
- Original text
- Translated text
- Source/target languages
- Timestamp

---

## Module Dependencies

```
app/
├── model/          # No dependencies
├── view/           # Depends on model/
├── service/        # Depends on model/
├── widget/         # Depends on model/
└── cpp/           # Native code
```
