package com.radiko.update

/** 桌面端目前只发布 Windows 安装包；在别的系统上跑就不检查更新。 */
actual val platformUpdateTarget: UpdateTarget? =
    if (System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)) {
        UpdateTarget.WINDOWS_INSTALLER
    } else {
        null
    }
