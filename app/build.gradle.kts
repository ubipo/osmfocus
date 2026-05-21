import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.google.protobuf.gradle.*
import java.io.FileNotFoundException
import java.io.FileInputStream
import java.util.Properties

data class AndroidVersion(val name: String, val code: Int) {
    companion object
}

fun AndroidVersion.Companion.fromGit(rootDir: File) = ProcessBuilder(
    "bash",
    rootDir.resolve("scripts/vcs_version.sh").absolutePath
)
    .directory(rootDir)
    .redirectErrorStream(true)
    .start()
    .run {
        val stdout = inputReader().use { it.readText() }.trim()
        check(waitFor() == 0) { stdout.ifBlank { "<empty stdout>" } }
        stdout
    }
    .lineSequence()
    .filter(String::isNotBlank)
    .map { line -> line.split('=', limit = 2) }
    .associate { (key, value) -> key to value }
    .run {
        AndroidVersion(
            name = getValue("semver"),
            code = getValue("android_code").toInt(),
        )
    }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.benmanes.versions)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

android {
    compileSdk = 36
    val androidVersion = try {
        AndroidVersion.fromGit(rootDir)
    } catch (e: Exception) {
        logger.warn("Failed to read VCS version (${e.message}); using fallback version 0.0.0 (1).")
        AndroidVersion("0.0.0", 1)
    }

    defaultConfig {
        applicationId = "net.pfiers.osmfocus"
        minSdk = 23
        targetSdk = 36
        versionName = androidVersion.name
        versionCode = androidVersion.code

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["appAuthRedirectScheme"] = "net.pfiers.osmfocus"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }


    signingConfigs {
        val getSigningValue: (String) -> String? = try {
            val props = Properties()
            FileInputStream(rootProject.file("secrets.properties")).use { inputStream ->
                props.load(inputStream)
            };
            { key -> props.getProperty(key) }
        } catch (_: FileNotFoundException) {
            { key -> System.getenv(key.uppercase()) }
        }

        run {
            val keystorePath = getSigningValue("signing_keystore_file") ?: return@run
            val storePassword = getSigningValue("signing_keystore_password") ?: return@run
            val keyAlias = getSigningValue("signing_key_alias") ?: return@run
            val keyPassword = getSigningValue("signing_key_password") ?: return@run

            create("release") {
                storeFile = file(keystorePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }
    }

    // Handles distribution channel differences (e.g. Google Play billing vs external payment flow for donations)
    val distributionChannelDimension = "distributionChannel"
    flavorDimensions += distributionChannelDimension

    productFlavors {
        create("fdroid") {
            dimension = distributionChannelDimension
            versionNameSuffix = "-fdroid"
            signingConfigs.asMap["release"]?.let { signingConfig = it }
        }

        create("gplay") {
            dimension = distributionChannelDimension
            versionNameSuffix = "-gplay"
            signingConfigs.asMap["release"]?.let { signingConfig = it }
        }
    }

    packaging {
        jniLibs {
            // Workaround for third-party native libs that are not 16 KB ELF-aligned.
            useLegacyPackaging = true
        }
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/license.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/notice.txt",
                    "META-INF/ASL2.0",
                    "META-INF/*.kotlin_module",
                    "org/apache/http/version.properties",
                    "org/apache/http/client/version.properties"
                )
            )
        }
    }
    namespace = "net.pfiers.osmfocus"
}

@Suppress("UnstableApiUsage")
androidComponents {
    beforeVariants(selector().withBuildType("release").withFlavor("distributionChannel", "fdroid")) {
        it.isMinifyEnabled = false
        it.shrinkResources = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/room-schemas")
}

protobuf {
    protoc {
        artifact = libs.google.protobuf.protoc.get().toString()
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
        }
    }
}

configurations {
    all {
        exclude(group = "junit", module = "junit")
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "org.apache.httpcomponents")
    }
}

dependencies {
    val implementation by configurations

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.material)
    implementation(libs.androidx.annotation)
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)

    // Spatial
    implementation(libs.locationtech.jts.core)
    implementation(libs.locationtech.jts.io)
    implementation(libs.locationtech.jts.io.common)
    implementation(libs.geographiclib)

    // Map
    implementation(libs.maplibre.compose)

    // HTTP
    implementation(libs.kittinunf.fuel)
    implementation(libs.kittinunf.fuel.coroutines)

    // Result
    implementation(libs.kittinunf.result.coroutines)

    // JSON
    implementation(libs.beust.klaxon)
    implementation(libs.kotlinx.serialization.json)

    // Navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)


    // Room DB
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Protobuf
    implementation(libs.google.protobuf.javalite)

    // Datastore
    implementation(libs.androidx.datastore)

    // Datetime
    implementation(libs.ocpsoft.prettytime)

    // Logging
    implementation(libs.jakewharton.timber)

    // Auth
    implementation(libs.openid.appauth)

    // Testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
