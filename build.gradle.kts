val kotlinVersion by project.extra("1.4.21")

buildscript {
    extra.apply {
        set("kotlin", "1.4.21")
    }

    repositories {
        google()
        jcenter()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx/")
        maven("http://storage.googleapis.com/r8-releases/raw")
    }

    dependencies {
        classpath("com.android.tools:r8:2.1.75")
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath(kotlin("gradle-plugin", version = project.extra["kotlin"] as String?))
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.14")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}
