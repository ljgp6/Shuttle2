package com.simplecityapps.playback.dagger

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.util.LruCache
import androidx.core.content.getSystemService
import au.com.simplecityapps.shuttle.imageloading.ArtworkImageLoader
import com.simplecityapps.mediaprovider.AggregateMediaInfoProvider
import com.simplecityapps.mediaprovider.repository.*
import com.simplecityapps.playback.*
import com.simplecityapps.playback.androidauto.MediaIdHelper
import com.simplecityapps.playback.audiofocus.AudioFocusHelper
import com.simplecityapps.playback.audiofocus.AudioFocusHelperApi21
import com.simplecityapps.playback.audiofocus.AudioFocusHelperApi26
import com.simplecityapps.playback.chromecast.CastService
import com.simplecityapps.playback.chromecast.CastSessionManager
import com.simplecityapps.playback.chromecast.HttpServer
import com.simplecityapps.playback.dsp.equalizer.Equalizer
import com.simplecityapps.playback.dsp.replaygain.ReplayGainAudioProcessor
import com.simplecityapps.playback.exoplayer.EqualizerAudioProcessor
import com.simplecityapps.playback.exoplayer.ExoPlayerPlayback
import com.simplecityapps.playback.mediasession.MediaSessionManager
import com.simplecityapps.playback.persistence.PlaybackPreferenceManager
import com.simplecityapps.playback.queue.QueueManager
import com.simplecityapps.playback.queue.QueueWatcher
import com.simplecityapps.playback.sleeptimer.SleepTimer
import com.simplecityapps.provider.emby.EmbyMediaInfoProvider
import com.simplecityapps.provider.jellyfin.JellyfinMediaInfoProvider
import com.simplecityapps.provider.plex.PlexMediaInfoProvider
import com.simplecityapps.shuttle.dagger.AppScope
import com.simplecityapps.shuttle.persistence.GeneralPreferenceManager
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named

@Module
class PlaybackModule {

    @AppScope
    @Provides
    fun provideQueueWatcher(): QueueWatcher {
        return QueueWatcher()
    }

    @AppScope
    @Provides
    fun provideQueueManager(queueWatcher: QueueWatcher): QueueManager {
        return QueueManager(queueWatcher)
    }

    @AppScope
    @Provides
    fun provideEqualizer(playbackPreferenceManager: PlaybackPreferenceManager): EqualizerAudioProcessor {
        return EqualizerAudioProcessor(playbackPreferenceManager.equalizerEnabled).apply {
            // Restore current eq
            preset = playbackPreferenceManager.preset

            // Restore custom eq bands
            playbackPreferenceManager.customPresetBands?.forEach { restoredBand ->
                Equalizer.Presets.custom.bands.forEach { customBand ->
                    if (customBand.centerFrequency == restoredBand.centerFrequency) {
                        customBand.gain = restoredBand.gain
                    }
                }
            }
        }
    }

    @AppScope
    @Provides
    fun provideReplayGainAudioProcessor(playbackPreferenceManager: PlaybackPreferenceManager): ReplayGainAudioProcessor {
        return ReplayGainAudioProcessor(playbackPreferenceManager.replayGainMode, playbackPreferenceManager.preAmpGain)
    }

    @AppScope
    @Provides
    fun providePlaybackPreferenceManager(sharedPreferences: SharedPreferences, moshi: Moshi): PlaybackPreferenceManager {
        return PlaybackPreferenceManager(sharedPreferences, moshi)
    }

    @AppScope
    @Provides
    fun provideAggregateMediaPathProvider(
        embyMediaPathProvider: EmbyMediaInfoProvider,
        jellyfinMediaPathProvider: JellyfinMediaInfoProvider,
        plexMediaPathProvider: PlexMediaInfoProvider
    ): AggregateMediaInfoProvider {
        return AggregateMediaInfoProvider(
            mutableSetOf(
                embyMediaPathProvider,
                jellyfinMediaPathProvider,
                plexMediaPathProvider
            )
        )
    }

    @Provides
    fun provideExoPlayerPlayback(
        context: Context,
        equalizerAudioProcessor: EqualizerAudioProcessor,
        replayGainAudioProcessor: ReplayGainAudioProcessor,
        mediaPathProvider: AggregateMediaInfoProvider
    ): ExoPlayerPlayback {
        return ExoPlayerPlayback(context, equalizerAudioProcessor, replayGainAudioProcessor, mediaPathProvider)
    }

    @AppScope
    @Provides
    fun providePlaybackWatcher(): PlaybackWatcher {
        return PlaybackWatcher()
    }

