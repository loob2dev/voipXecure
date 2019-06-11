package com.XECUREVoIP;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.XECUREVoIP.Service.AboveOreoObserveService;
import com.XECUREVoIP.Service.BelowOreoObserveService;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class RestartObserveReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AboveOreoObserveService.enqueueWork(context, new Intent());
    }
}
