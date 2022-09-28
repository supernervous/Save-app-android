package net.opendasharchive.openarchive.publish

import android.annotation.SuppressLint
import android.app.job.JobService
import android.app.job.JobParameters
import net.opendasharchive.openarchive.db.Media

@SuppressLint("SpecifyJobSchedulerIdRange")
class PublishJobService : JobService() {
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }
}