    @AppScope
    @Provides
    fun provideAudioFocusHelper(context: Context, playbackWatcher: PlaybackWatcher): AudioFocusHelper {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return AudioFocusHelperApi26(context, playbackWatcher)
        } else {
            return AudioFocusHelperApi21(context, playbackWatcher)
        }
    }

    @Provides
    fun provideMediaIdHelper(playlistRepository: PlaylistRepository, artistRepository: AlbumArtistRepository, albumRepository: AlbumRepository, songRepository: SongRepository): MediaIdHelper {
        return MediaIdHelper(playlistRepository, artistRepository, albumRepository, songRepository)
    }

    @Provides
    fun provideAudioManager(context: Context): AudioManager? {
        return context.getSystemService()
    }

    @Provides
    fun provideAudioEffectSessionManager(context: Context): AudioEffectSessionManager {
        return AudioEffectSessionManager(context)
    }

    @AppScope
    @Provides
    fun providePlaybackManager(
        queueManager: QueueManager,
        playback: ExoPlayerPlayback,
        playbackWatcher: PlaybackWatcher,
        audioFocusHelper: AudioFocusHelper,
        playbackPreferenceManager: PlaybackPreferenceManager,
        audioEffectSessionManager: AudioEffectSessionManager,
        @Named("AppCoroutineScope") coroutineScope: CoroutineScope,
        queueWatcher: QueueWatcher,
        audioManager: AudioManager?
    ): PlaybackManager {
        return PlaybackManager(queueManager, playbackWatcher, audioFocusHelper, playbackPreferenceManager, audioEffectSessionManager, coroutineScope, playback, queueWatcher, audioManager)
    }

    @AppScope
    @Provides
    fun provideCastService(context: Context, songRepository: SongRepository, artworkImageLoader: ArtworkImageLoader): CastService {
        return CastService(context, songRepository, artworkImageLoader)
    }

    @AppScope
    @Provides
    fun provideHttpServer(castService: CastService): HttpServer {
        return HttpServer(castService)
    }

    @AppScope
    @Provides
    fun provideCastSessionManager(
        context: Context,
        playbackManager: PlaybackManager,
        httpServer: HttpServer,
        exoPlayerPlayback: ExoPlayerPlayback,
        mediaPathProvider: AggregateMediaInfoProvider
    ): CastSessionManager {
        return CastSessionManager(playbackManager, context, httpServer, exoPlayerPlayback, mediaPathProvider)
    }

    @AppScope
    @Provides
    fun provideMediaSessionManager(
        context: Context,
        @Named("AppCoroutineScope") appCoroutineScope: CoroutineScope,
        playbackManager: PlaybackManager,
        queueManager: QueueManager,
        artistRepository: AlbumArtistRepository,
        albumRepository: AlbumRepository,
        songRepository: SongRepository,
        genreRepository: GenreRepository,
        artworkImageLoader: ArtworkImageLoader,
        artworkCache: LruCache<String, Bitmap?>,
        preferenceManager: GeneralPreferenceManager,
        playbackWatcher: PlaybackWatcher,
        queueWatcher: QueueWatcher,
        mediaIdHelper: MediaIdHelper
    ): MediaSessionManager {
        return MediaSessionManager(
            context,
            appCoroutineScope,
            playbackManager,
            queueManager,
            mediaIdHelper,
            artistRepository,
            albumRepository,
            songRepository,
            genreRepository,
            artworkImageLoader,
            artworkCache,
            preferenceManager,
            playbackWatcher,
            queueWatcher
        )
    }

    @AppScope
    @Provides
    fun provideNoiseManager(context: Context, playbackManager: PlaybackManager, playbackWatcher: PlaybackWatcher): NoiseManager {
        return NoiseManager(context, playbackManager, playbackWatcher)
    }

    @AppScope
    @Provides
    fun providePlaybackNotificationManager(
        context: Context,
        playbackManager: PlaybackManager,
        queueManager: QueueManager,
        mediaSessionManager: MediaSessionManager,
        playbackWatcher: PlaybackWatcher,
        queueWatcher: QueueWatcher,
        lruCache: LruCache<String, Bitmap>,
        artworkImageLoader: ArtworkImageLoader
    ): PlaybackNotificationManager {
        return PlaybackNotificationManager(
            context,
            context.getSystemService()!!,
            playbackManager,
            queueManager,
            mediaSessionManager,
            playbackWatcher,
            queueWatcher,
            lruCache,
            artworkImageLoader
        )
    }

    @AppScope
    @Provides
    fun provideSleepTimer(playbackManager: PlaybackManager, playbackWatcher: PlaybackWatcher): SleepTimer {
        return SleepTimer(playbackManager, playbackWatcher)
    }
}