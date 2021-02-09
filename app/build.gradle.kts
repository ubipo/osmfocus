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
    buildToolsVersion = "30.0.0"

    defaultConfig {
        applicationId = "net.pfiers.osmfocus"
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 100
        versionName = "1.0.0"

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

    buildTypes {
        named("release") {
//            isDebuggable = true
            isMinifyEnabled = true

            setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
        }
    }
}

sourceSets.forEach { ss ->
    println(ss.name)
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
    implementation(kotlin("stdlib", "1.4.21"))
    implementation("com.android.support:multidex:1.0.3")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.annotation:annotation:1.1.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.1")

    // Uil
    implementation("com.google.guava:guava:30.0-android")

    // Spatial
    val jtsVersion = "1.17.1"
    implementation("org.locationtech.jts:jts-core:$jtsVersion")
    implementation("org.locationtech.jts:jts-io:$jtsVersion")
    implementation("org.locationtech.jts.io:jts-io-common:$jtsVersion")
    implementation("net.sf.geographiclib:GeographicLib-Java:1.50")

    // Map
    implementation("org.osmdroid:osmdroid-android:6.1.8")

    // HTTP
    val fuelVersion = "2.3.0"
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:$fuelVersion")

    // Result
    implementation("com.github.kittinunf.result:result-coroutines:3.1.0")

    // JSON
    implementation("com.beust:klaxon:5.4")

    // Navigation
    val navVersion = "2.3.2"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.fragment:fragment-ktx:1.3.0-rc02")

    // Room DB
    val roomVersion = "2.2.6"
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
//    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    // Protobuf
    implementation("com.google.protobuf:protobuf-lite:3.0.0")

    // Datastore
    val datastoreVersion = "1.0.0-alpha06"
    implementation("androidx.datastore:datastore:$datastoreVersion")
    implementation("androidx.datastore:datastore-rxjava3:$datastoreVersion")

//    testImplementation("junit:junit:4.13.1")
//    androidTestImplementation("androidx.test.ext:junit:1.1.2")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
