# Changelog

All notable changes to this project will be documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### 🐛 Fixed

#### Translation
- **Partial translation bug**: Fixed issue where full sentences translated only the first word (race condition with Dispatchers.IO)
- **Missing import**: Added `kotlinx.coroutines.launch` import in TextTranslationFragment

#### Settings
- **Settings not saving**: Fixed issue where settings changes were not saved
  - Added `isInitializing` flag to prevent listeners from firing during initialization
  - Removed duplicate `PreferencesManager` initialization
  - Added `coerceIn` for font size to prevent index out of bounds
  - Fixed theme toggle to use new Intent

### 🧹 Cleanup

- Removed `app-debug.apk` and `debug-apk.zip` from repository (~55MB)
- Removed `analysis/` folder (irrelevant Python/Jupyter scripts)
- Removed `stats_data.json` (local data)
- Removed `docs/logo.png` (redundant, logo exists in mipmap-*)
- Removed `docs/README_COMPLETO.md` (replaced with professional structure)
- Removed fake company references (PELKO)
- Added comprehensive `.gitignore` for Android projects

### 📚 Documentation

- New professional `README.md` with badges and links
- New `docs/TECHNICAL.md` - Technical documentation (OCR→NMT pipeline)
- New `docs/ARCHITECTURE.md` - System architecture
- New `docs/SETUP.md` - Development setup guide
- Updated `CONTRIBUTING.md` with clean guidelines

---

## [1.0.0] - 2024-01-15

### ✅ Added

#### Translation
- Text input translation
- Camera image translation
- Source and target language selection
- Quick language swap

#### Language Library
- Download languages for offline use
- Manage downloaded languages
- Support for multiple language pairs (en↔pt)

#### Interface
- Modern dark theme
- Tabs for text and image
- Translation area selection
- Camera preview

#### Technical
- 100% offline (no internet after downloading languages)
- NDK/JNI for C translation engine
- CameraX for image capture
- ML Kit for OCR and translation
- ViewBinding for performance

### 📱 Requirements

- Android 5.0+ (API 21)
- ~100MB storage for languages
- Camera for image translation

---

## [0.1.0] - 2024-01-10

### 🧪 Added

- Initial project with MVP structure
- Basic translation interface
- C translation engine
- Initial layouts

---

## Commit Format

This project uses conventional commits:

```
feat: new feature
fix: bug fix
docs: documentation
style: formatting
refactor: refactoring
test: tests
chore: maintenance
```

---

## Links

- [Releases](https://github.com/whesley264-oss/offline-camera-translator/releases)
- [Issues](https://github.com/whesley264-oss/offline-camera-translator/issues)
