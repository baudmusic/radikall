package com.radiko.player

import android.R
import android.content.Intent
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 只负责前台通知和系统媒体控件。MediaSession 和 ExoPlayer 都归 [AndroidExoRadioPlayer] 所有，
 * 这里只挂上/摘下，**绝不 release**。
 *
 * 暂停或播放出错后服务会退出前台，App 在后台待一会儿系统就会把它回收（onDestroy）。
 * 以前 onDestroy 顺手把会话 release 了，播放器手里却还拿着这个已释放的会话；
 * 回到 App 再按播放时，它被交给新起的服务，addSession 直接抛异常，App 闪退。
 */
class AndroidAudioService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_media_play)
            },
        )
        pendingSession?.let(::attachSession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    fun attachSession(session: MediaSession) {
        if (mediaSession === session) {
            pendingSession = null
            return
        }
        mediaSession?.let(::removeSession)
        mediaSession = session
        addSession(session)
        pendingSession = null
    }

    fun detachSession(session: MediaSession?) {
        if (mediaSession === session) {
            session?.let(::removeSession)
            mediaSession = null
        }
        if (pendingSession === session) {
            pendingSession = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.pause()
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.let(::removeSession)
        mediaSession = null
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: AndroidAudioService? = null

        @Volatile
        private var pendingSession: MediaSession? = null

        fun registerSession(session: MediaSession) {
            pendingSession = session
            instance?.attachSession(session)
        }

        fun unregisterSession(session: MediaSession?) {
            instance?.detachSession(session)
            if (pendingSession === session) {
                pendingSession = null
            }
        }
    }
}
