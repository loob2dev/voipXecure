package com.XECUREVoIP.Service;

/*
XecureService.java
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.XECUREVoIP.FragmentsAvailable;
import com.XECUREVoIP.chat.ChatFragment;
import com.XECUREVoIP.chat.ChatUtils.XecureChatMessage;
import com.XECUREVoIP.contacts.ContactsManager;
import com.XECUREVoIP.KeepAliveReceiver;
import com.XECUREVoIP.R;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecureContact;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.XecurePreferences;
import com.XECUREVoIP.compatibility.Compatibility;

import org.jivesoftware.smack.ReconnectionManager;
import org.jxmpp.jid.impl.LocalAndDomainpartJid;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallLog.CallStatus;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.GlobalState;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.Version;

import com.XECUREVoIP.security.LoginWithPassActivity;
import com.XECUREVoIP.security.LoginWithPatternActivity;
import com.XECUREVoIP.ui.LinphoneOverlay;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.WindowManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Linphone service, reacting to Incoming calls, ...<br />
 *
 * Roles include:<ul>
 * <li>Initializing XecureManager</li>
 * <li>Starting C libLinphone through XecureManager</li>
 * <li>Reacting to XecureManager state changes</li>
 * <li>Delegating GUI state change actions to GUI listener</li>
 */
public final class XecureService extends Service {
	/* Listener needs to be implemented in the Service as it calls
	 * setLatestEventInfo and startActivity() which needs a context.
	 */
	public static final String START_LINPHONE_LOGS = " ==== Phone information dump ====";
	public static final int IC_LEVEL_ORANGE=0;
	/*private static final int IC_LEVEL_GREEN=1;
	private static final int IC_LEVEL_RED=2;*/
	//public static final int IC_LEVEL_OFFLINE=3;

	private static XecureService instance;

	private final static int NOTIF_ID=1;
	private final static int INCALL_NOTIF_ID=2;
	private final static int MESSAGE_NOTIF_ID=3;
	private final static int CUSTOM_NOTIF_ID=4;
	private final static int MISSED_NOTIF_ID=5;
	private final static int SAS_NOTIF_ID=6;

	private static String TAG_NOTIFICATION = "notification";

	private boolean bPatten;

	public AbstractXMPPConnection connection = null;

	public static boolean isReady() {
		return instance != null && instance.mTestDelayElapsed;
	}

	/**
	 * @throws RuntimeException service not instantiated
	 */
	public static XecureService instance()  {
		if (isReady()) return instance;

		throw new RuntimeException("XecureService not instantiated yet");
	}

	public Handler mHandler = new Handler();

	//	private boolean mTestDelayElapsed; // add a timer for testing
	private boolean mTestDelayElapsed = true; // no timer
	private NotificationManager mNM;

	private Notification mNotif;
	private Notification mIncallNotif;
	private Notification mMsgNotif;
	private Notification mCustomNotif;
	private Notification mSasNotif;
	private int mMsgNotifCount;
	private PendingIntent mNotifContentIntent;
	private String mNotificationTitle;
	private boolean mDisableRegistrationStatus;
	private LinphoneCoreListenerBase mListener;
	public static int notifcationsPriority = (Version.sdkAboveOrEqual(Version.API16_JELLY_BEAN_41) ? Notification.PRIORITY_MIN : 0);
	private WindowManager mWindowManager;
	private LinphoneOverlay mOverlay;
	private Application.ActivityLifecycleCallbacks activityCallbacks;
	private Handler chatHandler = null;
	private Handler chatlistHandler = null;

	public void setChatListNofity(Handler mChatListUpdate) {
		chatlistHandler = mChatListUpdate;
	}

	public void setChatHandler(Handler mReceiveMessage) {
		chatHandler = mReceiveMessage;
	}

	/*Believe me or not, but knowing the application visibility state on Android is a nightmare.
	After two days of hard work I ended with the following class, that does the job more or less reliabily.
	*/
	class ActivityMonitor implements Application.ActivityLifecycleCallbacks {
		private ArrayList<Activity> activities = new ArrayList<Activity>();
		private boolean mActive = false;
		private int mRunningActivities = 0;

