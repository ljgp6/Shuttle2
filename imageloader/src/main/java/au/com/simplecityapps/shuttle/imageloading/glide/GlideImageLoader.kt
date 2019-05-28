package au.com.simplecityapps.shuttle.imageloading.glide

import android.app.Activity
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import au.com.simplecityapps.R
import au.com.simplecityapps.shuttle.imageloading.ArtworkImageLoader
import au.com.simplecityapps.shuttle.imageloading.CompletionHandler
import au.com.simplecityapps.shuttle.imageloading.glide.module.GlideApp
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.simplecityapps.mediaprovider.model.Album
import com.simplecityapps.mediaprovider.model.AlbumArtist
import com.simplecityapps.mediaprovider.model.Song

class GlideImageLoader : ArtworkImageLoader {

    sealed class LoadResult {
        object Success : LoadResult()
        object Failure : LoadResult()
    }

    private var requestManager: RequestManager

    constructor(fragment: Fragment) {
        this.requestManager = GlideApp.with(fragment)
    }

    constructor(activity: Activity) {
        this.requestManager = GlideApp.with(activity)
    }

    override fun loadArtwork(imageView: ImageView, albumArtist: AlbumArtist, vararg options: ArtworkImageLoader.Options, completionHandler: CompletionHandler) {
        loadArtwork(imageView, albumArtist as Any, *options, completionHandler = completionHandler)
    }

    override fun loadArtwork(imageView: ImageView, album: Album, vararg options: ArtworkImageLoader.Options, completionHandler: CompletionHandler) {
        loadArtwork(imageView, album as Any, *options, completionHandler = completionHandler)
    }

    override fun loadArtwork(imageView: ImageView, song: Song, vararg options: ArtworkImageLoader.Options, completionHandler: CompletionHandler) {
        loadArtwork(imageView, song as Any, *options, completionHandler = completionHandler)
    }

    @DrawableRes
    var placeHolderResId: Int = R.drawable.ic_placeholder_light

    private fun <T> loadArtwork(imageView: ImageView, `object`: T, vararg options: ArtworkImageLoader.Options, completionHandler: CompletionHandler) {
        val glideRequest = getRequestBuilder(*options)

        completionHandler?.let {
            glideRequest.addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    completionHandler(LoadResult.Failure)
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    completionHandler(LoadResult.Success)
                    return false
                }
            })
        }

        glideRequest
            .load(`object`)
            .into(imageView)
    }

    private fun getRequestBuilder(vararg options: ArtworkImageLoader.Options): RequestBuilder<Drawable> {
        val glideRequest = requestManager
            .asDrawable()
            .placeholder(placeHolderResId)

        options.forEach { option ->
            when (option) {
                is ArtworkImageLoader.Options.CircleCrop -> {
                    glideRequest.apply(RequestOptions.circleCropTransform())
                }
                is ArtworkImageLoader.Options.RoundedCorners -> {
                    glideRequest.apply(RequestOptions.bitmapTransform(MultiTransformation(mutableListOf(CenterCrop(), RoundedCorners(option.radius)))))
                }
                is ArtworkImageLoader.Options.Priority -> {
                    when (option.priority) {
                        ArtworkImageLoader.Options.Priority.Priority.Low -> glideRequest.priority(Priority.LOW)
                        ArtworkImageLoader.Options.Priority.Priority.Default -> glideRequest.priority(Priority.NORMAL)
                        ArtworkImageLoader.Options.Priority.Priority.High -> glideRequest.priority(Priority.HIGH)
                        ArtworkImageLoader.Options.Priority.Priority.Max -> glideRequest.priority(Priority.IMMEDIATE)
                    }
                }
                is ArtworkImageLoader.Options.Crossfade -> {
                    glideRequest.transition(DrawableTransitionOptions.withCrossFade(option.duration))
                }
            }
        }

        return glideRequest
    }

    override fun clear(imageView: ImageView) {
        requestManager.clear(imageView)
    }
}