package net.opendasharchive.openarchive.features.media.grid

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.repository.*
import net.opendasharchive.openarchive.util.SharedPreferencesHelper

class MediaGridViewModel(
    private val context: Application
) : ViewModel() {

    private val collectionRepository: CollectionRepository = CollectionRepositoryImpl()
    private val mediaRepository: MediaRepository = MediaRepositoryImpl()
    private val projectRepository: ProjectRepository by lazy {
        ProjectRepositoryImpl(SharedPreferencesHelper.newInstance(context))
    }

    val projectId = projectRepository.getProjectId()

    private val _collections = MutableLiveData<List<Collection>?>()
    val collections: LiveData<List<Collection>?>
        get() = _collections

    private val _mediaList = MutableLiveData<List<Media>?>()
    val mediaList: LiveData<List<Media>?>
        get() = _mediaList

    private val _currentCollection = MutableLiveData<Collection>()
    val currentCollection: LiveData<Collection>
        get() = _currentCollection

    fun getAllCollection() {
        viewModelScope.launch {
            _collections.value = collectionRepository.getAllAsList()
        }
    }

    fun getAllMediaAndCollections(collection: Collection) {
        viewModelScope.launch {
            _currentCollection.value = collection
            _mediaList.value = mediaRepository.getMediaByProjectAndCollection(
                projectId,
                collection.id
            )
        }
    }

}

@Suppress("UNCHECKED_CAST")
class MediaGridViewModelProvider(
    private val context: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaGridViewModel::class.java)) {
            return MediaGridViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}