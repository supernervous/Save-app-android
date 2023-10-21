package net.opendasharchive.openarchive

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.media.grid.MediaGridFragment
import net.opendasharchive.openarchive.features.settings.SettingsFragment
import net.opendasharchive.openarchive.util.SmartFragmentStatePagerAdapter
import timber.log.Timber
import kotlin.math.max

class ProjectAdapter(private val context: Context, fragmentManager: FragmentManager) : SmartFragmentStatePagerAdapter(fragmentManager) {

    var projects = listOf<Project>()
        private set

    override fun getItem(position: Int): Fragment {
        if (position == settingsIndex) {
            return SettingsFragment()
        }

        return MediaGridFragment().also { fragment ->
            getProject(position)?.let { project ->
                fragment.projectId = project.id
            }

            fragment.arguments = Bundle()
        }
    }

    override fun getCount(): Int {
        return max(1, projects.size) + 1
    }

    fun getProject(i: Int): Project? {
        return if (i > -1 && i < projects.size) projects[i] else null
    }

    val settingsIndex: Int
        get() = count - 1

    fun updateData(projects: List<Project>) {
        this.projects = projects

        notifyDataSetChanged()
    }

    fun getIndex(project: Project?): Int {
        if (project == null) {
            return 0
        }

        return (projects.indexOf(project)).coerceAtLeast(0)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return if (position == 0 && projects.isEmpty()) {
            val imageSpan = ImageSpan(context, R.drawable.ic_add_circle_outline_black_24dp)

            val spannableString = SpannableString(" ")
            spannableString.setSpan(imageSpan, 0, 1, 0)

            spannableString
        }
        else {
            getProject(position)?.description
        }
    }

    fun getRegisteredMediaGridFragment(position: Int): MediaGridFragment? {
        return getRegisteredFragment(position) as? MediaGridFragment
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        try {
            super.restoreState(state, loader)
        }
        catch (e: Exception) {
            Timber.e("restoreState failed", e)
        }
    }
}