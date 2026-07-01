# ProGuard rules for FindMyChild

# Keep Moshi adapters
-keep class com.findmychild.app.data.remote.dto.** { *; }
-keep class com.findmychild.app.domain.model.** { *; }

# Keep WebSocket model classes
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
