package net.opendasharchive.openarchive.db

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.media.NewFolderFragment
import net.opendasharchive.openarchive.features.media.grid.MediaGridFragment
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.util.SmartFragmentStatePagerAdapter

class ProjectAdapter(private val context: Context, fragmentManager: FragmentManager) : SmartFragmentStatePagerAdapter(fragmentManager) {

    var projects = listOf<Project>()
        private set

    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            NewFolderFragment()
        }
        else {
            MediaGridFragment().also { fragment ->
                getProject(position)?.let { project ->
                    fragment.setProjectId(project.id)
                }

                fragment.arguments = Bundle()
            }
        }
    }

    override fun getCount(): Int {
        return projects.size + 1
    }

    fun getProject(i: Int): Project? {
        return if (i > 0 && i <= projects.size) projects[i - 1] else null
    }

    fun updateData(projects: List<Project>) {
        this.projects = projects

        notifyDataSetChanged()
    }

    fun getIndex(project: Project?): Int {
        val default = if (projects.isNotEmpty()) 1 else 0

        if (project == null) {
            return default
        }

        return (projects.indexOf(project) + 1).coerceAtLeast(default)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return if (position == 0) {
            val imageSpan = ImageSpan(context, R.drawable.ic_add_circle_outline_black_24dp)

            val spannableString = SpannableString(" ")
            spannableString.setSpan(imageSpan, 0, 1, 0)

            spannableString
        }
        else {
            getProject(position)?.description
        }
    }

    fun getRegisteredMediaListFragment(position: Int): MediaListFragment? {
        return getRegisteredFragment(position) as? MediaListFragment
    }
}