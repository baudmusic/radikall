plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(24)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(23)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation(libs.coil.compose)
    implementation("net.java.dev.jna:jna-jpms:5.14.0")
    implementation("net.java.dev.jna:jna-platform-jpms:5.14.0")
}

compose.desktop {
    application {
        mainClass = "com.radiko.desktop.MainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
            )
            packageName = "Radikall"
            packageVersion = "1.0.1"
            windows {
                iconFile.set(project.file("src/main/resources/logo2.ico"))
                shortcut = true
                menuGroup = "Radikall"
}
        }
    }
}
