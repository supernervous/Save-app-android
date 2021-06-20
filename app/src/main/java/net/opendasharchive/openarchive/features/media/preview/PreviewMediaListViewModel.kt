package net.opendasharchive.openarchive.features.media.preview

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.*
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.publish.MediaWorker

class PreviewMediaListViewModel(
    private val repository: MediaRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableLiveData<UIState>()
    val uiState: LiveData<UIState>
        get() = _uiState

    val workState: LiveData<List<WorkInfo>>

    private val workManager = WorkManager.getInstance(application)
    private val TAG_MEDIA_UPLOADING = "TAG_MEDIA_UPLOADING"

    init {
        workState = workManager.getWorkInfosByTagLiveData(TAG_MEDIA_UPLOADING)
    }


    fun uploadFiles() {
        _uiState.value = UIState.Loading

        try {
            viewModelScope.launch {

                val mediaList = getFilesOnBackground()
                val result = mediaList.map { media ->
                    uploadFilesOnBackgroundThread(media)
                }
                _uiState.value = UIState.Success("Succesful")
            }
        } catch (ex: Exception) {
            _uiState.value = UIState.Error(ex.message ?: "Something happened")
        }
    }

    private suspend fun getFilesOnBackground() = withContext(Dispatchers.Default) {
        repository.getMedia()
    }

    private suspend fun uploadFilesOnBackgroundThread(media: Media) =
        withContext(Dispatchers.Default) {
            repository.uploadMedia(media)
        }

    fun applyMedia() {
        val mediaWorker = OneTimeWorkRequestBuilder<MediaWorker>().addTag(TAG_MEDIA_UPLOADING).build()
        workManager.enqueue(mediaWorker)
    }


}