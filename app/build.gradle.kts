import com.google.protobuf.gradle.id

val kotlinVersion: String by rootProject.extra
val composeVersion: String by rootProject.extra
val navVersion = "2.4.2"

val a = this
plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-kapt")
    id("com.google.protobuf")
    id("kotlin-android")
}

android {
    compileSdk = 33
    buildToolsVersion = "33.0.1"

    defaultConfig {
        applicationId = "net.pfiers.osmfocus"
        minSdk = 21
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        dataBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

protobuf {
    protobuf.apply {
        protoc {
            artifact = "com.google.protobuf:protoc:21.0-rc-1"
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
//        exclude(group = "junit", module = "junit")
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "org.apache.httpcomponents")
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.hamcrest:hamcrest-core:1.1")).using(module("junit:junit:4.10"))
        }
    }
}

dependencies {

    // Core
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("com.android.support:multidex:1.0.3")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("com.google.android.material:material:1.7.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Jetpack compose
    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.compose.runtime:runtime-livedata:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.navigation:navigation-compose:$navVersion")
    implementation("androidx.compose.animation:animation:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")

    // Jetpack compose material
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.material3:material3:1.1.0-alpha03")
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.0")

    // Compat / legacy
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Androidx views
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    // Jetpack / lifecycle
    val lifecycleVersion = "2.4.1"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")

    // Standard library extensions
    val arrowVersion = "1.1.3"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")

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
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.fragment:fragment-ktx:1.5.5")

    // Room DB
    val roomVersion = "2.4.0-alpha05"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
//    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    // Protobuf
    implementation("com.google.protobuf:protobuf-lite:3.0.1")

    // Datastore
    val datastoreVersion = "1.0.0-alpha08"
    implementation("androidx.datastore:datastore:$datastoreVersion")
    implementation("androidx.datastore:datastore-rxjava3:$datastoreVersion")

    // Datetime
    implementation("org.ocpsoft.prettytime:prettytime:5.0.0.Final")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Auth
    implementation("net.openid:appauth:0.10.0")

    // Testing
//    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
//    androidTestImplementation("androidx.test.ext:junit:1.1.2")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
