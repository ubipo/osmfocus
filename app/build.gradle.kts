//import com.google.protobuf.gradle.builtins
//import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
//import com.google.protobuf.gradle.plugins
//import com.google.protobuf.gradle.protoc

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-kapt")
    id("com.google.protobuf")
    id("kotlin-android")
}

android {
    compileSdk = 34
//    buildToolsVersion = "30.0.3"

    defaultConfig {
        applicationId = "net.pfiers.osmfocus"
        minSdk = 21
        targetSdk = 34
        versionCode = 150
        versionName = "1.5.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/room-schemas"
            }
        }

        manifestPlaceholders["appAuthRedirectScheme"] = "net.pfiers.osmfocus"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        dataBinding = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
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

            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isOptimizeCode = true
                isObfuscate = false
                proguardFiles("proguard-rules.pro")
            }
        }
    }

    // Handles distribution channel differences (e.g. Google Play billing vs external payment flow for donations)
    val distributionChannelDimension = "distributionChannel"
    flavorDimensionList.add(distributionChannelDimension)

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

    packagingOptions {
        resources.excludes.addAll(listOf(
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
        ))
    }
    namespace = "net.pfiers.osmfocus"
}

//sourceSets.getByName("main") {
//    java.srcDir("${protobuf.protobuf.generatedFilesBaseDir}/main/javalite")
//}


protobuf {
    protobuf.apply {
        protoc {
            artifact = "com.google.protobuf:protoc:3.24.2"
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
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "org.apache.httpcomponents")
    }
}

dependencies {
    val implementation by configurations

    val kotlinVersion = rootProject.extra["kotlin"] as String?
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("com.android.support:multidex:1.0.3")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.annotation:annotation:1.6.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    // Spatial
    val jtsVersion = "1.18.1"
    implementation("org.locationtech.jts:jts-core:$jtsVersion")
    implementation("org.locationtech.jts:jts-io:$jtsVersion")
    implementation("org.locationtech.jts.io:jts-io-common:$jtsVersion")
    implementation("net.sf.geographiclib:GeographicLib-Java:1.51")

    // Map
    implementation("org.osmdroid:osmdroid-android:6.1.10")

    // HTTP
    val fuelVersion = "2.3.1"
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:$fuelVersion")

    // Result
    implementation("com.github.kittinunf.result:result-coroutines:4.0.0")

    // JSON
    implementation("com.beust:klaxon:5.4")

    // Navigation
    val navVersion = "2.7.1"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.fragment:fragment-ktx:1.6.1")

    // Room DB
    val roomVersion = "2.6.0-beta01"
    implementation("androidx.room:room-runtime:$roomVersion")

    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
//    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    // Protobuf
    implementation("com.google.protobuf:protobuf-lite:3.0.1")

    // Datastore
    val datastoreVersion = "1.1.0-alpha04"
    implementation("androidx.datastore:datastore:$datastoreVersion")
    implementation("androidx.datastore:datastore-rxjava3:$datastoreVersion")

    // Datetime
    implementation("org.ocpsoft.prettytime:prettytime:5.0.0.Final")

    // Logging
    implementation("com.jakewharton.timber:timber:4.7.1")

    // Auth
    implementation("net.openid:appauth:0.10.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
//    testImplementation("junit:junit:4.13.1")
//    androidTestImplementation("androidx.test.ext:junit:1.1.2")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
