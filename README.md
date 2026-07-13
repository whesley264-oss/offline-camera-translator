# Offline Camera Translator 📱

<div align="center">

[![Version](https://img.shields.io/badge/version-1.0.0-blue?style=flat-square)](https://github.com/whesley264-oss/offline-camera-translator/releases)
[![Android](https://img.shields.io/badge/Android-5.0%2B-brightgreen?style=flat-square)](https://developer.android.com/about)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/whesley264-oss/offline-camera-translator/android_build.yml?style=flat-square)](https://github.com/whesley264-oss/offline-camera-translator/actions)
[![Stars](https://img.shields.io/github/stars/whesley264-oss/offline-camera-translator?style=social)](https://github.com/whesley264-oss/offline-camera-translator/stargazers)
[![Forks](https://img.shields.io/github/forks/whesley264-oss/offline-camera-translator?style=social)](https://github.com/whesley264-oss/offline-camera-translator/network/members)

**100% Offline Camera Translator for Android with Neural Machine Translation**

*Translate text from images using your camera or type directly — no internet required after downloading language packs.*

</div>

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 📷 **Camera Translation** | Point camera at any text and translate instantly |
| 🖼️ **Image Upload** | Select photos from gallery to translate |
| ⌨️ **Text Input** | Type or paste text for translation |
| 🌐 **100% Offline** | Works without internet after downloading language packs |
| 🔊 **Text-to-Speech** | Listen to translations with humanized voices |
| 📚 **Language Library** | Download and manage offline language packs |
| 🌙 **Dark Mode** | Modern dark theme support |
| 📋 **Clipboard Translation** | Auto-translate copied text |

---

## 📥 Download

### Latest Release

Download the latest APK from the [Releases page](https://github.com/whesley264-oss/offline-camera-translator/releases/latest).

### Build from Source

```bash
git clone https://github.com/whesley264-oss/offline-camera-translator.git
cd offline-camera-translator
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Installation

1. Download the APK from [Releases](https://github.com/whesley264-oss/offline-camera-translator/releases/latest)
2. Enable "Install from unknown sources" in Settings
3. Open and install the APK

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Kotlin** | Primary language |
| **CameraX** | Camera capture |
| **ML Kit Translation** | Offline NMT translation |
| **ML Kit Text Recognition** | OCR |
| **NDK/JNI** | Native C engine |
| **Material Design 3** | UI framework |
| **Coroutines** | Async operations |

---

## 🔧 Build

### Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|--------------|
| Java JDK | 17 | 17+ |
| Android SDK | API 21 | API 34 |
| Android Studio | 2024.1 | Latest |

### Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Clean
./gradlew clean

# Dependencies
./gradlew dependencies
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [📖 Technical](./docs/TECHNICAL.md) | How it works, OCR & NMT pipeline |
| [🏗️ Architecture](./docs/ARCHITECTURE.md) | System design |
| [🚀 Setup](./docs/SETUP.md) | Dev environment setup |
| [📝 Changelog](./CHANGELOG.md) | Release history |

---

## 📁 Project Structure

```
offline-camera-translator/
├── app/
│   └── src/main/
│       ├── java/com/offline/translator/
│       │   ├── model/           # Business logic
│       │   ├── view/            # Activities & Fragments
│       │   ├── service/         # Background services
│       │   └── widget/          # App widgets
│       ├── res/                 # Resources
│       └── cpp/                 # Native C engine
├── docs/                        # Documentation
└── .github/workflows/          # CI/CD
```

---

## ❓ FAQ

**Q: Does it work offline?**
A: Yes! After downloading language packs, no internet is required.

**Q: How much storage space?**
A: ~30-50MB per language pair.

**Q: How many languages?**
A: 50+ languages supported.

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details.

---

## 📜 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

---

<p align="center">
  Built with ❤️ by <a href="https://github.com/whesley264">@whesley264</a>
</p>
