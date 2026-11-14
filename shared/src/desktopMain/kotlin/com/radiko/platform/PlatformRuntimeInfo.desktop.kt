package com.radiko.platform

actual object PlatformRuntimeInfo {
    /**
     * 打包后的启动器（jpackage）会带上 -Djpackage.app-version，值就是 build.gradle.kts 里的
     * packageVersion，这样改版本号只用改一处。直接从 IDE / gradle run 启动时没有它，用后备值。
     */
    actual val versionName: String =
        System.getProperty("jpackage.app-version")?.takeIf { it.isNotBlank() } ?: "1.0.1"

    actual fun clearCaches(): Boolean = false
}
