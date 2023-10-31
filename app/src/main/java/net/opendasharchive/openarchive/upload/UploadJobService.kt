package net.opendasharchive.openarchive.upload

import android.annotation.SuppressLint
import android.app.job.JobService
import android.app.job.JobParameters

@SuppressLint("SpecifyJobSchedulerIdRange")
class UploadJobService : JobService() {
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }
}