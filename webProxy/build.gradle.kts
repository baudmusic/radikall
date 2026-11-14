plugins {
    application
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

// 不锁定 JDK 版本（以前的 jvmToolchain(17) 要求本机装着 JDK 17，换了 JDK 21 的 Android Studio 后同步直接失败），
// 和其他模块一样只把编译目标定在 Java 17：用 JDK 17、21、24 都能构建。
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

application {
    mainClass = "com.radiko.webproxy.MainKt"
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.status.pages)
    implementation("ch.qos.logback:logback-classic:1.5.16")
}
