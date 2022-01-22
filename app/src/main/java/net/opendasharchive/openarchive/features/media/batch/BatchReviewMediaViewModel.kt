package net.opendasharchive.openarchive.features.media.batch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.repository.MediaRepository
import net.opendasharchive.openarchive.repository.MediaRepositoryImpl
import net.opendasharchive.openarchive.util.extensions.runOnBackground

class BatchReviewMediaViewModel : ViewModel() {

    private val mediaRepository: MediaRepository = MediaRepositoryImpl()

    private val _medias = MutableLiveData<ArrayList<Media>>()
    val medias: LiveData<ArrayList<Media>>
        get() = _medias

    fun saveMedia(
        media: Media?,
        title: String,
        description: String,
        author: String,
        location: String,
        tags: String
    ) {
        media?.apply {
            this.title = title
            this.description = description
            this.author = author
            this.location = location
            this.setTags(tags)
            viewModelScope.runOnBackground {
                mediaRepository.saveMedia(this)
            }
        }
    }

    fun getMediaById(id: LongArray?) {
        val mediaList = arrayListOf<Media>()
        viewModelScope.launch {
            id?.forEach {
                mediaList.add(mediaRepository.getMediaById(it))
            }
        }
        _medias.value = mediaList
    }

}