		class InactivityChecker implements Runnable {
			private boolean isCanceled;

			public void cancel() {
				isCanceled = true;
			}

			@Override
			public void run() {
				synchronized(XecureService.this) {
					if (!isCanceled) {
						if (ActivityMonitor.this.mRunningActivities == 0 && mActive) {
							mActive = false;
							XecureService.this.onBackgroundMode();
						}
					}
				}
			}
		};

		private InactivityChecker mLastChecker;

		@Override
		public synchronized void onActivityCreated(Activity activity, Bundle savedInstanceState) {
			Log.i("Activity created:" + activity);
			if (!activities.contains(activity))
				activities.add(activity);
		}

		@Override
		public void onActivityStarted(Activity activity) {
			Log.i("Activity started:" + activity);
		}

		@Override
		public synchronized void onActivityResumed(Activity activity) {
			Log.i("Activity resumed:" + activity);
			if (activities.contains(activity)) {
				mRunningActivities++;
				Log.i("runningActivities=" + mRunningActivities);
				checkActivity();
			}

		}

		@Override
		public synchronized void onActivityPaused(Activity activity) {
			Log.i("Activity paused:" + activity);
			if (activities.contains(activity)) {
				mRunningActivities--;
				Log.i("runningActivities=" + mRunningActivities);
				checkActivity();
			}

		}

		@Override
		public void onActivityStopped(Activity activity) {
			Log.i("Activity stopped:" + activity);
		}

		@Override
		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

		}

		@Override
		public synchronized void onActivityDestroyed(Activity activity) {
			Log.i("Activity destroyed:" + activity);
			if (activities.contains(activity)) {
				activities.remove(activity);
			}
		}

		void startInactivityChecker() {
			if (mLastChecker != null) mLastChecker.cancel();
			XecureService.this.mHandler.postDelayed(
					(mLastChecker = new InactivityChecker()), 2000);
		}

