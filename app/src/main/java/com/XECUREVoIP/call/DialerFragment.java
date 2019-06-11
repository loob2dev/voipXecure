package com.XECUREVoIP.call;

/*
DialerFragment.java
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

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.XECUREVoIP.FragmentsAvailable;
import com.XECUREVoIP.R;
import com.XECUREVoIP.Service.XecureService;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.contacts.ContactsManager;
import com.XECUREVoIP.ui.AddressAware;
import com.XECUREVoIP.ui.AddressText;
import com.XECUREVoIP.ui.CallButton;
import com.XECUREVoIP.ui.EraseButton;
import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.Log;

public class DialerFragment extends Fragment {
	private static DialerFragment instance;
	private static boolean isCallTransferOngoing = false;

	private AddressAware numpad;
	private AddressText mAddress;
	private CallButton mCall;
	private ImageView mAddContact;
	private OnClickListener addContactListener, cancelListener, transferListener;
	private boolean shouldEmptyAddressField = true;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialer, container, false);

		mAddress = (AddressText) view.findViewById(R.id.address);
		mAddress.setDialerFragment(this);

		EraseButton erase = (EraseButton) view.findViewById(R.id.erase);
		erase.setAddressWidget(mAddress);

		mCall = (CallButton) view.findViewById(R.id.call);
		mCall.setAddressWidget(mAddress);
		if (XecureActivity.isInstanciated() && XecureManager.getLcIfManagerNotDestroyedOrNull() != null && XecureManager.getLcIfManagerNotDestroyedOrNull().getCallsNb() > 0) {
			if (isCallTransferOngoing) {
				mCall.setImageResource(R.drawable.call_transfer);
			} else {
				mCall.setImageResource(R.drawable.call_add);
			}
		} else {
			if (XecureManager.getLcIfManagerNotDestroyedOrNull() != null && XecureManager.getLcIfManagerNotDestroyedOrNull().getVideoAutoInitiatePolicy()) {
				mCall.setImageResource(R.drawable.call_video_start);
			} else {
				mCall.setImageResource(R.drawable.call_audio_start);
			}
		}

		numpad = (AddressAware) view.findViewById(R.id.numpad);
		if (numpad != null) {
			numpad.setAddressWidget(mAddress);
		}

		mAddContact = (ImageView) view.findViewById(R.id.add_contact);
		mAddContact.setEnabled(!(XecureActivity.isInstanciated() && XecureManager.getLcIfManagerNotDestroyedOrNull() != null && XecureManager.getLc().getCallsNb() > 0));

		addContactListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				XecureActivity.instance().displayContactEditor(mAddress.getText().toString());
			}
		};
		cancelListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				XecureActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
			}
		};
		transferListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinphoneCore lc = XecureManager.getLc();
				if (lc.getCurrentCall() == null) {
					return;
				}
				lc.transferCall(lc.getCurrentCall(), mAddress.getText().toString());
				isCallTransferOngoing = false;
				XecureActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
			}
		};

		resetLayout(isCallTransferOngoing);

		if (getArguments() != null) {
			shouldEmptyAddressField = false;
			String number = getArguments().getString("SipUri");
			String displayName = getArguments().getString("DisplayName");
			String photo = getArguments().getString("PhotoUri");
			mAddress.setText(number);
			if (displayName != null) {
				mAddress.setDisplayedName(displayName);
			}
			if (photo != null) {
				mAddress.setPictureUri(Uri.parse(photo));
			}
		}

		instance = this;

		return view;
    }

	/**
	 * @return null if not ready yet
	 */
	public static DialerFragment instance() {
		return instance;
	}

	@Override
	public void onPause() {
		instance = null;
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		instance = this;

		if (XecureActivity.isInstanciated()) {
			XecureActivity.instance().selectMenu(FragmentsAvailable.DIALER);
			XecureActivity.instance().updateDialerFragment(this);
			XecureActivity.instance().showStatusBar();
			XecureActivity.instance().hideTabBar(false);
		}

		boolean isOrientationLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		if(isOrientationLandscape && !getResources().getBoolean(R.bool.isTablet)) {
			((LinearLayout) numpad).setVisibility(View.GONE);
		} else {
			((LinearLayout) numpad).setVisibility(View.VISIBLE);
		}

		if (shouldEmptyAddressField) {
			mAddress.setText("");
		} else {
			shouldEmptyAddressField = true;
		}
		resetLayout(isCallTransferOngoing);

		String addressWaitingToBeCalled = XecureActivity.instance().mAddressWaitingToBeCalled;
		if (addressWaitingToBeCalled != null) {
			mAddress.setText(addressWaitingToBeCalled);
			if (getResources().getBoolean(R.bool.automatically_start_intercepted_outgoing_gsm_call)) {
				newOutgoingCall(addressWaitingToBeCalled);
			}
			XecureActivity.instance().mAddressWaitingToBeCalled = null;
		}
	}

	public void resetLayout(boolean callTransfer) {
		if (!XecureActivity.isInstanciated()) {
			return;
		}
		isCallTransferOngoing = XecureActivity.instance().isCallTransfer();
		LinphoneCore lc = XecureManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null) {
			return;
		}

		if (lc.getCallsNb() > 0) {
			if (isCallTransferOngoing) {
				mCall.setImageResource(R.drawable.call_transfer);
				mCall.setExternalClickListener(transferListener);
			} else {
				mCall.setImageResource(R.drawable.call_add);
				mCall.resetClickListener();
			}
			mAddContact.setEnabled(true);
			mAddContact.setImageResource(R.drawable.call_alt_back);
			mAddContact.setOnClickListener(cancelListener);
		} else {
			if (XecureManager.getLc().getVideoAutoInitiatePolicy()) {
				mCall.setImageResource(R.drawable.call_video_start);
			} else {
				mCall.setImageResource(R.drawable.call_audio_start);
			}
			mAddContact.setEnabled(false);
			mAddContact.setImageResource(R.drawable.contact_add_button);
			mAddContact.setOnClickListener(addContactListener);
			enableDisableAddContact();
		}
	}

	public void enableDisableAddContact() {
		mAddContact.setEnabled(XecureManager.getLcIfManagerNotDestroyedOrNull() != null && XecureManager.getLc().getCallsNb() > 0 || !mAddress.getText().toString().equals(""));
	}

	public void displayTextInAddressBar(String numberOrSipAddress) {
		shouldEmptyAddressField = false;
		mAddress.setText(numberOrSipAddress);
	}

	public void newOutgoingCall(String numberOrSipAddress) {
		displayTextInAddressBar(numberOrSipAddress);
		XecureManager.getInstance().newOutgoingCall(mAddress);
	}

	public void newOutgoingCall(Intent intent) {
		if (intent != null && intent.getData() != null) {
			String scheme = intent.getData().getScheme();
			if (scheme.startsWith("imto")) {
				mAddress.setText("sip:" + intent.getData().getLastPathSegment());
			} else if (scheme.startsWith("call") || scheme.startsWith("sip")) {
				mAddress.setText(intent.getData().getSchemeSpecificPart());
			} else {
				Uri contactUri = intent.getData();
				String address = ContactsManager.getAddressOrNumberForAndroidContact(XecureService.instance().getContentResolver(), contactUri);
				if(address != null) {
					mAddress.setText(address);
				} else {
					Log.e("Unknown scheme: ", scheme);
					mAddress.setText(intent.getData().getSchemeSpecificPart());
				}
			}

			mAddress.clearDisplayedName();
			intent.setData(null);

			XecureManager.getInstance().newOutgoingCall(mAddress);
		}
	}
}
