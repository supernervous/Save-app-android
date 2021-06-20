package net.opendasharchive.openarchive.publish

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager

fun applyMedia(context: Context, owner: LifecycleOwner) {

    val mediaWorker = OneTimeWorkRequestBuilder<MediaWorker>().build()

    //val result = WorkManager.getInstance(context).beginWith(mediaWorker).then(uploadWorker).enqueue()
    val result = WorkManager.getInstance(context).enqueue(mediaWorker)

    val workInfo = WorkManager.getInstance(context).getWorkInfoByIdLiveData(mediaWorker.id)
        .observe(owner, Observer { workInfo ->
            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
               Toast.makeText(context, "Upload completed", Toast.LENGTH_LONG).show()
            }
        })

}