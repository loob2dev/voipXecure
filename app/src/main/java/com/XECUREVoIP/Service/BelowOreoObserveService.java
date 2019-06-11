package com.XECUREVoIP.Service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.XECUREVoIP.RefreshAlarmReceiver;
import com.XECUREVoIP.R;
import com.XECUREVoIP.XecurePreferences;

/**
 * Created by Amal on 01/06/2018.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BelowOreoObserveService extends JobService {

    private static final String TAG = "BelowOreoObserveService";


    @Override
    public boolean onStartJob(JobParameters params) {
        myTask doThisTask = new myTask();
        doThisTask.execute();

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {

        return false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        super.onTaskRemoved(rootIntent);
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
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        bDoze = pm.isDeviceIdleMode();
                    }
                    if (bDoze && XecureService.isReady()){
                        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "turn service on");
                        wakeLock.acquire();
                    }
                    //check the xecure service
                    SharedPreferences preXecue = getSharedPreferences("Xecure", MODE_PRIVATE);
                    Boolean isIllegal = preXecue.getBoolean(getResources().getString(R.string.illegal_user), false);

                    if (!isIllegal && !XecureService.isReady()){
                        Intent restartService = new Intent(getApplicationContext(),
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


                        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmService.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +500, restartServicePI);
                        }else
                            alarmService.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +500, restartServicePI);
                        Log.d(TAG, "startService");
                    } else if(!isIllegal && XecureService.isReady() && !XecurePreferences.instance().isFirstLaunch()){
                        Intent intentAlarm = new Intent(getApplicationContext(), RefreshAlarmReceiver.class);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2, intentAlarm, 0);
                        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            alarmService.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +500, pendingIntent);
                        else
                            alarmService.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() +500, pendingIntent);
                    }
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