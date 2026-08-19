# ============================================================================
# OORUVA — R8 / ProGuard rules
#
# The app is shrunk and obfuscated for release. Most of what needs keeping is
# reflective: kotlinx.serialization generates serializers that R8 cannot see
# being used, and Ktor resolves its engine through a service loader.
# ============================================================================

# ── OORUVA data models ─────────────────────────────────────────────────────
# Every DTO is @Serializable, and its generated serializer is only ever reached
# reflectively. Losing one produces a runtime SerializationException on a screen
# that worked perfectly in debug, which is the worst way to find this out.
-keepclassmembers class com.ooruva.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.ooruva.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ooruva.app.data.remote.**$$serializer { *; }
-keep,includedescriptorclasses class com.ooruva.app.data.models.**$$serializer { *; }

# The models themselves keep their field names: they are serialised by name
# against Postgres column names, so obfuscating them breaks the wire format.
-keep class com.ooruva.app.data.models.** { *; }
-keep class com.ooruva.app.data.remote.** { *; }

# ── kotlinx.serialization ──────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Ktor ───────────────────────────────────────────────────────────────────
# The OkHttp engine is found through a service loader, so nothing in the code
# references it directly for R8 to follow.
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# ── Supabase ───────────────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ── Firebase ───────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Play Services / Maps ───────────────────────────────────────────────────
-dontwarn com.google.maps.**
-dontwarn com.google.android.gms.**

# ── Keep line numbers for readable crash reports ───────────────────────────
# Without this a production stack trace is a list of obfuscated names and is
# effectively useless. SourceFile is renamed rather than kept, so it leaks
# nothing while the mapping file stays the way back to real names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
