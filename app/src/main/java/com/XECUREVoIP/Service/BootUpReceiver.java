package com.XECUREVoIP.Service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.XECUREVoIP.Service.AboveOreoObserveService;
import com.XECUREVoIP.XecureLauncherActivity;
import com.XECUREVoIP.compatibility.Compatibility;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AboveOreoObserveService.enqueueWork(context, new Intent());
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            JobScheduler jobScheduler = (JobScheduler)context.getSystemService(JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(context, BelowOreoObserveService.class);
            JobInfo jobInfo = new JobInfo.Builder(1, componentName)
                    .setPeriodic(100)
                    .setPersisted(true)
                    .build();
            jobScheduler.schedule(jobInfo);
        }else {
            Intent lXecureServiceIntent = new Intent(Intent.ACTION_MAIN);
            lXecureServiceIntent.setClass(context, XecureService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(lXecureServiceIntent);
            }else
                context.startService(lXecureServiceIntent);
        }
    }
}
