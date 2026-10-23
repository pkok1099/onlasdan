import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.onlasdan.comic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.onlasdan.comic"
        minSdk = 35
        targetSdk = 36
        // Single source of truth versi: root gradle.properties
        // (onlasdan.versionCode / onlasdan.versionName).
        // versionCode = MAJOR*10000 + MINOR*100 + PATCH. Jangan bump tanpa perintah.
        versionCode = (project.findProperty("onlasdan.versionCode") as? String ?: "1").toInt()
        versionName = (project.findProperty("onlasdan.versionName") as? String ?: "1.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    signingConfigs {
        // One keystore for BOTH debug + release so installed APKs can upgrade.
        // Values are loaded from `android/keystore.properties` (ignored by git).
        create("app") {
            val props = Properties()
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                propsFile.inputStream().use { props.load(it) }
            }

            val storeFilePath = (props.getProperty("storeFile") ?: System.getenv("ANDROID_KEYSTORE_FILE"))?.trim()
            val storePasswordValue = (
                props.getProperty(
                    "storePassword",
                ) ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
                )?.trim()
            val keyAliasValue = (props.getProperty("keyAlias") ?: System.getenv("ANDROID_KEY_ALIAS"))?.trim()
            val keyPasswordValue = (props.getProperty("keyPassword") ?: System.getenv("ANDROID_KEY_PASSWORD"))?.trim()

            if (!storeFilePath.isNullOrBlank()) storeFile = rootProject.file(storeFilePath)
            if (!storePasswordValue.isNullOrBlank()) storePassword = storePasswordValue
            if (!keyAliasValue.isNullOrBlank()) keyAlias = keyAliasValue
            if (!keyPasswordValue.isNullOrBlank()) keyPassword = keyPasswordValue

            enableV1Signing = false
            enableV2Signing = false
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        debug {
            // If keystore is configured, sign debug with the same key as release.
            // If not configured, Gradle falls back to the default debug keystore.
            signingConfig = signingConfigs.findByName("app")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("app")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = true
        checkAllWarnings = true
        warningsAsErrors = true
        checkReleaseBuilds = false
        checkDependencies = true

        // Strict warnings & Unused resources policy
        enable += setOf(
            "UnusedResources",
            "UnusedIds",
            "StopShip",
            "CheckResult",
            "TypographyDashes",
            "TypographyQuotes",
        )
        fatal += setOf(
            "UnusedResources",
        )

        // Ignore third-party library update suggestions and launcher icon design heuristics
        disable += setOf(
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "NewerVersionAvailable",
            "ChromeOsAbiSupport",
            "IconLauncherShape",
            "MonochromeLauncherIcon",
            "IconDuplicates",
            "IconLocation",
            "DuplicateStrings",
            "ComposableLambdaParameterNaming",
            "ComposableLambdaParameterPosition",
            "ConvertToWebp",
            "AppLinksAutoVerify",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.jsoup)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint("1.8.0")
            .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_filename" to "disabled",
                    "ktlint_standard_kdoc" to "disabled",
                    "ktlint_standard_property-naming" to "disabled",
                    "ktlint_standard_backing-property-naming" to "disabled",
                    "ktlint_standard_max-line-length" to "disabled",
                    "ktlint_compose_modifier-missing-check" to "disabled",
                    "ktlint_compose_compositionlocal-allowlist" to "disabled",
                    "ktlint_compose_parameter-naming" to "disabled",
                    "ktlint_compose_modifier-not-used-at-root" to "disabled",
                    "ktlint_compose_param-order-check" to "disabled",
                    "ktlint_compose_mutable-state-param-check" to "disabled",
                    "ktlint_compose_multiple-emitters-check" to "disabled",
                    "ktlint_compose_lambda-param-in-effect" to "disabled",
                    "ktlint_compose_modifier-reused-check" to "disabled",
                    "ktlint_compose_modifier-composed-check" to "disabled",
                ),
            )
            .customRuleSets(listOf("io.nlopez.compose.rules:ktlint:0.6.8"))
    }
    kotlinGradle {
        target("*.gradle.kts")
        targetExclude("**/build/**")
        ktlint("1.8.0")
            .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
    }
}

// Gerbang kompleksitas (detekt): hanya ruleset complexity yang aktif.
// Utang lama tercatat di baseline — hanya pelanggaran BARU yang menggagalkan build.
// Perbarui baseline hanya atas perintah eksplisit: ./gradlew detektBaseline
detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
    source.setFrom("src/main/java")
    parallel = true
    autoCorrect = false
}

// Kebijakan ukuran file (2026-10-22): 300 baris lunak (peringatan tercetak),
// 500 baris keras (build gagal). Di atas 500 hanya dengan pengecualian tercatat
// di config/quality/file-size-exceptions.txt (path|cap|alasan); file warisan
// dibekukan di cap-nya — tumbuh melewati cap sama saja pelanggaran baru.
val verifyFileLineLimits by tasks.registering {
    notCompatibleWithConfigurationCache("Memindai pohon file sumber langsung dari disk pada setiap run.")
    group = "verification"
    description = "Gerbang ukuran file: >500 baris gagal kecuali berpengecualian tercatat; 301-500 peringatan."
    doLast {
        val softLimit = 300
        val hardLimit = 500
        val exceptions = rootProject.file("config/quality/file-size-exceptions.txt").readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { line ->
                val parts = line.split("|")
                parts[0].trim() to parts[1].trim().toInt()
            }
        val failures = mutableListOf<String>()
        val softFiles = mutableListOf<String>()
        val seenExceptions = mutableSetOf<String>()
        fileTree("src/main/java") { include("**/*.kt") }.forEach { f ->
            val lineCount = f.readLines().size
            val rel = f.toRelativeString(rootProject.projectDir)
            val cap = exceptions[rel]
            when {
                cap != null -> {
                    seenExceptions += rel
                    if (lineCount > cap) {
                        failures += "$rel: $lineCount baris melewati cap pengecualian $cap — file warisan tidak boleh tumbuh."
                    }
                }
                lineCount > hardLimit ->
                    failures += "$rel: $lineCount baris melewati batas keras $hardLimit — pecah file ini, atau catat pengecualian beralasan kuat di config/quality/file-size-exceptions.txt."
                lineCount > softLimit -> softFiles += "$rel: $lineCount baris"
            }
        }
        (exceptions.keys - seenExceptions).forEach {
            logger.lifecycle("verifyFileLineLimits: entri pengecualian tidak terpakai lagi (file hilang/menyusut): $it")
        }
        if (softFiles.isNotEmpty()) {
            logger.warn("verifyFileLineLimits: ${softFiles.size} file di zona lunak ($softLimit-$hardLimit baris):")
            softFiles.forEach { logger.warn("  - $it") }
        }
        if (failures.isNotEmpty()) {
            throw GradleException("Batas ukuran file dilanggar:\n" + failures.joinToString("\n") { "  - $it" })
        }
    }
}

tasks.named("check") {
    dependsOn("detekt", verifyFileLineLimits)
}
