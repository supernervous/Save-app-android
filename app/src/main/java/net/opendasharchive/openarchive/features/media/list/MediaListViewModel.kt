package net.opendasharchive.openarchive.features.media.list

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.repository.*
import net.opendasharchive.openarchive.util.Constants.EMPTY_ID
import net.opendasharchive.openarchive.util.SharedPreferencesHelper

class MediaListViewModel(
    private val context: Application
) : ViewModel() {

    private val collectionRepository: CollectionRepository = CollectionRepositoryImpl()
    private val mediaRepository: MediaRepository = MediaRepositoryImpl()
    private val projectRepository: ProjectRepository by lazy {
        ProjectRepositoryImpl(SharedPreferencesHelper.newInstance(context))
    }

    val projectId = projectRepository.getProjectId()

    private val _mediaList = MutableLiveData<List<Media>?>()
    val mediaList: LiveData<List<Media>?>
        get() = _mediaList

    fun getMediaList(status: LongArray) {
        viewModelScope.launch {
            if (projectId == EMPTY_ID) {
                _mediaList.value = Media.getMediaByStatus(status, Media.ORDER_PRIORITY)
            } else {
                _mediaList.value = Media.getMediaByProject(projectId)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class MediaListViewModelProvider(
    private val context: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaListViewModel::class.java)) {
            return MediaListViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}