# Offline Camera Translator 📱

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Android](https://img.shields.io/badge/Android-5.0%2B-brightgreen)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Build](https://img.shields.io/github/actions/workflow/status/whesley264-oss/offline-camera-translator/android_build.yml)
[![Release](https://img.shields.io/github/v/release/whesley264-oss/offline-camera-translator?include_prereleases)](https://github.com/whesley264-oss/offline-camera-translator/releases/latest)

**100% Offline Camera Translator for Android with Neural Machine Translation**

*Translate text from images using your camera or type directly — no internet required after downloading language packs.*

[🇧🇷 Português](./docs/README_pt-BR.md) | [🇺🇸 English](./docs/README_en-US.md)

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

---

## 📥 Download

| Version | Type | Download |
|---------|------|----------|
| **v1.0.0** | Stable | [Download APK](https://github.com/whesley264-oss/offline-camera-translator/releases/latest) |
| Beta | Testing | [Download Beta](https://github.com/whesley264-oss/offline-camera-translator/releases) |

### Installation

1. Download APK from [latest release](https://github.com/whesley264-oss/offline-camera-translator/releases/latest)
2. On your phone, go to **Settings > Security**
3. Enable **"Unknown sources"**
4. Open the downloaded `.apk` file
5. Tap **Install**

---

## 🛠️ Tech Stack

| Technology | Usage |
|------------|-------|
| **Kotlin** | Primary language |
| **CameraX** | Camera capture |
| **ML Kit Translation** | Offline NMT translation |
| **ML Kit Text Recognition** | OCR |
| **NDK/JNI** | Native C engine |
| **Material Design 3** | UI framework |

---

## 🔧 Build from Source

```bash
# Clone repository
git clone https://github.com/whesley264-oss/offline-camera-translator.git
cd offline-camera-translator

# Build debug APK
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/
```

### Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|--------------|
| Android | 5.0 (API 21) | 10.0+ (API 29) |
| RAM | 2 GB | 4 GB+ |
| Storage | 100 MB | 500 MB+ |

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [📖 Technical Documentation](./docs/TECHNICAL.md) | How the app works, architecture, OCR & NMT details |
| [🏗️ Architecture](./docs/ARCHITECTURE.md) | System design and component interaction |
| [🚀 Setup Guide](./docs/SETUP.md) | Development environment setup |
| [📋 Contributing](./CONTRIBUTING.md) | How to contribute |
| [📝 Changelog](./CHANGELOG.md) | Release history |

---

## 📁 Project Structure

```
offline-camera-translator/
├── app/
│   └── src/main/
│       ├── java/com/offline/translator/
│       │   ├── model/           # Business logic
│       │   ├── view/            # UI (Activities/Fragments)
│       │   ├── service/         # Background services
│       │   └── widget/          # App widgets
│       ├── res/                 # Layouts, drawables, values
│       └── cpp/                 # Native C translation engine
├── docs/                        # Technical documentation
├── analysis/                    # Data analysis scripts
└── .github/workflows/          # CI/CD pipelines
```

---

## ❓ FAQ

**Q: Does it work offline?**
A: Yes! After downloading language packs, no internet is required.

**Q: How much storage?**
A: ~30-50MB per language pair.

**Q: How many languages?**
A: 50+ languages supported.

---

## 🤝 Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

---

## 📜 License

MIT License - see [LICENSE](./LICENSE)

---

<p align="center">
  Made with ❤️ in Brazil 🇧🇷
</p>
