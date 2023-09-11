package net.opendasharchive.openarchive.features.media

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Recycler
import net.opendasharchive.openarchive.databinding.FragmentMediaListSectionBinding

data class SectionViewHolder(
    var mediaSection: FragmentMediaListSectionBinding,
    var mediaGrid: Recycler? = null,
) {
    val sectionStatus: TextView
        get() = mediaSection.sectionStatus

    val sectionTimestamp: TextView
        get() = mediaSection.sectionTimestamp

    val action: TextView
        get() = mediaSection.actionNext
}