package com.simplecityapps.provider.jellyfin

import android.net.Uri
import androidx.core.net.toUri
import com.simplecityapps.mediaprovider.MediaInfo
import com.simplecityapps.mediaprovider.MediaInfoProvider
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.provider.jellyfin.http.JellyfinTranscodeService
import javax.inject.Inject

class JellyfinMediaInfoProvider @Inject constructor(
    private val jellyfinAuthenticationManager: JellyfinAuthenticationManager,
    private val jellyfinTranscodeService: JellyfinTranscodeService
) : MediaInfoProvider {

    override fun handles(uri: Uri): Boolean {
        return uri.scheme == "jellyfin"
    }

    @Throws(IllegalStateException::class)
    override suspend fun getMediaInfo(song: Song): MediaInfo {
        val jellyfinPath = jellyfinAuthenticationManager.getAuthenticatedCredentials()?.let { authenticatedCredentials ->
            jellyfinAuthenticationManager.buildJellyfinPath(
                Uri.parse(song.path).pathSegments.last(),
                authenticatedCredentials
            )?.toUri() ?: run {
                throw IllegalStateException("Failed to build jellyfin path")
            }
        } ?: run {
            throw IllegalStateException("Failed to authenticate")
        }

        val mimeType = getMimeType(jellyfinPath, song.mimeType)

        return MediaInfo(path = jellyfinPath, mimeType = mimeType, isRemote = true)
    }

    private suspend fun getMimeType(path: Uri, defaultMimeType: String): String {
        val response = jellyfinTranscodeService.transcode(path.toString())
        return if (response.isSuccessful) {
            return response.headers().get("Content-Type") ?: defaultMimeType
        } else {
            defaultMimeType
        }
    }
}