package org.oxycblt.auxio.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import org.oxycblt.auxio.BuildConfig
import org.oxycblt.auxio.R
import org.oxycblt.auxio.logD
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.recycler.DisplayMode
import org.oxycblt.auxio.settings.ui.AccentAdapter
import org.oxycblt.auxio.ui.ACCENTS
import org.oxycblt.auxio.ui.Accent

/**
 * The actual fragment containing the settings menu. Inherits [PreferenceFragmentCompat].
 * @author OxygenCobalt
 */
@Suppress("UNUSED")
class SettingsListFragment : PreferenceFragmentCompat() {
    private val playbackModel: PlaybackViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceScreen.children.forEach {
            recursivelyHandleChildren(it)
        }

        logD("Fragment created.")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_main, rootKey)
    }

    private fun recursivelyHandleChildren(pref: Preference) {
        if (pref is PreferenceCategory) {
            // Show the debug category if this build is a debug build
            if (pref.title == getString(R.string.debug_title) && BuildConfig.DEBUG) {
                logD("Showing debug category.")

                pref.isVisible = true
            }

            // If this preference is a category of its own, handle its own children
            pref.children.forEach { recursivelyHandleChildren(it) }
        } else {
            handlePreference(pref)
        }
    }

    private fun handlePreference(pref: Preference) {
        pref.apply {
            when (key) {
                SettingsManager.Keys.KEY_THEME -> {
                    setIcon(AppCompatDelegate.getDefaultNightMode().toThemeIcon())

                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
                        AppCompatDelegate.setDefaultNightMode((value as String).toThemeInt())

                        setIcon(AppCompatDelegate.getDefaultNightMode().toThemeIcon())

                        true
                    }
                }

                SettingsManager.Keys.KEY_ACCENT -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        showAccentDialog()
                        true
                    }

                    summary = Accent.get().getDetailedSummary(context)
                }

                SettingsManager.Keys.KEY_EDGE_TO_EDGE -> {
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                        requireActivity().recreate()

                        true
                    }
                }

                SettingsManager.Keys.KEY_LIBRARY_DISPLAY_MODE -> {
                    setIcon(SettingsManager.getInstance().libraryDisplayMode.iconRes)

                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
                        setIcon(DisplayMode.valueOfOrFallback(value as String).iconRes)

                        true
                    }
                }

                SettingsManager.Keys.KEY_SHOW_COVERS -> {
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                        Coil.imageLoader(requireContext()).apply {
                            bitmapPool.clear()
                            memoryCache.clear()
                        }

                        requireActivity().recreate()

                        true
                    }
                }

                SettingsManager.Keys.KEY_QUALITY_COVERS -> {
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                        // Clear out any cached images, before recreating the activity
                        Coil.imageLoader(requireContext()).apply {
                            bitmapPool.clear()
                            memoryCache.clear()
                        }

                        requireActivity().recreate()

                        true
                    }
                }

                SettingsManager.Keys.KEY_DEBUG_SAVE -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        playbackModel.save(requireContext())

                        true
                    }
                }
            }
        }
    }

    private fun showAccentDialog() {
        MaterialDialog(requireActivity()).show {
            title(R.string.setting_accent)

            // Roll my own RecyclerView since [To no surprise whatsoever] Material Dialogs
            // has a bug where ugly dividers will show with the RecyclerView even if you disable them.
            // This is why I hate using third party libraries.
            val recycler = RecyclerView(requireContext()).apply {
                adapter = AccentAdapter {
                    if (it != Accent.get()) {
                        SettingsManager.getInstance().accent = it

                        requireActivity().recreate()
                    }

                    this@show.dismiss()
                }

                post {
                    // Combine the width of the recyclerview with the width of an item in order
                    // to center the currently selected accent.
                    val childWidth = getChildAt(0).width / 2

                    (layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(
                            ACCENTS.indexOf(Accent.get()),
                            (width / 2) - childWidth
                        )
                }

                layoutManager = LinearLayoutManager(
                    requireContext()
                ).also { it.orientation = LinearLayoutManager.HORIZONTAL }
            }

            customView(view = recycler)

            view.invalidateDividers(showTop = false, showBottom = false)

            negativeButton(android.R.string.cancel)

            show()
        }
    }
}
