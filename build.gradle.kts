buildscript {
    val kotlinVersion: String by extra("1.6.21")
    val composeVersion: String by extra("1.2.0-rc02")

    repositories {
        google()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx/")
        maven("https://storage.googleapis.com/r8-releases/raw")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.4.0")
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.2")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
//        maven("https://kotlin.bintray.com/kotlinx/")
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

val secretsPropertiesFile = rootProject.file("secrets.properties")
val secretProperties by project.extra(java.util.Properties())
if (secretsPropertiesFile.exists()) {
    secretProperties.load(java.io.FileInputStream(secretsPropertiesFile))
} else {
    System.getenv("SIGNING_KEYSTORE_FILE")
        ?.let { secretProperties.setProperty("signing_keystore_file", it) }
    System.getenv("SIGNING_KEYSTORE_PASSWORD")
        ?.let { secretProperties.setProperty("signing_keystore_password", it) }
    System.getenv("SIGNING_KEY_ALIAS")
        ?.let { secretProperties.setProperty("signing_key_alias", it) }
    System.getenv("SIGNING_KEY_PASSWORD")
        ?.let { secretProperties.setProperty("signing_key_password", it) }
}
