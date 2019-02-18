package com.simplecityapps.shuttle.ui.screens.library.albumartists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.request.RequestOptions
import com.simplecityapps.adapter.ViewBinder
import com.simplecityapps.mediaprovider.model.AlbumArtist
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.imageloading.GlideApp

class AlbumArtistBinder(val albumArtist: AlbumArtist) : ViewBinder {

    interface Listener {
        fun onAlbumArtistClicked(albumArtist: AlbumArtist)
    }

    var listener: Listener? = null

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_album_artist, parent, false))
    }

    override fun viewType(): ViewBinder.ViewType {
        return ViewBinder.ViewType.AlbumArtist
    }

    override fun sectionName(): String? {
        return albumArtist.name.firstOrNull().toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlbumArtistBinder) return false

        if (albumArtist != other.albumArtist) return false

        return true
    }

    override fun hashCode(): Int {
        return albumArtist.hashCode()
    }

    override fun areContentsTheSame(other: Any): Boolean {
        return albumArtist.name == (other as? AlbumArtist)?.name
    }


    class ViewHolder(itemView: View) : ViewBinder.ViewHolder<AlbumArtistBinder>(itemView) {

        init {
            itemView.setOnClickListener { viewBinder?.listener?.onAlbumArtistClicked(viewBinder!!.albumArtist) }
        }

        private val title = itemView.findViewById<TextView>(R.id.title)
        private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        override fun bind(viewBinder: AlbumArtistBinder) {
            super.bind(viewBinder)

            title.text = viewBinder.albumArtist.name
            subtitle.text = viewBinder.albumArtist.toString()

            // Todo: Pass image loader instance into ViewBinder.
            // Todo: Abstract away behind a generic image loading interface
            GlideApp
                .with(itemView.context)
                .load(viewBinder.albumArtist)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)
        }

        override fun recycle() {
            // Todo: Hook up recycler listener to call recycler on RecyclerAdapter
            GlideApp.with(itemView.context).clear(imageView)
        }
    }
}