package com.simplecityapps.playback.mediasession

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import au.com.simplecityapps.shuttle.imageloading.ArtworkImageLoader
import au.com.simplecityapps.shuttle.imageloading.glide.GlideImageLoader
import com.simplecityapps.playback.PlaybackManager
import com.simplecityapps.playback.PlaybackWatcher
import com.simplecityapps.playback.PlaybackWatcherCallback
import com.simplecityapps.playback.androidauto.MediaIdHelper
import com.simplecityapps.playback.queue.QueueChangeCallback
import com.simplecityapps.playback.queue.QueueManager
import com.simplecityapps.playback.queue.QueueWatcher
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MediaSessionManager(
    private val context: Context,
    private val playbackManager: PlaybackManager,
    private val queueManager: QueueManager,
    private val mediaIdHelper: MediaIdHelper,
    playbackWatcher: PlaybackWatcher,
    queueWatcher: QueueWatcher
) : PlaybackWatcherCallback,
    QueueChangeCallback {

    val mediaSession: MediaSessionCompat by lazy {
        val mediaSession = MediaSessionCompat(context, "ShuttleMediaSession")
        mediaSession.setCallback(mediaSessionCallback)
        mediaSession
    }

    private var playbackStateBuilder = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                    or PlaybackStateCompat.ACTION_SEEK_TO
                    or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                    or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
        )

    private var artworkImageLoader: ArtworkImageLoader

    init {
        playbackWatcher.addCallback(this)
        queueWatcher.addCallback(this)
        artworkImageLoader = GlideImageLoader(context)
    }

    // PlaybackWatcherCallback Implementation

    override fun onPlaystateChanged(isPlaying: Boolean) {
        mediaSession.isActive = isPlaying

        if (isPlaying) {
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, playbackManager.getPosition()?.toLong() ?: 0, 1.0f)
        } else {
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, playbackManager.getPosition()?.toLong() ?: 0, 1.0f)
        }

        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }


    // QueueChangeCallback Implementation

    override fun onQueueChanged() {
        mediaSession.setQueue(queueManager.getQueue().map { queueItem -> queueItem.toQueueItem() })
    }

    override fun onQueuePositionChanged(oldPosition: Int?, newPosition: Int?) {
        queueManager.getCurrentItem()?.let { currentItem ->
            playbackStateBuilder.setActiveQueueItemId(currentItem.toQueueItem().queueId)
            mediaSession.setPlaybackState(playbackStateBuilder.build())

            val mediaMetadataCompat = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentItem.song.id.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentItem.song.albumArtistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentItem.song.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentItem.song.name)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentItem.song.duration.toLong())
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, currentItem.song.track.toLong())
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, queueManager.getSize().toLong())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)

            mediaSession.setMetadata(
                mediaMetadataCompat.build()
            )

            artworkImageLoader.loadBitmap(currentItem.song) { bitmap ->
                mediaMetadataCompat.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                mediaSession.setMetadata(
                    mediaMetadataCompat.build()
                )
            }
        }
    }

    override fun onShuffleChanged() {
        mediaSession.setShuffleMode(queueManager.getShuffleMode().toShuffleMode())
    }

    override fun onRepeatChanged() {
        mediaSession.setRepeatMode(queueManager.getRepeatMode().toRepeatMode())
    }


    // MediaSessionCompat.Callback Implementation

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            playbackManager.play()
        }

        override fun onPause() {
            playbackManager.pause()
        }

        override fun onSkipToPrevious() {
            playbackManager.skipToPrev()
        }

        override fun onSkipToNext() {
            playbackManager.skipToNext(ignoreRepeat = true)
        }

        override fun onSkipToQueueItem(id: Long) {
            val index = queueManager.getQueue().indexOfFirst { queueItem -> queueItem.toQueueItem().queueId == id }
            if (index != -1) {
                playbackManager.skipTo(index)
            }
        }

        override fun onSeekTo(pos: Long) {
            playbackManager.seekTo(pos.toInt())
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            queueManager.setRepeatMode(repeatMode.toRepeatMode())
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            queueManager.setShuffleMode(shuffleMode.toShuffleMode())
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            mediaId?.let {
                mediaIdHelper.getPlayQueue(mediaId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { playQueue ->
                            playbackManager.load(playQueue.songs, playQueue.playbackPosition) { result ->
                                result.onSuccess { playbackManager.play() }
                                result.onFailure { error -> Timber.e(error, "Failed to load playback after onPlayFromMediaId") }
                            }
                        },
                        { throwable -> Timber.e(throwable, "onPlayFromMediaId failed") })
            } ?: Timber.e("onPlayFromMediaId requested with null parentMediaId")
        }
    }
}