		void checkActivity() {

			if (mRunningActivities == 0) {
				if (mActive) startInactivityChecker();
			} else if (mRunningActivities > 0) {
				if (!mActive) {
					mActive = true;
					XecureService.this.onForegroundMode();
				}
				if (mLastChecker != null) {
					mLastChecker.cancel();
					mLastChecker = null;
				}
			}
		}
	}

	protected void onBackgroundMode(){
		Log.i("App has entered background mode");
		if (XecurePreferences.instance() != null && XecurePreferences.instance().isFriendlistsubscriptionEnabled()) {
			if (XecureManager.isInstanciated())
				XecureManager.getInstance().subscribeFriendList(false);
		}
	}

	protected void onForegroundMode() {
		Log.i("App has left background mode");
		if (XecurePreferences.instance() != null && XecurePreferences.instance().isFriendlistsubscriptionEnabled()) {
			if (XecureManager.isInstanciated())
				XecureManager.getInstance().subscribeFriendList(true);
		}
	}

	private void setupActivityMonitor(){
		if (activityCallbacks != null) return;
		getApplication().registerActivityLifecycleCallbacks(activityCallbacks = new ActivityMonitor());
	}

	public int getMessageNotifCount() {
		return mMsgNotifCount;
	}

	public void resetMessageNotifCount() {
		mMsgNotifCount = 0;
	}

	public boolean displayServiceNotification() {
		return XecurePreferences.instance().getServiceNotificationVisibility();
	}

	public void showServiceNotification() {
		startForegroundCompat(NOTIF_ID, mNotif);

		LinphoneCore lc = XecureManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null) return;
		LinphoneProxyConfig lpc = lc.getDefaultProxyConfig();
		if (lpc != null) {
			if (lpc.isRegistered()) {
				sendNotification(IC_LEVEL_ORANGE, R.string.notification_registered);
			} else {
				sendNotification(IC_LEVEL_ORANGE, R.string.notification_register_failure);
			}
		} else {
			sendNotification(IC_LEVEL_ORANGE, R.string.notification_started);
		}
	}

	public void hideServiceNotification() {
		stopForegroundCompat(NOTIF_ID);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		connectOpenfire();

		bPatten = isPattern();
//		incomingReceivedActivity = bPatten ?
//				LoginWithPatternActivity.class : LoginWithPassActivity.class;
		incomingReceivedActivity = XecureActivity.class;

		setupActivityMonitor();
		// In case restart after a crash. Main in XecureActivity
		mNotificationTitle = getString(R.string.service_name);

		// Needed in order for the two next calls to succeed, libraries must have been loaded first
		XecurePreferences.instance().setContext(getBaseContext());
		LinphoneCoreFactory.instance().setLogCollectionPath(getFilesDir().getAbsolutePath());
		boolean isDebugEnabled = XecurePreferences.instance().isDebugEnabled();
		LinphoneCoreFactory.instance().enableLogCollection(isDebugEnabled);
		LinphoneCoreFactory.instance().setDebugMode(isDebugEnabled, getString(R.string.app_name));

		// Dump some debugging information to the logs
		Log.i(START_LINPHONE_LOGS);
		dumpDeviceInformation();
		dumpInstalledLinphoneInformation();

		//Disable service notification for Android O
		if ((Version.sdkAboveOrEqual(Version.API26_O_80))) {
			XecurePreferences.instance().setServiceNotificationVisibility(false);
			mDisableRegistrationStatus = true;
		}

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNM.cancel(INCALL_NOTIF_ID); // in case of crash the icon is not removed
		Compatibility.CreateChannel(this);

		Intent notifIntent = new Intent(this, incomingReceivedActivity);
		notifIntent.putExtra("Notification", true);
		mNotifContentIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Bitmap bm = null;
		try {
			bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
		} catch (Exception e) {
		}
		mNotif = Compatibility.createNotification(this, mNotificationTitle, "", R.drawable.linphone_notification_icon, R.mipmap.ic_launcher, bm, mNotifContentIntent, true,notifcationsPriority);

		XecureManager.createAndStart(XecureService.this);

		instance = this; // instance is ready once linphone manager has been created
		incomingReceivedActivityName = XecurePreferences.instance().getActivityToLaunchOnIncomingReceived();
		try {
			incomingReceivedActivity = (Class<? extends Activity>) Class.forName(incomingReceivedActivityName);
		} catch (ClassNotFoundException e) {
			Log.e(e);
		}

		XecureManager.getLc().addListener(mListener = new LinphoneCoreListenerBase() {
			@Override
			public void callState(LinphoneCore lc, LinphoneCall call, State state, String message) {
				if (instance == null) {
					Log.i("Service not ready, discarding call state change to ",state.toString());
					return;
				}

				if (state == State.IncomingReceived) {
					if(! XecureManager.getInstance().getCallGsmON())
						onIncomingReceived();
				}

				if (state == State.CallEnd || state == State.CallReleased || state == State.Error) {
					Log.i(TAG_NOTIFICATION, "CallEnd || CallReleased || Error");
					if (XecureManager.isInstanciated() && XecureManager.getLc() != null && XecureManager.getLc().getCallsNb() == 0) {
						if (XecureActivity.isInstanciated() && XecureActivity.instance().getStatusFragment() != null) {
							removeSasNotification();
							XecureActivity.instance().getStatusFragment().setisZrtpAsk(false);
						}
					}
					destroyOverlay();
				}

				if (state == State.CallEnd && call.getCallLog().getStatus() == CallStatus.Missed) {
					Log.i(TAG_NOTIFICATION, "CallEnd || Missed");
					int missedCallCount = XecureManager.getLcIfManagerNotDestroyedOrNull().getMissedCallsCount();
					String body;
					if (missedCallCount > 1) {
						body = getString(R.string.missed_calls_notif_body).replace("%i", String.valueOf(missedCallCount));
					} else {
						LinphoneAddress address = call.getRemoteAddress();
						XecureContact c = ContactsManager.getInstance().findContactFromAddress(address);
						if (c != null) {
							body = c.getFullName();
						} else {
							body = address.getDisplayName();
							if (body == null) {
								body = address.asStringUriOnly();
							}
						}
					}

					Intent missedCallNotifIntent = new Intent(XecureService.this, incomingReceivedActivity);
					missedCallNotifIntent.putExtra("GoToHistory", true);
					PendingIntent intent = PendingIntent.getActivity(XecureService.this, 0, missedCallNotifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
					Notification notif = Compatibility.createMissedCallNotification(instance, getString(R.string.missed_calls_notif_title), body, intent);
					notifyWrapper(MISSED_NOTIF_ID, notif);
				}

				if (state == State.StreamsRunning) {
					// Workaround bug current call seems to be updated after state changed to streams running
					if (getResources().getBoolean(R.bool.enable_call_notification))
						refreshIncallIcon(call);
				} else {
					if (getResources().getBoolean(R.bool.enable_call_notification))
						refreshIncallIcon(XecureManager.getLc().getCurrentCall());
				}
			}

			@Override
			public void globalState(LinphoneCore lc,GlobalState state, String message) {
				if (!mDisableRegistrationStatus && state == GlobalState.GlobalOn && displayServiceNotification()) {
					sendNotification(IC_LEVEL_ORANGE, R.string.notification_started);
				}
			}

			@Override
			public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg, RegistrationState state, String smessage) {
//				if (instance == null) {
//					Log.i("Service not ready, discarding registration state change to ",state.toString());
//					return;
//				}
				if (!mDisableRegistrationStatus) {
					if (displayServiceNotification() && state == RegistrationState.RegistrationOk && XecureManager.getLc().getDefaultProxyConfig() != null && XecureManager.getLc().getDefaultProxyConfig().isRegistered()) {
						sendNotification(IC_LEVEL_ORANGE, R.string.notification_registered);
					}

					if (displayServiceNotification() && (state == RegistrationState.RegistrationFailed || state == RegistrationState.RegistrationCleared) && (XecureManager.getLc().getDefaultProxyConfig() == null || !XecureManager.getLc().getDefaultProxyConfig().isRegistered())) {
						sendNotification(IC_LEVEL_ORANGE, R.string.notification_register_failure);
					}

					if (displayServiceNotification() && state == RegistrationState.RegistrationNone) {
						sendNotification(IC_LEVEL_ORANGE, R.string.notification_started);
					}
				}
			}
		});


		try {
			mStartForeground = getClass().getMethod("startForeground", mStartFgSign);
			mStopForeground = getClass().getMethod("stopForeground", mStopFgSign);
		} catch (NoSuchMethodException e) {
			Log.e(e, "Couldn't find startForeground or stopForeground");
		}

		if (!Version.sdkAboveOrEqual(Version.API26_O_80)
				|| (ContactsManager.getInstance() != null && ContactsManager.getInstance().hasContactsAccess())) {
			getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, ContactsManager.getInstance());
		}

		if (displayServiceNotification()) {
			startForegroundCompat(NOTIF_ID, mNotif);
		}

		if (!mTestDelayElapsed) {
			// Only used when testing. Simulates a 5 seconds delay for launching service
			mHandler.postDelayed(new Runnable() {
				@Override public void run() {
					mTestDelayElapsed = true;
				}
			}, 5000);
		}

		//make sure the application will at least wakes up every 10 mn
		Intent intent = new Intent(this, KeepAliveReceiver.class);
		PendingIntent keepAlivePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		AlarmManager alarmManager = ((AlarmManager) this.getSystemService(Context.ALARM_SERVICE));
		Compatibility.scheduleAlarm(alarmManager, AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 600000, keepAlivePendingIntent);

		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
	}

	public void createOverlay() {
		if (mOverlay != null) destroyOverlay();

		LinphoneCall call = XecureManager.getLc().getCurrentCall();
		if (call == null || !call.getCurrentParams().getVideoEnabled()) return;

		mOverlay = new LinphoneOverlay(this);
		WindowManager.LayoutParams params = mOverlay.getWindowManagerLayoutParams();
		params.x = 0;
		params.y = 0;
		mWindowManager.addView(mOverlay, params);
	}

	public void destroyOverlay() {
		if (mOverlay != null) {
			mWindowManager.removeViewImmediate(mOverlay);
			mOverlay.destroy();
		}
		mOverlay = null;
	}

	private enum IncallIconState {INCALL, PAUSE, VIDEO, IDLE}
	private IncallIconState mCurrentIncallIconState = IncallIconState.IDLE;
	private synchronized void setIncallIcon(IncallIconState state) {
		if (state == mCurrentIncallIconState) return;
		mCurrentIncallIconState = state;

		int notificationTextId = 0;
		int inconId = 0;

		switch (state) {
			case IDLE:
				if (!displayServiceNotification()) {
					stopForegroundCompat(INCALL_NOTIF_ID);
				} else {
					mNM.cancel(INCALL_NOTIF_ID);
				}
				return;
			case INCALL:
				inconId = R.drawable.topbar_call_notification;
				notificationTextId = R.string.incall_notif_active;
				break;
			case PAUSE:
				inconId = R.drawable.topbar_call_notification;
				notificationTextId = R.string.incall_notif_paused;
				break;
			case VIDEO:
				inconId = R.drawable.topbar_videocall_notification;
				notificationTextId = R.string.incall_notif_video;
				break;
			default:
				throw new IllegalArgumentException("Unknown state " + state);
		}

		if (XecureManager.getLc().getCallsNb() == 0) {
			return;
		}

		LinphoneCall call = XecureManager.getLc().getCalls()[0];
		String userName = call.getRemoteAddress().getUserName();
		String domain = call.getRemoteAddress().getDomain();
		String displayName = call.getRemoteAddress().getDisplayName();
		LinphoneAddress address = LinphoneCoreFactory.instance().createLinphoneAddress(userName,domain,null);
		address.setDisplayName(displayName);

		XecureContact contact = ContactsManager.getInstance().findContactFromAddress(address);
		Uri pictureUri = contact != null ? contact.getPhotoUri() : null;
		Bitmap bm = null;
		try {
			bm = MediaStore.Images.Media.getBitmap(getContentResolver(), pictureUri);
		} catch (Exception e) {
			bm = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
		}
		String name = address.getDisplayName() == null ? address.getUserName() : address.getDisplayName();
		Intent notifIntent = new Intent(this, incomingReceivedActivity);
		notifIntent.putExtra("Notification", true);
		mNotifContentIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mIncallNotif = Compatibility.createInCallNotification(getApplicationContext(), mNotificationTitle, getString(notificationTextId), inconId, bm, name, mNotifContentIntent);

		if (!displayServiceNotification()) {
			startForegroundCompat(INCALL_NOTIF_ID, mIncallNotif);
		} else {
			notifyWrapper(INCALL_NOTIF_ID, mIncallNotif);
		}
	}

	public void refreshIncallIcon(LinphoneCall currentCall) {
		LinphoneCore lc = XecureManager.getLc();
		if (currentCall != null) {
			if (currentCall.getCurrentParams().getVideoEnabled() && currentCall.cameraEnabled()) {
				// checking first current params is mandatory
				setIncallIcon(IncallIconState.VIDEO);
			} else {
				setIncallIcon(IncallIconState.INCALL);
			}
		} else if (lc.getCallsNb() == 0) {
			setIncallIcon(IncallIconState.IDLE);
		}  else if (lc.isInConference()) {
			setIncallIcon(IncallIconState.INCALL);
		} else {
			setIncallIcon(IncallIconState.PAUSE);
		}
	}

	@Deprecated
	public void addNotification(Intent onClickIntent, int iconResourceID, String title, String message) {
		addCustomNotification(onClickIntent, iconResourceID, title, message, true);
	}

	public void addCustomNotification(Intent onClickIntent, int iconResourceID, String title, String message, boolean isOngoingEvent) {
		PendingIntent notifContentIntent = PendingIntent.getActivity(this, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Bitmap bm = null;
		try {
			bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
		} catch (Exception e) {
		}
		mCustomNotif = Compatibility.createNotification(this, title, message, iconResourceID, 0, bm, notifContentIntent, isOngoingEvent,notifcationsPriority);

		mCustomNotif.defaults |= Notification.DEFAULT_VIBRATE;
		mCustomNotif.defaults |= Notification.DEFAULT_SOUND;
		mCustomNotif.defaults |= Notification.DEFAULT_LIGHTS;

		notifyWrapper(CUSTOM_NOTIF_ID, mCustomNotif);
	}

	public void removeCustomNotification() {
		mNM.cancel(CUSTOM_NOTIF_ID);
		resetIntentLaunchedOnNotificationClick();
	}

	public void displayMessageNotification(String to, String fromSipUri, String fromName, String message) {
		Intent notifIntent = bPatten ?
				new Intent(this, LoginWithPatternActivity.class) :
				new Intent(this, LoginWithPassActivity.class);
		notifIntent.putExtra("GoToChat", true);
		notifIntent.putExtra("ChatContactSipUri", fromSipUri);

		PendingIntent notifContentIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (fromName == null) {
			fromName = fromSipUri;
		}

		if (mMsgNotif == null) {
			mMsgNotifCount = 1;
		} else {
			mMsgNotifCount++;
		}

		Uri pictureUri = null;
		try {
			XecureContact contact = ContactsManager.getInstance().findContactFromAddress(LinphoneCoreFactory.instance().createLinphoneAddress(fromSipUri));
			if (contact != null)
				pictureUri = contact.getThumbnailUri();
		} catch (LinphoneCoreException e1) {
			Log.e("Cannot parse from address ", e1);
		}

		Bitmap bm = null;
		if (pictureUri != null) {
			try {
				bm = MediaStore.Images.Media.getBitmap(getContentResolver(), pictureUri);
			} catch (Exception e) {
				bm = BitmapFactory.decodeResource(getResources(), R.drawable.topbar_avatar);
			}
		} else {
			bm = BitmapFactory.decodeResource(getResources(), R.drawable.topbar_avatar);
		}
		mMsgNotif = Compatibility.createMessageNotification(getApplicationContext(), mMsgNotifCount, to, fromName, message, bm, notifContentIntent);

		notifyWrapper(MESSAGE_NOTIF_ID, mMsgNotif);
	}

	public void displayInappNotification(String message) {
		Intent notifIntent = bPatten ?
				new Intent(this, LoginWithPatternActivity.class) :
				new Intent(this, LoginWithPassActivity.class);
		notifIntent.putExtra("GoToInapp", true);

		PendingIntent notifContentIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mNotif = Compatibility.createSimpleNotification(getApplicationContext(), getString(R.string.inapp_notification_title), message, notifContentIntent);

		notifyWrapper(NOTIF_ID, mNotif);
	}

	public void removeMessageNotification() {
		mNM.cancel(MESSAGE_NOTIF_ID);
		resetIntentLaunchedOnNotificationClick();
	}

	public void displaySasNotification(String sas) {
		mSasNotif = Compatibility.createSimpleNotification(getApplicationContext(),
				getString(R.string.zrtp_notification_title),
				sas + " " + getString(R.string.zrtp_notification_message),
				null);

		notifyWrapper(SAS_NOTIF_ID, mSasNotif);
	}

	public void removeSasNotification() {
		mNM.cancel(SAS_NOTIF_ID);
	}

	private static final Class<?>[] mSetFgSign = new Class[] {boolean.class};
	private static final Class<?>[] mStartFgSign = new Class[] {
			int.class, Notification.class};
	private static final Class<?>[] mStopFgSign = new Class[] {boolean.class};

	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private String incomingReceivedActivityName;
	private Class<? extends Activity> incomingReceivedActivity;

	void invokeMethod(Method method, Object[] args) {
		try {
			method.invoke(this, args);
		} catch (InvocationTargetException e) {
			// Should not happen.
			Log.w(e, "Unable to invoke method");
		} catch (IllegalAccessException e) {
			// Should not happen.
			Log.w(e, "Unable to invoke method");
		}
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			invokeMethod(mStartForeground, mStartForegroundArgs);
			return;
		}

		// Fall back on the old API.
		if (mSetForeground != null) {
			mSetForegroundArgs[0] = Boolean.TRUE;
			invokeMethod(mSetForeground, mSetForegroundArgs);
			// continue
		}

		notifyWrapper(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			invokeMethod(mStopForeground, mStopForegroundArgs);
			return;
		}

		// Fall back on the old API.  Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		mNM.cancel(id);
		if (mSetForeground != null) {
			mSetForegroundArgs[0] = Boolean.FALSE;
			invokeMethod(mSetForeground, mSetForegroundArgs);
		}
	}

	private void dumpDeviceInformation() {
		StringBuilder sb = new StringBuilder();
		sb.append("DEVICE=").append(Build.DEVICE).append("\n");
		sb.append("MODEL=").append(Build.MODEL).append("\n");
		sb.append("MANUFACTURER=").append(Build.MANUFACTURER).append("\n");
		sb.append("SDK=").append(Build.VERSION.SDK_INT).append("\n");
		sb.append("Supported ABIs=");
		for (String abi : Version.getCpuAbis()) {
			sb.append(abi + ", ");
		}
		sb.append("\n");
		Log.i(sb.toString());
	}

	private void dumpInstalledLinphoneInformation() {
		PackageInfo info = null;
		try {
			info = getPackageManager().getPackageInfo(getPackageName(),0);
		} catch (NameNotFoundException nnfe) {}

		if (info != null) {
			Log.i("Linphone version is ", info.versionName + " (" + info.versionCode + ")");
		} else {
			Log.i("Linphone version is unknown");
		}
	}

	private synchronized void sendNotification(int level, int textId) {
		String text = getString(textId);
		if (text.contains("%s") && XecureManager.getLc() != null) {
			// Test for null lc is to avoid a NPE when Android mess up badly with the String resources.
			LinphoneProxyConfig lpc = XecureManager.getLc().getDefaultProxyConfig();
			String id = lpc != null ? lpc.getIdentity() : "";
			text = String.format(text, id);
		}

		Bitmap bm = null;
		try {
			bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
		} catch (Exception e) {
		}
		String key = "@xecu.re:8161";
		if (text.contains(key)){
			text = text.substring(text.indexOf(key) + key.length());
		}
		mNotif = Compatibility.createNotification(this, mNotificationTitle, text, R.drawable.status_level, 0, bm, mNotifContentIntent, true,notifcationsPriority);
		notifyWrapper(NOTIF_ID, mNotif);
	}

	/**
	 * Wrap notifier to avoid setting the linphone icons while the service
	 * is stopping. When the (rare) bug is triggered, the linphone icon is
	 * present despite the service is not running. To trigger it one could
	 * stop linphone as soon as it is started. Transport configured with TLS.
	 */
	private synchronized void notifyWrapper(int id, Notification notification) {
		if (instance != null && notification != null) {
			mNM.notify(id, notification);
		} else {
			Log.i("Service not ready, discarding notification");
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		if (getResources().getBoolean(R.bool.kill_service_with_task_manager)) {
			Log.d("Task removed, stop service");

			// If push is enabled, don't unregister account, otherwise do unregister
			if (XecurePreferences.instance().isPushNotificationEnabled()) {
				LinphoneCore lc = XecureManager.getLcIfManagerNotDestroyedOrNull();
				if (lc != null) lc.setNetworkReachable(false);
			}
			stopSelf();
		}
		Intent restartService = new Intent(getApplicationContext(),
				this.getClass());
		restartService.setPackage(getPackageName());

		PendingIntent restartServicePI = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
		alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +500, restartServicePI);

		super.onTaskRemoved(rootIntent);
	}

	@Override
	public synchronized void onDestroy() {

		if (activityCallbacks != null){
			getApplication().unregisterActivityLifecycleCallbacks(activityCallbacks);
			activityCallbacks = null;
		}

		destroyOverlay();
		LinphoneCore lc = XecureManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.removeListener(mListener);
		}

		instance = null;
		XecureManager.destroy();

		// Make sure our notification is gone.
		stopForegroundCompat(NOTIF_ID);
		mNM.cancel(INCALL_NOTIF_ID);
		mNM.cancel(MESSAGE_NOTIF_ID);

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	public void setActivityToLaunchOnIncomingReceived(String activityName) {
		try {
			incomingReceivedActivity = (Class<? extends Activity>) Class.forName(activityName);
			incomingReceivedActivityName = activityName;
			XecurePreferences.instance().setActivityToLaunchOnIncomingReceived(incomingReceivedActivityName);
		} catch (ClassNotFoundException e) {
			Log.e(e);
		}
		resetIntentLaunchedOnNotificationClick();
	}

	private void resetIntentLaunchedOnNotificationClick() {
		Intent notifIntent = new Intent(this, incomingReceivedActivity);
		mNotifContentIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		/*if (mNotif != null) {
			mNotif.contentIntent = mNotifContentIntent;
		}
		notifyWrapper(NOTIF_ID, mNotif);*/
	}

	protected void onIncomingReceived() {
		//wakeup linphone
		startActivity(new Intent()
				.setClass(this, incomingReceivedActivity)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}


	public void tryingNewOutgoingCallButAlreadyInCall() {
	}

	public void tryingNewOutgoingCallButCannotGetCallParameters() {
	}

	public void tryingNewOutgoingCallButWrongDestinationAddress() {
	}

	public void onCallEncryptionChanged(final LinphoneCall call, final boolean encrypted,
										final String authenticationToken) {
	}

	private boolean isPattern() {
		SharedPreferences preXecue = getApplicationContext().getSharedPreferences("Xecure", MODE_PRIVATE);
		String strCrrEncryptionPass = preXecue.getString("pass", "");

		return strCrrEncryptionPass.isEmpty()? true: false;
	}

	public void connectOpenfire(){
		// Create the configuration for this new connection

		//this function or code given in official documention give an error in openfire run locally to solve this error
		//first off firewall
		//then follow my steps

		new Thread(){
			@Override
			public void run() {

				HostnameVerifier verifier = new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return false;
					}
				};
				DomainBareJid serviceName = null;
				try {
					serviceName = JidCreate.domainBareFrom("chat.xecu.re");
				} catch (XmppStringprepException e) {
					e.printStackTrace();
				}
				SharedPreferences pres = getApplication().getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
				final String username = pres.getString("username", "");
				String password = pres.getString("password", "");
				if(username == "")
					return;
				XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()

						.setUsernameAndPassword(username, password)
						.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
						.setXmppDomain(serviceName)
						.setHostnameVerifier(verifier)
						.enableDefaultDebugger()
						.setPort(9090)
						.build();
				connection = new XMPPTCPConnection(config);


				try {
					connection.connect();
					// all these proceedure also thrown error if you does not seperate this thread now we seperate thread create
					connection.login();

					ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(connection);
					reconnectionManager.setEnabledPerDefault(true);
					reconnectionManager.enableAutomaticReconnection();
					reconnectionManager.setFixedDelay(15);

					if(connection.isAuthenticated() && connection.isConnected()){
						ChatManager chatManager = ChatManager.getInstanceFor(connection);
						chatManager.addIncomingListener(new IncomingChatMessageListener() {
							@Override
							public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
								if (message.getBody().compareTo("") == 0){
									XecureManager.getInstance().receivePulicKey(from.getLocalpart().toString(), message.getSubject());
								} else {
									XecureChatMessage chatMessage = new XecureChatMessage(message.getBody(), false);
									XecureManager.getInstance().add(from.getLocalpart().toString(), chatMessage, chat, message.getSubject());
									String fromId = from.getLocalpart().toString();
									displayMessageNotification(username, fromId, fromId, chatMessage.getBody());
									try {
										if (XecureActivity.isInstanciated()){
											Bundle bundle = new Bundle();
											bundle.putString("idFrom", fromId);
											android.os.Message msg = new android.os.Message();
											msg.setData(bundle);
											msg.obj = chatMessage;
											if (XecureActivity.instance().getCurrentFragment().compareTo(FragmentsAvailable.CHAT) == 0 && chatHandler != null) {
												chatHandler.sendMessage(msg);
											}

											XecureActivity.instance().mNotifyReceiceMesage.sendEmptyMessage(0);
											if (chatlistHandler != null)
												chatlistHandler.sendEmptyMessage(0);
										}
									}catch (Exception e){
										e.printStackTrace();
									}
								}
							}
						});
						XecureManager.getInstance().setSmackConnection(connection);
					}
				} catch (SmackException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (XMPPException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} .start();

	}

}

