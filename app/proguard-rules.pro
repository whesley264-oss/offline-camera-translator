# Proguard rules for Offline Translator

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep translation engine
-keep class com.offline.translator.model.NativeTranslationEngine { *; }

# Keep MVP classes
-keep class com.offline.translator.presenter.** { *; }
-keep class com.offline.translator.view.** { *; }
-keep class com.offline.translator.model.** { *; }