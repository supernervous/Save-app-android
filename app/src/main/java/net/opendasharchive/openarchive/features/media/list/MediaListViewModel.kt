package net.opendasharchive.openarchive.features.media.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project

class MediaListViewModel : ViewModel() {

    private val mMedia = MutableLiveData<List<Media>?>()
    val media: LiveData<List<Media>?>
        get() = mMedia

    fun setMedia(projectId: Long, status: List<Media.Status>) {
        if (projectId == Project.EMPTY_ID) {
            mMedia.value = Media.getByStatus(status, Media.ORDER_PRIORITY)
        } else {
            mMedia.value = Media.getByProject(projectId)
        }
    }
}
