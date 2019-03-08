package com.simplecityapps.shuttle.ui.screens.playback

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.PagerSnapHelper
import au.com.simplecityapps.shuttle.imageloading.ArtworkImageLoader
import com.simplecityapps.adapter.RecyclerAdapter
import com.simplecityapps.adapter.RecyclerListener
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.playback.queue.QueueItem
import com.simplecityapps.playback.queue.QueueManager
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.dagger.Injectable
import com.simplecityapps.shuttle.ui.common.recyclerview.SpacesItemDecoration
import com.simplecityapps.shuttle.ui.common.toHms
import kotlinx.android.synthetic.main.fragment_playback.*
import javax.inject.Inject

class PlaybackFragment :
    Fragment(),
    Injectable,
    PlaybackContract.View,
    SeekBar.OnSeekBarChangeListener {

    @Inject lateinit var presenter: PlaybackPresenter

    @Inject lateinit var imageLoader: ArtworkImageLoader

    private val adapter = RecyclerAdapter()


    // Lifecycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_playback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.bindView(this)

        playPauseButton.setOnClickListener { presenter.togglePlayback() }
        shuffleButton.setOnClickListener { presenter.toggleShuffle() }
        repeatButton.setOnClickListener { presenter.toggleRepeat() }
        skipNextButton.setOnClickListener { presenter.skipNext() }
        skipPrevButton.setOnClickListener { presenter.skipPrev() }
        seekBar.setOnSeekBarChangeListener(this)

        recyclerView.adapter = adapter
        recyclerView.setRecyclerListener(RecyclerListener())
        PagerSnapHelper().attachToRecyclerView(recyclerView)
        recyclerView.addItemDecoration(SpacesItemDecoration(8))

        playPauseButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.colorPrimary))
    }

    override fun onDestroyView() {
        presenter.unbindView()

        super.onDestroyView()
    }


    // PlaybackContract.View

    override fun setPlayState(isPlaying: Boolean) {
        when {
            isPlaying -> playPauseButton.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_pause_black_24dp))
            else -> playPauseButton.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_play_arrow_black_24dp))
        }
    }

    override fun setShuffleMode(shuffleMode: QueueManager.ShuffleMode) {
        when (shuffleMode) {
            QueueManager.ShuffleMode.Off -> shuffleButton.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_shuffle_black_24dp))
            QueueManager.ShuffleMode.On -> shuffleButton.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_shuffle_off_black_24dp))
        }
    }

    override fun setRepeatMode(repeatMode: QueueManager.RepeatMode) {

    }

    override fun setCurrentSong(song: Song?) {
        song?.let {
            titleTextView.text = song.name
            subtitleTextView.text = "${song.albumArtistName} • ${song.albumName}"
            durationTextView.text = song.duration.toHms()
        }
    }

    override fun setQueuePosition(position: Int?, total: Int) {
        position?.let { position ->
            recyclerView.smoothScrollToPosition(position)
        }
    }

    override fun setQueue(queue: List<QueueItem>, position: Int?) {
        adapter.setData(
            queue.map { ArtworkBinder(it.song, imageLoader) },
            completion = {
                position?.let { position ->
                    recyclerView?.scrollToPosition(position)
                }
            }
        )
    }

    override fun setProgress(position: Int, duration: Int) {
        currentTimeTextView.text = position.toLong().toHms()
        seekBar.progress = ((position.toFloat() / duration) * 1000).toInt()
    }


    // SeekBar.OnSeekBarChangeListener Implementation

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        presenter.seek(seekBar.progress / 1000f)
    }


    // Static

    companion object {
        fun newInstance() = PlaybackFragment()
    }
}