package net.opendasharchive.openarchive.features.main

import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ViewSectionBinding
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import java.text.DateFormat
import java.text.NumberFormat

data class SectionViewHolder(
    private val binding: ViewSectionBinding
) {

    companion object {

        private val mNf
            get() = NumberFormat.getIntegerInstance()

        private val mDf
            get() = DateFormat.getDateTimeInstance()

    }

    val root
        get() = binding.root

    val timestamp
        get() = binding.timestamp

    val count
        get() = binding.count

    val recyclerView
        get() = binding.recyclerView

    val bottomSpacing
        get() = binding.bottomSpacing

    fun setHeader(
        collection: Collection,
        media: List<Media>
    ) {
        if (media.firstOrNull { it.sStatus == Media.Status.Queued
                    || it.sStatus == Media.Status.Uploading
                    || it.sStatus == Media.Status.Error } != null)
        {
            val uploaded = media.filter { it.sStatus == Media.Status.Uploaded }.size

            count.text = count.context.getString(R.string.counter, uploaded, media.size)
        }
        else {
            count.text = mNf.format(media.size)
        }

        val uploadDate = collection.uploadDate

        if (uploadDate != null) {
            timestamp.text = mDf.format(uploadDate)
        }
        else {
            timestamp.text = ""
        }
    }

}