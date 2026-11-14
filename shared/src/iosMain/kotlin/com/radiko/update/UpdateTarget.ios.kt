package com.radiko.update

/** iOS 用的是 Web App，由 Service Worker 自动更新，不弹更新提醒。 */
actual val platformUpdateTarget: UpdateTarget? = null
