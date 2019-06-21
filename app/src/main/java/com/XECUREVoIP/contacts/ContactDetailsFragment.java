package com.XECUREVoIP.contacts;

/*
ContactDetailsFragment.java
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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;


import com.XECUREVoIP.FragmentsAvailable;
import com.XECUREVoIP.R;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecureContact;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.XecureNumberOrAddress;
import com.XECUREVoIP.XecurePreferences;
import com.XECUREVoIP.XecureUtils;

import org.linphone.core.LinphoneProxyConfig;
import org.w3c.dom.Text;

public class ContactDetailsFragment extends Fragment implements OnClickListener {
	private XecureContact contact;
	private ImageView editContact, deleteContact, back;
	private TextView company;
	private TextView department;
	private LayoutInflater inflater;
	private View view;
	private boolean displayChatAddressOnly = false;

	private OnClickListener dialListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int id = v.getId();
			if (XecureActivity.isInstanciated()) {
				switch (id){
					case R.id.contact_call:
						XecurePreferences.instance().setInitiateVideoCall(false);
						break;
					case R.id.contact_videocall:
						XecurePreferences.instance().setInitiateVideoCall(true);
				}
				String tag = (String)v.getTag();
				XecureActivity.instance().setAddresGoToDialerAndCall(tag, contact.getFullName(), contact.getPhotoUri());
			}
		}
	};

	private OnClickListener chatListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (XecureActivity.isInstanciated()) {
				String strAdress = v.getTag().toString().contains(":8161") ?
						v.getTag().toString().replace("sip:", "").replace("@sipmanagement.xecu.re:8161", "")
						: v.getTag().toString().replace("sip:", "").replace("@sipmanagement.xecu.re", "");
				XecureActivity.instance().displayChat(strAdress, null, null);
			}
		}
	};

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		contact = (XecureContact) getArguments().getSerializable("Contact");

		this.inflater = inflater;
		view = inflater.inflate(R.layout.contact, container, false);

		if (getArguments() != null) {
			displayChatAddressOnly = getArguments().getBoolean("ChatAddressOnly");
		}

		editContact = (ImageView) view.findViewById(R.id.editContact);
		editContact.setOnClickListener(this);

		deleteContact = (ImageView) view.findViewById(R.id.deleteContact);
		deleteContact.setOnClickListener(this);

		company = (TextView) view.findViewById(R.id.contactCompany);
		boolean isOrgVisible = getResources().getBoolean(R.bool.display_contact_organization);
		String cmp = contact.getCompany();
		if (cmp != null && !cmp.isEmpty() && isOrgVisible) {
			company.setText(cmp);
		} else {
			company.setVisibility(View.GONE);
		}
		department = (TextView) view.findViewById(R.id.contactDepartment);
		String dept = contact.getDepartment();
		if (department != null && !dept.isEmpty()){
			department.setText(dept);
		} else {
			department.setVisibility(View.GONE);
		}

		back = (ImageView) view.findViewById(R.id.back);
		if(getResources().getBoolean(R.bool.isTablet)){
			back.setVisibility(View.INVISIBLE);
		} else {
			back.setOnClickListener(this);
		}

		return view;
	}

	public void changeDisplayedContact(XecureContact newContact) {
		contact = newContact;
		displayContact(inflater, view);
	}

	@SuppressLint("InflateParams")
	private void displayContact(LayoutInflater inflater, View view) {
		ImageView contactPicture = (ImageView) view.findViewById(R.id.contact_picture);
		if (contact.hasPhoto()) {
			XecureUtils.setImagePictureFromUri(getActivity(), contactPicture, contact.getPhotoUri(), contact.getThumbnailUri());
        } else {
        	contactPicture.setImageBitmap(ContactsManager.getInstance().getDefaultAvatarBitmap());
        }

		TextView contactName = (TextView) view.findViewById(R.id.contact_name);
		contactName.setText(contact.getFullName());
		company.setText((contact.getCompany() != null) ? contact.getCompany() : "");
		String dept = contact.getDepartment();
		if (!dept.isEmpty()){
			department.setText(dept);
		}

		TableLayout controls = (TableLayout) view.findViewById(R.id.controls);
		controls.removeAllViews();
		for (XecureNumberOrAddress noa : contact.getNumbersOrAddresses()) {
			boolean skip = false;
			if (noa.isSIPAddress())
				continue;
			View v = inflater.inflate(R.layout.contact_control_row, null);

			String value = noa.getValue();
			String displayednumberOrAddress = XecureUtils.getDisplayableUsernameFromAddress(value);

			TextView label = (TextView) v.findViewById(R.id.address_label);
			if (noa.isSIPAddress()) {
				label.setText(R.string.sip_address);
				skip |= getResources().getBoolean(R.bool.hide_contact_sip_addresses);
			} else {
				label.setText(R.string.phone_number);
				skip |= getResources().getBoolean(R.bool.hide_contact_phone_numbers);
			}

			TextView tv = (TextView) v.findViewById(R.id.numeroOrAddress);
			tv.setText(displayednumberOrAddress);
			tv.setSelected(true);


			LinphoneProxyConfig lpc = XecureManager.getLc().getDefaultProxyConfig();
			if (lpc != null) {
				String username = lpc.normalizePhoneNumber(displayednumberOrAddress);
				value = XecureUtils.getFullAddressFromUsername(username);
			}

//			String contactAddress = contact.getPresenceModelForUri(noa.getValue());
			String contactAddress = null;
			if (contactAddress != null) {
				v.findViewById(R.id.friendLinphone).setVisibility(View.VISIBLE);
			}

			if (!displayChatAddressOnly) {
				v.findViewById(R.id.contact_call).setOnClickListener(dialListener);
				v.findViewById(R.id.contact_videocall).setOnClickListener(dialListener);
				if (contactAddress != null) {
					v.findViewById(R.id.contact_call).setTag(contactAddress);
					v.findViewById(R.id.contact_videocall).setTag(contactAddress);
				} else {
					v.findViewById(R.id.contact_call).setTag(value);
					v.findViewById(R.id.contact_videocall).setTag(value);
				}
			} else {
				v.findViewById(R.id.contact_call).setVisibility(View.GONE);
				v.findViewById(R.id.contact_videocall).setVisibility(View.GONE);
			}

			v.findViewById(R.id.contact_chat).setOnClickListener(chatListener);
			if (contactAddress != null) {
				v.findViewById(R.id.contact_chat).setTag(contactAddress);
			} else {
				v.findViewById(R.id.contact_chat).setTag(value);
			}

			if (getResources().getBoolean(R.bool.disable_chat)) {
				v.findViewById(R.id.contact_chat).setVisibility(View.GONE);
			}

			if (!skip) {
				controls.addView(v);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (XecureActivity.isInstanciated()) {
			XecureActivity.instance().selectMenu(FragmentsAvailable.CONTACT_DETAIL);
			XecureActivity.instance().hideTabBar(false);
		}
		displayContact(inflater, view);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();

		if (id == R.id.editContact) {
			XecureActivity.instance().editContact(contact);
		}
		if (id == R.id.deleteContact) {
			final Dialog dialog = XecureActivity.instance().displayDialog(getString(R.string.delete_text));
			Button delete = (Button) dialog.findViewById(R.id.delete_button);
			Button cancel = (Button) dialog.findViewById(R.id.cancel);

			delete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					ContactDBHelper dbHelper = new ContactDBHelper(getActivity());
					dbHelper.deleteData(contact.getId());
					XecureActivity.instance().displayContacts(false);
					dialog.dismiss();
					ContactsManager.getInstance().fetchContactsAsync();
				}
			});

			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					dialog.dismiss();

				}
			});
			dialog.show();
		}
		if (id == R.id.back) {
			getFragmentManager().popBackStackImmediate();
		}
	}
}
