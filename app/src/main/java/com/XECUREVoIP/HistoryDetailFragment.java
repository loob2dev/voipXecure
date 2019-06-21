package com.XECUREVoIP;

/*
HistoryDetailFragment.java
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
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.XECUREVoIP.contacts.ContactsManager;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.mediastream.Log;

public class HistoryDetailFragment extends Fragment implements OnClickListener {
	private ImageView dialBack, videocall, chat, addToContacts, goToContact, back;
	private View view;
	private ImageView contactPicture, callDirection;
	private TextView contactName, contactAddress, time, date;
	private String sipUri, displayName, pictureUri;
	private XecureContact contact;
	LinphoneAddress lAddress = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		sipUri = getArguments().getString("SipUri");
		if (sipUri.contains("@136.144.213.201:8161")){
			sipUri = sipUri.replace("@136.144.213.201", "@sipmanagement.xecu.re");
		}
		displayName = getArguments().getString("DisplayName");
		pictureUri = getArguments().getString("PictureUri");
		String status = getArguments().getString("CallStatus");
		String callTime = getArguments().getString("CallTime");
		String callDate = getArguments().getString("CallDate");

		view = inflater.inflate(R.layout.history_detail, container, false);

		dialBack = (ImageView) view.findViewById(R.id.call);
		dialBack.setOnClickListener(this);
		videocall = (ImageView) view.findViewById(R.id.contact_videocall);
		videocall.setOnClickListener(this);

		back = (ImageView) view.findViewById(R.id.back);
		if(getResources().getBoolean(R.bool.isTablet)){
			back.setVisibility(View.INVISIBLE);
		} else {
			back.setOnClickListener(this);
		}

		chat = (ImageView) view.findViewById(R.id.chat);
		chat.setOnClickListener(this);
		if (getResources().getBoolean(R.bool.disable_chat))
			view.findViewById(R.id.chat).setVisibility(View.GONE);

		addToContacts = (ImageView) view.findViewById(R.id.add_contact);
		addToContacts.setOnClickListener(this);

		goToContact = (ImageView) view.findViewById(R.id.goto_contact);
		goToContact.setOnClickListener(this);

		contactPicture = (ImageView) view.findViewById(R.id.contact_picture);

		contactName = (TextView) view.findViewById(R.id.contact_name);
		contactAddress = (TextView) view.findViewById(R.id.contact_address);

		callDirection = (ImageView) view.findViewById(R.id.direction);

		time = (TextView) view.findViewById(R.id.time);
		date = (TextView) view.findViewById(R.id.date);

		displayHistory(status, callTime, callDate);

		return view;
	}

	private void displayHistory(String status, String callTime, String callDate) {
		if (status.equals(getResources().getString(R.string.missed))) {
			callDirection.setImageResource(R.drawable.call_missed);
		} else if (status.equals(getResources().getString(R.string.incoming))) {
			callDirection.setImageResource(R.drawable.call_incoming);
		} else if (status.equals(getResources().getString(R.string.outgoing))) {
			callDirection.setImageResource(R.drawable.call_outgoing);
		}

		time.setText(callTime == null ? "" : callTime);
		Long longDate = Long.parseLong(callDate);
		date.setText(XecureUtils.timestampToHumanDate(getActivity(),longDate,getString(R.string.history_detail_date_format)));

		try {
			lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
		} catch (LinphoneCoreException e) {
			Log.e(e);
		}

		if (lAddress != null) {

			String strAdress = lAddress.asStringUriOnly().contains(":8161") ?
					lAddress.asStringUriOnly().replace("sip:", "NUMBER:").replace("@sipmanagement.xecu.re:8161", "")
					: lAddress.asStringUriOnly().replace("sip:", "NUMBER:").replace("@sipmanagement.xecu.re", "");
			contactAddress.setText(strAdress);
			contact = ContactsManager.getInstance().findContactFromAddress(lAddress);
			if (contact != null) {
				contactName.setText(contact.getFullName());
				XecureUtils.setImagePictureFromUri(view.getContext(),contactPicture,contact.getPhotoUri(),contact.getThumbnailUri());
				addToContacts.setVisibility(View.GONE);
				goToContact.setVisibility(View.VISIBLE);
			} else {
				contactName.setText(displayName == null ? XecureUtils.getAddressDisplayName(sipUri) : displayName);
				contactPicture.setImageBitmap(ContactsManager.getInstance().getDefaultAvatarBitmap());
				addToContacts.setVisibility(View.VISIBLE);
				goToContact.setVisibility(View.GONE);
			}
		} else {
			contactAddress.setText(sipUri);
			contactName.setText(displayName == null ? XecureUtils.getAddressDisplayName(sipUri) : displayName);
		}
	}

	public void changeDisplayedHistory(String sipUri, String displayName, String pictureUri, String status, String callTime, String callDate) {
		if (displayName == null ) {
			displayName = XecureUtils.getUsernameFromAddress(sipUri);
		}

		this.sipUri = sipUri;
		this.displayName = displayName;
		this.pictureUri = pictureUri;
		displayHistory(status, callTime, callDate);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (XecureActivity.isInstanciated()) {
			XecureActivity.instance().selectMenu(FragmentsAvailable.HISTORY_DETAIL);
			XecureActivity.instance().hideTabBar(false);
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();

		if (id == R.id.back) {
			getFragmentManager().popBackStackImmediate();
		} if (id == R.id.call || id == R.id.contact_videocall) {
			switch (id){
				case R.id.call:
					XecurePreferences.instance().setInitiateVideoCall(false);
					break;
				case R.id.contact_videocall:
					XecurePreferences.instance().setInitiateVideoCall(true);
			}
			XecureActivity.instance().setAddresGoToDialerAndCall(sipUri, displayName, pictureUri == null ? null : Uri.parse(pictureUri));
		} else if (id == R.id.chat) {
			String strAdress = lAddress.asStringUriOnly().contains(":8161") ?
					lAddress.asStringUriOnly().replace("sip:", "").replace("@sipmanagement.xecu.re:8161", "")
					: lAddress.asStringUriOnly().replace("sip:", "").replace("@sipmanagement.xecu.re", "");
			XecureActivity.instance().displayChat(strAdress, null, null);
		} else if (id == R.id.add_contact) {
			String uri = sipUri;
			LinphoneAddress addr = null;
			try {
				addr = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
				uri = addr.asStringUriOnly();
			} catch (LinphoneCoreException e) {
				Log.e(e);
			}
//			if (addr != null && addr.getDisplayName() != null)
//				XecureActivity.instance().displayContactsForEdition(addr.asStringUriOnly(), addr.getDisplayName());
//			else {
				uri = uri.replace("sip:", "").replace("@sipmanagement.xecu.re:8161", "");
				XecureActivity.instance().displayContactEditor(uri);
//			}
		} else if (id == R.id.goto_contact) {
			XecureActivity.instance().displayContact(contact, false);
		}
	}
}
