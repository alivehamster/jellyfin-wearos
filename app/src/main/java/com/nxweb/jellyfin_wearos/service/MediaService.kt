package com.nxweb.jellyfin_wearos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.nxweb.jellyfin_wearos.activity.Jellyfin
import com.nxweb.jellyfin_wearos.activity.MainActivity
import org.jellyfin.sdk.model.api.BaseItemDto

class MediaService : Service() {

    private lateinit var notificationManager: NotificationManager
    private val localBinder = LocalBinder()

    private var serviceRunningInForeground = false
    lateinit var exoPlayer: ExoPlayer

    private var currentQueue: List<BaseItemDto> = emptyList()
    private var currentIndex: Int = 0

    private val notificationId = 7893569


    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = exoPlayer.currentMediaItemIndex
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentIndex = 0
                }
            }
        })

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onDestroy() {
        exoPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        stopForegroundService()
        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        stopForegroundService()
    }

    override fun onUnbind(intent: Intent?): Boolean {

        if(exoPlayer.isPlaying) {
            val notification = genNotification()
            startForeground(notificationId, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            serviceRunningInForeground = true
        }

        return true

    }

    private fun stopForegroundService() {
        if (serviceRunningInForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            serviceRunningInForeground = false
        }
    }

    fun genNotification(): Notification {
        val notification_Channel_ID = "Jellyfin_wearos_1"

        val notificationChannel = NotificationChannel(
            notification_Channel_ID,"Jellyfin WearOS", NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)

        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationCompatBuilder = NotificationCompat.Builder(applicationContext, notification_Channel_ID)

        val notificationBuilder = notificationCompatBuilder
            .setContentTitle("Jellyfin WearOS")
            .setContentText("Tap to open media controls")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSmallIcon(com.nxweb.jellyfin_wearos.R.drawable.ic_launcher_foreground)

        val ongoingActivity = OngoingActivity.Builder(applicationContext, notificationId, notificationBuilder)
            .setTouchIntent(pendingIntent)
            .setStatus(Status.Builder().addTemplate("IDK what this is").build())
            .build()

        ongoingActivity.apply(applicationContext)

        return notificationBuilder.build()
    }
    fun setQueue(songs: List<BaseItemDto>, jellyfin: Jellyfin, startIndex: Int = 0) {
        currentQueue = songs
        currentIndex = startIndex

        startService(Intent(applicationContext, MediaService::class.java))


        val mediaItems = songs.map { song ->
            val url = jellyfin.getAudioUrl(song.id)
            MediaItem.fromUri(url)
        }

        exoPlayer.setMediaItems(mediaItems, startIndex, 0)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun setShuffleQueue(songs: List<BaseItemDto>, jellyfin: Jellyfin, startIndex: Int = 0) {
        val shuffled = songs.shuffled()
        setQueue(shuffled, jellyfin, startIndex)
    }

    fun playQueue() {
        exoPlayer.play()
    }

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun previous() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun getCurrentSong(): BaseItemDto? {
        return if (currentIndex < currentQueue.size) currentQueue[currentIndex] else null
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun stop() {
        exoPlayer.stop()
        currentQueue = emptyList()
        currentIndex = 0
    }

    inner class LocalBinder : Binder() {
        fun getService(): MediaService = this@MediaService
    }

}