package net.opendasharchive.openarchive.features.media.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project

class MediaListViewModel : ViewModel() {

    private val _mediaList = MutableLiveData<List<Media>?>()
    val mediaList: LiveData<List<Media>?>
        get() = _mediaList

    fun getMediaList(projectId: Long, status: List<Media.Status>) {
        if (projectId == Project.EMPTY_ID) {
            _mediaList.value = Media.getByStatus(status, Media.ORDER_PRIORITY)
        } else {
            _mediaList.value = Media.getByProject(projectId)
        }
    }
}
