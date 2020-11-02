package com.simplecityapps.shuttle.ui.screens.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.ui.common.PagerAdapter
import com.simplecityapps.shuttle.ui.common.autoCleared
import com.simplecityapps.shuttle.ui.common.view.ToolbarHost
import com.simplecityapps.shuttle.ui.screens.library.albumartists.AlbumArtistListFragment
import com.simplecityapps.shuttle.ui.screens.library.albums.AlbumListFragment
import com.simplecityapps.shuttle.ui.screens.library.genres.GenreListFragment
import com.simplecityapps.shuttle.ui.screens.library.playlists.PlaylistListFragment
import com.simplecityapps.shuttle.ui.screens.library.songs.SongListFragment

class LibraryFragment : Fragment(), ToolbarHost {

    private var toolbar: Toolbar by autoCleared()
        @JvmName("getToolbar_") get // Resolves clash with ToolbarHost function

    private var viewPager: ViewPager by autoCleared()

    private var tabLayout: TabLayout by autoCleared()


    // Lifecycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        tabLayout.setupWithViewPager(viewPager, true)

        val adapter = PagerAdapter(childFragmentManager)
        adapter.addFragment("Genres", GenreListFragment.newInstance())
        adapter.addFragment("Playlists", PlaylistListFragment.newInstance())
        adapter.addFragment("Artists", AlbumArtistListFragment.newInstance())
        adapter.addFragment("Albums", AlbumListFragment.newInstance())
        adapter.addFragment("Songs", SongListFragment.newInstance())
        adapter.notifyDataSetChanged()

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 3
        viewPager.setCurrentItem(2, false)

        toolbar = view.findViewById(R.id.toolbar)
    }


    // ToolbarHost Implementation

    override fun getToolbar(): Toolbar? {
        return toolbar
    }
}