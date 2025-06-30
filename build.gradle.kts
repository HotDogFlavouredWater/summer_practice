import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.compose") version "1.6.10"
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}

// Настраиваем Gradle Java-toolchain на JDK 21:
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Настраиваем Kotlin JVM-toolchain на JDK 21:
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Указываем Kotlin-компилятору целевую JVM 21:
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}

compose.desktop {
    application {
        mainClass = "org.example.MainKt"

        buildTypes {
            release {
                proguard {
                    isEnabled.set(false)
                }
            }
        }
    }
}
