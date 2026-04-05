package com.radiko.player

import android.R
import android.content.Intent
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

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
        mediaSession?.release()
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
        mediaSession?.release()
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
