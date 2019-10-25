package com.simplecityapps.shuttle.ui.screens.library.albums

import com.simplecityapps.mediaprovider.MediaImporter
import com.simplecityapps.mediaprovider.model.Album
import com.simplecityapps.mediaprovider.repository.AlbumRepository
import com.simplecityapps.mediaprovider.repository.SongQuery
import com.simplecityapps.mediaprovider.repository.SongRepository
import com.simplecityapps.playback.PlaybackManager
import com.simplecityapps.shuttle.ui.common.mvp.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.text.Collator
import javax.inject.Inject

class AlbumListPresenter @Inject constructor(
    private val albumArtistRepository: AlbumRepository,
    private val songRepository: SongRepository,
    private val playbackManager: PlaybackManager,
    private val mediaImporter: MediaImporter
) : AlbumListContract.Presenter,
    BasePresenter<AlbumListContract.View>() {

    private val mediaImporterListener = object : MediaImporter.Listener {
        override fun onProgress(progress: Float, message: String) {
            view?.setLoadingProgress(progress)
        }
    }

    override fun loadAlbums() {
        addDisposable(
            albumArtistRepository.getAlbums()
                .map { album -> album.sortedWith(Comparator { a, b -> Collator.getInstance().compare(a.sortKey, b.sortKey) }) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { albums ->
                        if (albums.isEmpty()) {
                            if (mediaImporter.isScanning) {
                                mediaImporter.listeners.add(mediaImporterListener)
                                view?.setLoadingState(AlbumListContract.LoadingState.Scanning)
                            } else {
                                mediaImporter.listeners.remove(mediaImporterListener)
                                view?.setLoadingState(AlbumListContract.LoadingState.Empty)
                            }
                        } else {
                            mediaImporter.listeners.remove(mediaImporterListener)
                            view?.setLoadingState(AlbumListContract.LoadingState.None)
                        }
                        view?.setAlbums(albums)
                    },
                    onError = { error -> Timber.e(error, "Failed to retrieve albums") })
        )
    }

    override fun addToQueue(album: Album) {
        addDisposable(
            songRepository.getSongs(SongQuery.AlbumIds(listOf(album.id)))
                .first(emptyList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { songs ->
                        playbackManager.addToQueue(songs)
                        view?.onAddedToQueue(album)
                    },
                    onError = { throwable -> Timber.e(throwable, "Failed to retrieve songs for album: ${album.name}") })
        )
    }
}