# --- Proteção VibeCast Studio ---

# Impede que nomes de classes de dados (importantes para salvar rádios) sejam renomeados
-keepclassmembers class com.nandohypesoft.vibecast.data.** { *; }
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# Proteção para Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# Proteção para FFmpegKit (não pode ofuscar chamadas nativas JNI)
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.moizhassan.ffmpeg.** { *; }

# Proteção para Media3/ExoPlayer
-keep class androidx.media3.** { *; }

# Remove logs de depuração automaticamente da versão final (segurança extra)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Impede a engenharia reversa de nomes de métodos sensíveis
-repackageclasses ''
-allowaccessmodification
