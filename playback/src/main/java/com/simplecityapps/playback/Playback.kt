package com.simplecityapps.playback

import com.simplecityapps.mediaprovider.model.Song

interface Playback {

    var callback: Callback?

    fun load(completion: (Result<Boolean>) -> Unit)

    fun loadNext()

    fun play()

    fun pause()

    fun isPlaying(): Boolean

    /**
     * @param position the position to seek to, in milliseconds
     */
    fun seek(position: Int)

    /**
     * @return the current seek position, in milliseconds
     */
    fun getPosition(): Int?

    /**
     * @return the track duration, in milliseconds
     */
    fun getDuration(): Int?

    fun setVolume(volume: Float)

    interface Callback {

        fun onPlaystateChanged(isPlaying: Boolean)

        fun onPlaybackComplete(song: Song)
    }

}