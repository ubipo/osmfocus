import com.google.protobuf.gradle.*
import org.gradle.kotlin.dsl.protobuf


plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("android.extensions")
    id("com.google.protobuf")
    id("kotlin-android")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.3"

    defaultConfig {
        applicationId = "net.pfiers.osmfocus"
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 103
        versionName = "1.0.3"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        dataBinding = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    signingConfigs {
        val secretProperties = project.rootProject.extra["secretProperties"] as java.util.Properties
        val gplayStoreFile =
            (secretProperties["signing_keystore_file"] as String?)?.let { file(it) }
        val gplayStorePassword = secretProperties["signing_keystore_password"] as String?
        val gplayKeyAlias = secretProperties["signing_key_alias"] as String?
        val gplayKeyPassword = secretProperties["signing_key_password"] as String?

        if (gplayStoreFile != null && gplayStorePassword != null && gplayKeyAlias != null && gplayKeyPassword != null) {
            create("gplayRelease") {
                storeFile = gplayStoreFile
                storePassword = gplayStorePassword
                keyAlias = gplayKeyAlias
                keyPassword = gplayKeyPassword
            }
        }
    }

    buildTypes {
        named("release") {
//            isDebuggable = true
            isMinifyEnabled = true

            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
        }
    }

    // Handles distribution channel differences (e.g. Google Play billing vs external payment flow for donations)
    val distributionChannelDimension = "distributionChannel"
    flavorDimensions(distributionChannelDimension)

    productFlavors {
        create("fdroid") {
            dimension = distributionChannelDimension
            versionNameSuffix = "-fdroid"
        }

        create("gplay") {
            dimension = distributionChannelDimension
            versionNameSuffix = "-gplay"
            signingConfigs.asMap["gplayRelease"]?. let { signingConfig = it }
        }
    }
}

//sourceSets.getByName("main") {
//    java.srcDir("${protobuf.protobuf.generatedFilesBaseDir}/main/javalite")
//}


protobuf {
    protobuf.apply {
        protoc {
            artifact = "com.google.protobuf:protoc:4.0.0-rc-2"
        }
        plugins {
            id("javalite") {
                artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0"
            }
        }
        generateProtoTasks {
            all().forEach { task ->
                task.builtins {
//                remove("java")
                }
                task.plugins {
                    id("javalite")
                }
            }
        }
    }
}

configurations {
    all {
        exclude(group = "junit", module = "junit")
    }
}

dependencies {
    val implementation by configurations

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.32")
    implementation("com.android.support:multidex:1.0.3")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.annotation:annotation:1.2.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    // Uil
    implementation("com.google.guava:guava:30.0-android")

    // Spatial
    val jtsVersion = "1.18.1"
    implementation("org.locationtech.jts:jts-core:$jtsVersion")
    implementation("org.locationtech.jts:jts-io:$jtsVersion")
    implementation("org.locationtech.jts.io:jts-io-common:$jtsVersion")
    implementation("net.sf.geographiclib:GeographicLib-Java:1.51")

    // Map
    implementation("org.osmdroid:osmdroid-android:6.1.10")

    // HTTP
    val fuelVersion = "2.3.0"
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:$fuelVersion")

    // Result
    implementation("com.github.kittinunf.result:result-coroutines:3.1.0")

    // JSON
    implementation("com.beust:klaxon:5.4")

    // Navigation
    val navVersion = "2.3.4"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.fragment:fragment-ktx:1.3.2")

    // Room DB
    val roomVersion = "2.2.6"
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
//    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    // Protobuf
    implementation("com.google.protobuf:protobuf-lite:3.0.1")

    // Datastore
    val datastoreVersion = "1.0.0-alpha08"
    implementation("androidx.datastore:datastore:$datastoreVersion")
    implementation("androidx.datastore:datastore-rxjava3:$datastoreVersion")


    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
//    testImplementation("junit:junit:4.13.1")
//    androidTestImplementation("androidx.test.ext:junit:1.1.2")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
