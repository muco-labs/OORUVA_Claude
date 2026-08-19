plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    // Applied by app/build.gradle.kts only when google-services.json exists.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
