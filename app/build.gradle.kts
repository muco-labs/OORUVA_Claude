import java.util.Properties

// Keys live in local.properties (git-ignored). Absent keys are not an error:
// the app falls back to mock data so a fresh clone still builds and runs.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String, fallback: String = "") = localProps.getProperty(name) ?: fallback

/**
 * Environment-scoped secret. Falls back to the unsuffixed key so a single-project
 * setup keeps working, then to empty so a fresh clone still builds.
 *   SUPABASE_URL_DEV / _STAGING / _PROD  ->  SUPABASE_URL  ->  ""
 */
fun envSecret(name: String, env: String) =
    localProps.getProperty(name + "_" + env) ?: localProps.getProperty(name) ?: ""

/**
 * Release signing, read from keystore.properties in the project root
 * (git-ignored, like local.properties).
 *
 *   storeFile=../release.jks
 *   storePassword=...
 *   keyAlias=ooruva
 *   keyPassword=...
 *
 * Absent on a normal clone and in CI, and that is deliberate: a keystore in CI
 * is a keystore any workflow change on the default branch can exfiltrate.
 * Without it a release build still compiles and shrinks, it just comes out
 * unsigned -- which is exactly what CI should be producing.
 */
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasSigningConfig = keystoreProps.getProperty("storeFile") != null

// google-services.json is only present once Firebase is configured. Applying the
// plugin without it fails the build, so it is applied conditionally.
val hasGoogleServices = rootProject.file("app/google-services.json").exists()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

if (rootProject.file("app/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.ooruva.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ooruva.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Environment-specific values are set per build type below.
        buildConfigField("Boolean", "HAS_FIREBASE", hasGoogleServices.toString())
    }

    // One codebase, two shipped apps. Separate applicationIds mean a phone can
    // hold both, and each opens straight into its own dashboard.
    // src/customer/kotlin and src/vendor/kotlin are picked up by AGP convention.
    // Each flavor therefore compiles only its own screens and navigation: the
    // customer binary contains no vendor code, and vice versa. Separation is
    // enforced at compile time, not by runtime conditionals (spec 4).
    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    flavorDimensions += "audience"
    productFlavors {
        create("customer") {
            dimension = "audience"
            applicationId = "com.ooruva.app.customer"
            resValue("string", "app_name", "OORUVA")
            buildConfigField("String", "AUDIENCE", "\"CUSTOMER\"")
        }
        create("vendor") {
            dimension = "audience"
            applicationId = "com.ooruva.app.vendor"
            resValue("string", "app_name", "OORUVA Vendor")
            buildConfigField("String", "AUDIENCE", "\"VENDOR\"")
        }
    }

    // Development, staging and production point at different Supabase projects.
    // A debug build can never reach production data by accident (spec: do not
    // connect development apps directly to production).
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            isDebuggable = true
            buildConfigField("String", "ENVIRONMENT", "\"DEVELOPMENT\"")
            buildConfigField("String", "SUPABASE_URL", "\"" + envSecret("SUPABASE_URL", "DEV") + "\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"" + envSecret("SUPABASE_ANON_KEY", "DEV") + "\"")
            manifestPlaceholders["MAPS_API_KEY"] = envSecret("MAPS_API_KEY", "DEV")
        }

        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            matchingFallbacks += listOf("debug")
            buildConfigField("String", "ENVIRONMENT", "\"STAGING\"")
            buildConfigField("String", "SUPABASE_URL", "\"" + envSecret("SUPABASE_URL", "STAGING") + "\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"" + envSecret("SUPABASE_ANON_KEY", "STAGING") + "\"")
            manifestPlaceholders["MAPS_API_KEY"] = envSecret("MAPS_API_KEY", "STAGING")
        }

        release {
            // Shrinking is not only about size. An unminified APK hands anyone
            // who unzips it the full class and method names of the auth and
            // reward paths.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Null leaves the build unsigned rather than falling back to the
            // debug key. A release APK signed with the debug key is worse than
            // an unsigned one: it looks shippable and cannot be updated later.
            signingConfig = if (hasSigningConfig) signingConfigs.getByName("release") else null
            buildConfigField("String", "ENVIRONMENT", "\"PRODUCTION\"")
            buildConfigField("String", "SUPABASE_URL", "\"" + envSecret("SUPABASE_URL", "PROD") + "\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"" + envSecret("SUPABASE_ANON_KEY", "PROD") + "\"")
            manifestPlaceholders["MAPS_API_KEY"] = envSecret("MAPS_API_KEY", "PROD")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // AGP 9 turns resValue off unless asked; the flavors set app_name with it.
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    // Pinned explicitly. Something in the Play Services / Firebase graph drags
    // in a pre-1.3 fragment, and registerForActivityResult is silently broken
    // on those versions -- lintVital fails the release build over it, correctly.
    implementation("androidx.fragment:fragment:1.8.6")
    implementation("androidx.core:core-splashscreen:1.2.0")

    // Jetpack Compose (BOM keeps every compose artifact on one aligned version)
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Serialization (data models are @Serializable, ready for a backend)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    // Firebase — identity only. Resolves without google-services.json; the
    // plugin (and therefore real initialisation) activates once that file lands.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    // Backend — wired but inert until SUPABASE_URL / SUPABASE_ANON_KEY are supplied
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")

    // Maps — placeholder screen swaps to GoogleMap once a key is in the manifest
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:6.4.1")

    // Lifecycle / viewmodel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
