# Setup Guide

## Development Environment

### Prerequisites

| Requirement | Version | Notes |
|-------------|--------|-------|
| Java JDK | 17+ | Required for Android development |
| Android SDK | API 21+ | Minimum SDK 21 (Android 5.0) |
| Gradle | 8.4 | Included via wrapper |
| Android Studio | 2024+ | Recommended IDE |
| NDK | 25.1+ | For native C/C++ code |

### Installing Java JDK

```bash
# macOS (using Homebrew)
brew install openjdk@17

# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# Verify installation
java -version
```

### Installing Android SDK

1. Download [Android Command Line Tools](https://developer.android.com/studio#command-line-tools-only)
2. Extract to your preferred location (e.g., `~/Android`)
3. Set environment variables:

```bash
# Add to ~/.bashrc or ~/.zshrc
export ANDROID_HOME=~/Android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

4. Install required SDK components:

```bash
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;25.1.8937393"
```

### Installing NDK

The NDK is required for the native C translation engine:

```bash
# Using sdkmanager
sdkmanager "ndk;25.1.8937393"

# Or download from Android Developer site
# https://developer.android.com/ndk/downloads
```

---

## Project Setup

### 1. Clone Repository

```bash
git clone https://github.com/whesley264-oss/offline-camera-translator.git
cd offline-camera-translator
```

### 2. Open in Android Studio

1. Open Android Studio
2. Select **File > Open**
3. Navigate to the project folder
4. Click **OK**

Android Studio will automatically sync Gradle files.

### 3. Configure SDK (if needed)

1. Go to **File > Project Structure**
2. Under **SDK Location**, verify Android SDK path
3. Click **Apply** and **OK**

---

## Build Commands

### Debug Build

```bash
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/`

### Release Build

```bash
./gradlew assembleRelease
```

Requires signing configuration in `app/build.gradle.kts`.

### Clean Build

```bash
./gradlew clean assembleDebug
```

### Build with Dependencies

```bash
./gradlew dependencies
```

---

## Running the App

### Via Android Studio

1. Connect an Android device or start an emulator
2. Click the **Run** button (▶️) or press `Shift + F10`
3. Select your device
4. The app will install and launch

### Via Command Line

```bash
# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# (Optional) Grant permissions
adb shell pm grant com.offline.translator android.permission.CAMERA
adb shell pm grant com.offline.translator android.permission.POST_NOTIFICATIONS
```

---

## Project Structure

```
offline-camera-translator/
├── app/
│   └── src/main/
│       ├── java/com/offline/translator/
│       │   ├── model/          # Business logic
│       │   │   ├── Language.kt
│       │   │   ├── TranslationService.kt
│       │   │   ├── TextRecognitionService.kt
│       │   │   ├── PreferencesManager.kt
│       │   │   └── ...
│       │   ├── view/           # UI layer
│       │   │   ├── MainActivity.kt
│       │   │   ├── TextTranslationFragment.kt
│       │   │   ├── ImageTranslationFragment.kt
│       │   │   └── ...
│       │   ├── service/        # Background services
│       │   └── widget/         # App widgets
│       ├── res/                 # Resources
│       │   ├── layout/         # XML layouts
│       │   ├── drawable/        # Graphics
│       │   ├── values/          # Strings, colors, themes
│       │   └── ...
│       └── cpp/                 # Native C code
│           ├── translation_engine.c
│           └── CMakeLists.txt
├── docs/                        # Documentation
├── analysis/                    # Analysis scripts
├── gradle/                      # Gradle wrapper
└── .github/workflows/          # CI/CD
```

---

## Troubleshooting

### Gradle Sync Fails

```bash
# Clear Gradle cache
./gradlew clean

# Delete .gradle folder
rm -rf .gradle

# Resync project
./gradlew --refresh-dependencies
```

### NDK Not Found

Ensure NDK is installed:
```bash
sdkmanager "ndk;25.1.8937393"
```

Set NDK path in `local.properties`:
```bash
echo "ndk.dir=$ANDROID_HOME/ndk/25.1.8937393" >> local.properties
```

### Camera Permission Denied

```bash
# Grant permission via ADB
adb shell pm grant com.offline.translator android.permission.CAMERA
```

---

## CI/CD

The project uses GitHub Actions for continuous integration:

- **Build**: Automatically builds on push/PR
- **Test**: Runs basic smoke tests
- **Release**: Creates debug APK artifacts

See `.github/workflows/android_build.yml` for configuration.
