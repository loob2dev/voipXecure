package com.XECUREVoIP;

/*
XecureLauncherActivity.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.XECUREVoIP.Service.AboveOreoObserveService;
import com.XECUREVoIP.Service.BelowOreoObserveService;
import com.XECUREVoIP.Service.XecureService;
import com.XECUREVoIP.call.CallActivity;
import com.XECUREVoIP.chat.ChatUtils.ChatRoomDBHelper;
import com.XECUREVoIP.contacts.ContactDBHelper;
import com.XECUREVoIP.contacts.ContactsManager;
import com.XECUREVoIP.security.LoginWithPassActivity;
import com.XECUREVoIP.security.LoginWithPatternActivity;
import com.XECUREVoIP.security.createSecurity.GenSecurityActivity;
import com.XECUREVoIP.tutorials.TutorialLauncherActivity;


import com.XECUREVoIP.assistant.RemoteProvisioningActivity;

import org.linphone.mediastream.Version;

import static android.content.Intent.ACTION_MAIN;

/**
 * Launch Linphone main activity when Service is ready.
 */
public class XecureLauncherActivity extends Activity {

	private final String ACTION_CALL_LINPHONE  = "org.linphone.intent.action.CallLaunched";

	private Handler mHandler;
	private ServiceWaitThread mServiceThread;
	private String addressToCall;
	private Uri uriToResolve;
	public final int LOGIN_REQUEST = 0x001;
	public final int MY_IGNORE_OPTIMIZATION_REQUEST = 0x002;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//turn doze mode off
//		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//			boolean isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(getPackageName());
//			if(!isIgnoringBatteryOptimizations){
//				Intent intent = new Intent();
//				intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//				intent.setData(Uri.parse("package:" + getPackageName()));
//				startActivityForResult(intent, MY_IGNORE_OPTIMIZATION_REQUEST);
//			}
//			else startXecure();
//		}else
		startXecure();
	}

	private void startXecure() {
		//check the database
		confirmDB();
		//turn on observe
		runObserveService();
		// Hack to avoid to draw twice XecureActivity on tablets
		if (getResources().getBoolean(R.bool.orientation_portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.launch_screen);

		AnimationDrawable animFrame = (AnimationDrawable)findViewById(R.id.id_animLoading).getBackground();
		animFrame.setOneShot(true);
		animFrame.start();

		mHandler = new Handler();

		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();
			if (Intent.ACTION_CALL.equals(action)) {
				if (intent.getData() != null) {
					addressToCall = intent.getData().toString();
					addressToCall = addressToCall.replace("%40", "@");
					addressToCall = addressToCall.replace("%3A", ":");
					if (addressToCall.startsWith("sip:")) {
						addressToCall = addressToCall.substring("sip:".length());
					}
				}
			} else if (Intent.ACTION_VIEW.equals(action)) {
				if (XecureService.isReady()) {
					addressToCall = ContactsManager.getInstance().getAddressOrNumberForAndroidContact(getContentResolver(), intent.getData());
				} else {
					uriToResolve = intent.getData();
				}
			}
		}

		if (XecureService.isReady()) {
			onServiceReady();
		} else {
			// start secure voip as background
			scheduleJob();
		}
	}

	private void confirmDB() {
		ContactDBHelper dbHelper = new ContactDBHelper(this);
		Cursor cursor = dbHelper.getAllData();
		if (cursor.getColumnIndex(ContactDBHelper.COL_LAST_NAME) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_FIRST_NAME) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_EMAIL) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_SIP_ACCOUNT) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_NUMBER00) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_NUMBER01) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_PHOTO_URI) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_COMPANY) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_DEPARTMENT) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_SUB_DEPARTMENT) > 0
				&& cursor.getColumnIndex(ContactDBHelper.COL_SHARE) > 0)
			return;
		else
			deleteDatabase(ContactDBHelper.DATABASE_NAME);

		ChatRoomDBHelper chat_dbHelper = new ChatRoomDBHelper(this);
		Cursor chat_cursor = chat_dbHelper.getAllData();
		if (chat_cursor.getColumnIndex(ChatRoomDBHelper.COL_ENTRY_ID) > 0
				&& chat_cursor.getColumnIndex(ChatRoomDBHelper.COL_ACCEPT) > 0
				&& chat_cursor.getColumnIndex(ChatRoomDBHelper.COL_EXCHANGED) > 0
				&& chat_cursor.getColumnIndex(ChatRoomDBHelper.COL_KEY) > 0)
			return;
		else
			deleteDatabase(ChatRoomDBHelper.DATABASE_NAME);
	}

	private void runObserveService() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			AboveOreoObserveService.enqueueWork(this, new Intent());
		}else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
			JobScheduler jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
			ComponentName componentName = new ComponentName((XecureLauncherActivity.this), BelowOreoObserveService.class);
			JobInfo jobInfo = new JobInfo.Builder(1, componentName)
					.setPeriodic(100)
					.setPersisted(true)
					.build();
			jobScheduler.schedule(jobInfo);
		}
	}

	private void scheduleJob() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(new Intent(ACTION_MAIN).setClass(this, XecureService.class));
		}
		else
			startService(new Intent(ACTION_MAIN).setClass(this, XecureService.class));
		mServiceThread = new ServiceWaitThread();
		mServiceThread.start();
	}
	@Override
	protected void onResume(){
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	protected void onServiceReady() {
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
			    if (XecurePreferences.instance().isFirstLaunch() || XecureActivity.isInstanciated())
                {
                    startLinphone();
                }else if (!isLocked())
					startActivity(new Intent(XecureLauncherActivity.this, GenSecurityActivity.class));
			    else{
			    	if (isPatten()){
						Intent intent = new Intent(XecureLauncherActivity.this, LoginWithPatternActivity.class);
						intent.putExtra("ForResult", true);
						startActivityForResult(intent, LOGIN_REQUEST);
					}else{
						Intent intent = new Intent(XecureLauncherActivity.this, LoginWithPassActivity.class);

						startActivityForResult(intent, LOGIN_REQUEST);
					}
                }
			}
		}, 1000);
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            finish();
        switch (requestCode){
			case RESULT_OK:
				startLinphone();
				break;
			case MY_IGNORE_OPTIMIZATION_REQUEST:
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
					boolean isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(getPackageName());
					if(isIgnoringBatteryOptimizations){
						startXecure();
					}else{
						Toast.makeText(this, "Ignore battery optimization for running safely.", Toast.LENGTH_LONG).show();
						Intent intent = new Intent();
						intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
						intent.setData(Uri.parse("package:" + getPackageName()));
						startActivityForResult(intent, MY_IGNORE_OPTIMIZATION_REQUEST);
					}
				}
		}
    }

    private void startLinphone() {
        Class<? extends Activity> classToStart;
        if (getResources().getBoolean(R.bool.show_tutorials_instead_of_app)) {
            classToStart = TutorialLauncherActivity.class;
        } else if (getResources().getBoolean(R.bool.display_sms_remote_provisioning_activity) && XecurePreferences.instance().isFirstRemoteProvisioning()) {
            classToStart = RemoteProvisioningActivity.class;
        } else {
            classToStart = XecureActivity.class;
        }

        // We need XecureService to start bluetoothManager
        if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
            BluetoothManager.getInstance().initBluetooth();
        }

		Intent newIntent = new Intent(XecureLauncherActivity.this, classToStart);
		Intent intent = getIntent();
		String stringFileShared = null;
		String stringUriFileShared = null;
		Uri fileUri = null;
		if (intent != null) {
			String action = intent.getAction();
			String type = intent.getType();
			newIntent.setData(intent.getData());
			if (Intent.ACTION_SEND.equals(action) && type != null) {
				if (type.contains("text/")){
					if(("text/plain").equals(type) && intent.getStringExtra(Intent.EXTRA_TEXT)!= null) {
						stringFileShared = intent.getStringExtra(Intent.EXTRA_TEXT);
						newIntent.putExtra("msgShared", stringFileShared);
					} else if(((Uri) intent.getExtras().get(Intent.EXTRA_STREAM)) != null){
						stringFileShared = (XecureUtils.createCvsFromString(XecureUtils.processContactUri(getApplicationContext(), (Uri)intent.getExtras().get(Intent.EXTRA_STREAM)))).toString();
						newIntent.putExtra("fileShared", stringFileShared);
					}
				}else {
					if(intent.getStringExtra(Intent.EXTRA_STREAM) != null){
						stringUriFileShared = intent.getStringExtra(Intent.EXTRA_STREAM);
					}else {
						fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
						stringUriFileShared = XecureUtils.getRealPathFromURI(getBaseContext(), fileUri);
						if(stringUriFileShared == null)
							if(fileUri.getPath().contains("/0/1/mediakey:/local")) {
								stringUriFileShared = XecureUtils.getFilePath(getBaseContext(), fileUri);
							}else
								stringUriFileShared = fileUri.getPath();
					}
					newIntent.putExtra("fileShared", stringUriFileShared);
				}
			}else if( ACTION_CALL_LINPHONE.equals(action) && (intent.getStringExtra("NumberToCall") != null)) {
				String numberToCall = intent.getStringExtra("NumberToCall");
				if (CallActivity.isInstanciated()) {
					CallActivity.instance().startIncomingCallActivity();
				} else {
					XecureManager.getInstance().newOutgoingCall(numberToCall, null);
				}
			}
		}
		if (uriToResolve != null) {
			addressToCall = ContactsManager.getInstance().getAddressOrNumberForAndroidContact(getContentResolver(), uriToResolve);
			Log.i("LinphoneLauncher", "Intent has uri to resolve : " + uriToResolve.toString());
			uriToResolve = null;
		}
		if (addressToCall != null) {
			newIntent.putExtra("SipUriOrNumber", addressToCall);
			Log.i("LinphoneLauncher", "Intent has address to call : " + addressToCall);
			addressToCall = null;
		}
		startActivity(newIntent);
		if (classToStart == XecureActivity.class && XecureActivity.isInstanciated() && (stringFileShared != null || fileUri != null)) {
			if(stringFileShared != null) {
				XecureActivity.instance().displayChat(null, stringFileShared, null);
			}
			else if(fileUri != null) {
				XecureActivity.instance().displayChat(null, null, stringUriFileShared);
			}
		}
		finish();
	}


	private class ServiceWaitThread extends Thread {
		public void run() {
			while (!XecureService.isReady()) {
				try {
					sleep(30);
				} catch (InterruptedException e) {
					throw new RuntimeException("waiting thread sleep() has been interrupted");
				}
			}
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					onServiceReady();
				}
			});
			mServiceThread = null;
		}
	}
	private boolean isPatten() {
		SharedPreferences preXecue = getSharedPreferences("Xecure", MODE_PRIVATE);
		String strCrrEncryptionPass = preXecue.getString("pass", "");

		return strCrrEncryptionPass.isEmpty()? true: false;
	}
    private boolean isLocked() {
        SharedPreferences preXecue = getSharedPreferences("Xecure", MODE_PRIVATE);
        String strCrrEncryptionPass = preXecue.getString("pass", "");
        String strCrrEncryptionPattern = preXecue.getString("pattern", "");
        if (strCrrEncryptionPass.isEmpty() && strCrrEncryptionPattern.isEmpty())
            return false;

        return true;
    }
}