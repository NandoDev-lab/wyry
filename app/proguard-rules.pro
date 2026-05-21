# FFmpegKit rules
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# Keep Compose internal classes
-keep class androidx.compose.material3.** { *; }

# General Android
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
