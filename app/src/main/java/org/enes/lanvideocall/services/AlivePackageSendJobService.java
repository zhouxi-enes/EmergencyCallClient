package org.enes.lanvideocall.services;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class AlivePackageSendJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
