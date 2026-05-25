# TelePort ProGuard Rules

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }

# Ktor (server + client)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.teleport.app.**$$serializer { *; }
-keepclassmembers class com.teleport.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.teleport.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp (used by Ktor client and ScreenCastService)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ZXing QR Code
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep all app components referenced in the manifest
-keep class com.teleport.app.MainActivity { *; }
-keep class com.teleport.app.TelePortApp { *; }
-keep class com.teleport.app.tv.player.NativePlayerActivity { *; }
-keep class com.teleport.app.tv.server.LocalServerService { *; }
-keep class com.teleport.app.tv.server.BootReceiver { *; }
-keep class com.teleport.app.mobile.mirror.ScreenCastService { *; }
