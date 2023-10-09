package net.opendasharchive.openarchive.features.media.preview

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import net.opendasharchive.openarchive.features.media.MediaWorker
import timber.log.Timber

class PreviewMediaListViewModel(
    application: Application
) : ViewModel() {

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory
    {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Application::class.java).newInstance(application)
        }
    }

    companion object {
        private const val TAG_MEDIA_UPLOADING = "TAG_MEDIA_UPLOADING"

        fun getInstance(owner: ViewModelStoreOwner, application: Application) : PreviewMediaListViewModel {
            return ViewModelProvider(owner, Factory(application))[PreviewMediaListViewModel::class.java]
        }
    }

    private val mWorkState: LiveData<List<WorkInfo>>
    private val mWorkManager = WorkManager.getInstance(application)

    init {
        mWorkState = mWorkManager.getWorkInfosByTagLiveData(TAG_MEDIA_UPLOADING)
    }

    fun applyMedia(): Operation {
        val mediaWorker = OneTimeWorkRequestBuilder<MediaWorker>().addTag(TAG_MEDIA_UPLOADING).build()

        return mWorkManager.enqueue(mediaWorker)
    }

    fun observeValuesForWorkState(activity : AppCompatActivity) {
        mWorkState.observe(activity) { workInfo ->
            workInfo.forEach {
                when (it.state) {
                    WorkInfo.State.RUNNING -> {
                        Timber.e("Loading")
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        Timber.e("Succeed")
                    }
                    WorkInfo.State.FAILED -> {
                        Timber.e("Failed")
                    }
                    else -> {
                        Timber.d("workInfo is null")
                    }
                }
            }
        }
    }
}
