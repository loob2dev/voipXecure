package com.XECUREVoIP;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.XECUREVoIP.Service.AboveOreoObserveService;
import com.XECUREVoIP.Service.BelowOreoObserveService;

import org.linphone.core.LinphoneCore;

public class RefreshAlarmReceiver extends BroadcastReceiver {
    private static String TAG = "ObserveService";
    @Override
    public void onReceive(Context context, Intent intent) {
        LinphoneCore lc =  XecureManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null && lc.getDefaultProxyConfig() != null &&
                lc.getDefaultProxyConfig().getState() != LinphoneCore.RegistrationState.RegistrationOk) {
            Log.d(TAG, lc.getDefaultProxyConfig().getState().toString());
            lc.refreshRegisters();
        }
    }
}
