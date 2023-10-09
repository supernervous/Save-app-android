package net.opendasharchive.openarchive.features.media.review

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import net.opendasharchive.openarchive.features.media.MediaWorker
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListViewModel

class ReviewMediaViewModel(
    application: Application
): ViewModel() {

    companion object {
        private const val TAG_MEDIA_UPLOADING = "TAG_MEDIA_UPLOADING"

        fun getInstance(owner: ViewModelStoreOwner, application: Application): ReviewMediaViewModel {
            return ViewModelProvider(owner, PreviewMediaListViewModel.Factory(application))[ReviewMediaViewModel::class.java]
        }
    }

    val workState: LiveData<List<WorkInfo>>

    private val mWorkManager = WorkManager.getInstance(application)

    init {
        workState = mWorkManager.getWorkInfosByTagLiveData(TAG_MEDIA_UPLOADING)
    }

    fun applyMedia() {
        val mediaWorker = OneTimeWorkRequestBuilder<MediaWorker>().addTag(TAG_MEDIA_UPLOADING).build()
        mWorkManager.enqueue(mediaWorker)
    }
}