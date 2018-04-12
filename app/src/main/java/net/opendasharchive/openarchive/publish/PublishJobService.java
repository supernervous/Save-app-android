package net.opendasharchive.openarchive.publish;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PublishJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        startService(new Intent(this, PublishService.class));

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
