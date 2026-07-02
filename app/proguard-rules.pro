# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jules/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep all Fragments in this package:
-keep public class * extends androidx.fragment.app.Fragment

# Keep all view models
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep data classes (often needed for Gson)
-keep class com.example.presencedetector.data.** { *; }

# Keep specific classes used in reflection if any
-keep class com.example.presencedetector.MainActivity { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# LibVLC (might need special handling)
-keep class org.videolan.libvlc.** { *; }

# ML Kit (might need special handling)
-keep class com.google.mlkit.** { *; }
