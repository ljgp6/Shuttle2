package com.simplecityapps.shuttle.ui.screens.library.playlists.detail

import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.shuttle.ui.common.mvp.BaseContract

interface PlaylistDetailContract {

    interface View {
        fun setData(songs: List<Song>)
        fun showLoadError(error: Error)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun loadData()
        fun onSongClicked(song: Song)
        fun shuffle()
    }
}