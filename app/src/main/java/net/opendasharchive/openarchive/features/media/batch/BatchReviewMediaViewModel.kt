package net.opendasharchive.openarchive.features.media.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.util.extensions.runOnBackground

class BatchReviewMediaViewModel : ViewModel() {

    fun saveMedia(
        media: Media?,
        title: String,
        description: String,
        author: String,
        location: String,
        tags: String
    ) {
        @Suppress("NAME_SHADOWING")
        val media = media ?: return

        viewModelScope.runOnBackground {
            media.title = title
            media.description = description
            media.author = author
            media.location = location
            media.setTags(tags)
            media.licenseUrl = media.project?.licenseUrl
            if (media.sStatus == Media.Status.New) media.sStatus = Media.Status.Local
            media.save()
        }
    }
}