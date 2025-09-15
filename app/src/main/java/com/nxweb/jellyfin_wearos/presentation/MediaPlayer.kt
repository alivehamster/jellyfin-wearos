package com.nxweb.jellyfin_wearos.presentation

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import io.ktor.utils.io.ByteReadChannel
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

class MediaPlayer(context: Context) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private var currentQueue: List<BaseItemDto> = emptyList()
    private var currentIndex: Int = 0

    init {
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
    }

    fun playFromUrl(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun setQueue(songs: List<BaseItemDto>, jellyfin: Jellyfin, startIndex: Int = 0) {
        currentQueue = songs
        currentIndex = startIndex

        val mediaItems = songs.map { song ->
            val url = jellyfin.getAudioUrl(song.id)
            MediaItem.fromUri(url)
        }

        exoPlayer.setMediaItems(mediaItems, startIndex, 0)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.prepare()
        exoPlayer.play()
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

    fun release() {
        exoPlayer.release()
    }
}