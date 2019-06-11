package com.XECUREVoIP.Service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.XECUREVoIP.R;
import com.XECUREVoIP.RestartObserveReceiver;
import com.XECUREVoIP.RefreshAlarmReceiver;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.XecurePreferences;

import org.linphone.core.LinphoneCore;

public class AboveOreoObserveService extends JobIntentService {

    private static final String TAG = "AboveOreoObserveService";
    private static final int JOB_ID = 0x01;

    public static void enqueueWork(Context context, Intent work){
        enqueueWork(context, AboveOreoObserveService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onStopCurrentWork() {
        Log.d(TAG, "onStopCurrentWork");
        return super.onStopCurrentWork();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        myTask doThisTask = new myTask();
        doThisTask.execute();
    }

    private class myTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            /** displaying a toast ... **/

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {

                @Override
                public void run() {
                    Log.d(TAG, "onHandleWork");
                    // if device is idle, wake it up.
                    PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                    boolean bDoze = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        bDoze = pm.isDeviceIdleMode();
                    }
                    if (bDoze && XecureService.isReady()){
                        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "turn service on");
                        wakeLock.acquire();
                    }
                    //check the xecure service
                    AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    SharedPreferences preXecue = getSharedPreferences("Xecure", MODE_PRIVATE);
                    Boolean isIllegal = preXecue.getBoolean(getResources().getString(R.string.illegal_user), false);
                    if (!isIllegal && !XecureService.isReady()) {
                        Intent restartService = new Intent(getApplicationContext(),
                                XecureService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(restartService);
                        }
                        /*Intent restartService = new Intent(getApplicationContext(),
                                XecureService.class);
                        restartService.setPackage(getPackageName());
                        PendingIntent restartServicePI = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            restartServicePI = PendingIntent.getForegroundService(
                                    getApplicationContext(), 1, restartService,
                                    PendingIntent.FLAG_ONE_SHOT);
                        }else {
                            restartServicePI = PendingIntent.getService(
                                    getApplicationContext(), 1, restartService,
                                    PendingIntent.FLAG_ONE_SHOT);
                        }

                        //Restart the service once it has been killed android
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmService.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +500, restartServicePI);
                        }else
                            alarmService.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +500, restartServicePI);
                            */
                        Log.d(TAG, "startService");
                    } else if(!isIllegal && XecureService.isReady() && !XecurePreferences.instance().isFirstLaunch()){
                        LinphoneCore lc =  XecureManager.getLcIfManagerNotDestroyedOrNull();
                        if (lc != null && lc.getDefaultProxyConfig() != null &&
                                lc.getDefaultProxyConfig().getState() != LinphoneCore.RegistrationState.RegistrationOk) {
                            Log.d(TAG, lc.getDefaultProxyConfig().getState().toString());
                            lc.refreshRegisters();
                        }
                        /*
                        Intent intentAlarm = new Intent(getApplicationContext(), RefreshAlarmReceiver.class);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2, intentAlarm, 0);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            alarmService.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +500, pendingIntent);
                        else
                            alarmService.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +500, pendingIntent);
                            */
                    }
                    //restart observe
                    Intent intentObserve = new Intent(getApplicationContext(), RestartObserveReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 3, intentObserve, 0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        alarmService.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +10000, pendingIntent);
                    else
                        alarmService.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +5000, pendingIntent);
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            super.onPostExecute(aVoid);
        }
    }
}
