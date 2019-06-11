package com.XECUREVoIP.contacts;

/*
 ContactEditorFragment.java
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
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.ContactsContract.DisplayPhoto;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.XECUREVoIP.R;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecureContact;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.XecureNumberOrAddress;
import com.XECUREVoIP.XecureUtils;

import org.linphone.mediastream.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactEditorFragment extends Fragment {
	private View view;
	private ImageView cancel, deleteContact, ok;
	private ImageView addNumber, addSipAddress, contactPicture;
	private LinearLayout phoneNumbersSection, sipAddressesSection;
	private EditText firstName, lastName, email, company, department, sub_department;
	private LayoutInflater inflater;

	private static final int ADD_PHOTO = 1337;
	private static final int PHOTO_SIZE = 128;

	private XecureContact contact;
	private List<XecureNumberOrAddress> numbersAndAddresses;
	private int firstSipAddressIndex = -1;
	private LinearLayout sipAddresses, numbers;
	private String newSipOrNumberToAdd, newDisplayName;
	private Uri pickedPhotoForContactUri;
	private byte[] photoToAdd;
	private boolean isNewContact;
    private int iAddres = 1;
    private int iNumber = 1;
    private final static int LIMIT_NUMBER = 2;

    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		this.inflater = inflater;
		view = inflater.inflate(R.layout.contact_edit, container, false);
		firstName = (EditText) view.findViewById(R.id.contactFirstName);
		lastName = (EditText) view.findViewById(R.id.contactLastName);
		email = (EditText) view.findViewById(R.id.contactEmail);


		isNewContact = true;

		if (getArguments() != null) {
			Serializable obj = getArguments().getSerializable("Contact");
			if (obj != null) {
				contact = (XecureContact) obj;
				isNewContact = false;
				if (getArguments().getString("NewSipAdress") != null) {
					newSipOrNumberToAdd = getArguments().getString("NewSipAdress");
				}if (getArguments().getString("NewDisplayName") != null) {
					newDisplayName = getArguments().getString("NewDisplayName");
				}
			} else if (getArguments().getString("NewSipAdress") != null) {
				newSipOrNumberToAdd = getArguments().getString("NewSipAdress");
				if (getArguments().getString("NewDisplayName") != null) {
					newDisplayName = getArguments().getString("NewDisplayName");
				}
			}
		}

		if (isNewContact){
			newSipOrNumberToAdd = getArguments().getString("NewSipAdress");
			if (getArguments().getString("NewDisplayName") != null) {
				newDisplayName = getArguments().getString("NewDisplayName");
			}
		}else {
			if (getArguments().getString("NewSipAdress") != null) {
				newSipOrNumberToAdd = getArguments().getString("NewSipAdress");
			}if (getArguments().getString("NewDisplayName") != null) {
				newDisplayName = getArguments().getString("NewDisplayName");
			}
		}

		phoneNumbersSection = (LinearLayout) view.findViewById(R.id.phone_numbers);
//		phoneNumbersSection.setVisibility(View.GONE);

		sipAddressesSection = (LinearLayout) view.findViewById(R.id.sip_addresses);
//		sipAddressesSection.setVisibility(View.GONE);


		deleteContact = (ImageView) view.findViewById(R.id.delete_contact);

		cancel = (ImageView) view.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getFragmentManager().popBackStackImmediate();
				}
			});

		ok = (ImageView) view.findViewById(R.id.ok);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean bValid = true;
				if (firstName.getText().toString().isEmpty()){
					firstName.setError("Type first name");
					bValid = false;
				}
				if (lastName.getText().toString().isEmpty()){
					lastName.setError("Type last name");
					bValid = false;
				}
				if (!email.getText().toString().isEmpty() && !isEmailValid(email.getText().toString())){
					email.setError("Type email");
					bValid = false;
				}
				if (isNewContact) {
					boolean areAllFielsEmpty = true;
					for (XecureNumberOrAddress nounoa : numbersAndAddresses) {
						if (nounoa.getValue() != null && !nounoa.getValue().equals("")) {
							areAllFielsEmpty = false;
							break;
						}
					}
					if (areAllFielsEmpty) {
						TextView error = view.findViewById(R.id.error);
						error.setVisibility(View.VISIBLE);
						bValid = false;
					}
					contact = new XecureContact();
				}
				if (company.getText().toString().isEmpty()){
					company.setError("Type company name");
					bValid = false;
				}
				if (!bValid)	return;
				contact.setFirstNameAndLastName(firstName.getText().toString(), lastName.getText().toString());
				contact.setEmail(email.getText().toString());
				if (photoToAdd != null) {
					contact.setPhoto(photoToAdd, getActivity());
				}
				for (XecureNumberOrAddress noa : numbersAndAddresses) {
					if (noa.isSIPAddress() && noa.getValue() != null) {
						noa.setValue(XecureUtils.getFullAddressFromUsername(noa.getValue()));
					}
					contact.addOrUpdateNumberOrAddress(noa);
				}
				contact.setCompany(company.getText().toString());
				contact.setDepartment(department.getText().toString());
				contact.setSubDepartment(sub_department.getText().toString());
				boolean result = false;
				if (isNewContact)
					result = contact.save(getActivity());
				else
					contact.setShare(false);
					result = contact.update(getActivity());
				if (result){
					getFragmentManager().popBackStackImmediate();
					ContactsManager.getInstance().fetchContactsAsync();
				}
				else{
					getActivity().deleteDatabase(ContactDBHelper.DATABASE_NAME);
                    Toast.makeText(getActivity(), "Failed. Try again.", Toast.LENGTH_SHORT).show();
                    getActivity().deleteDatabase(ContactDBHelper.DATABASE_NAME);
                }
			}
		});


		company = (EditText) view.findViewById(R.id.contactCompany);
		department = (EditText) view.findViewById(R.id.contactDepartment);
		sub_department = (EditText) view.findViewById(R.id.contactSubDepartment);
		boolean isOrgVisible = getResources().getBoolean(R.bool.display_contact_organization);
		if (!isOrgVisible) {
			company.setVisibility(View.GONE);
			view.findViewById(R.id.contactCompanyTitle).setVisibility(View.GONE);
		} else {
			if (!isNewContact) {
				company.setText(contact.getCompany());
				department.setText(contact.getDepartment());
				sub_department.setText(contact.getSubDepartment());
			}
		}

		if (!isNewContact) {
			String fn = contact.getFirstName();
			String ln = contact.getLastName();
			if (fn != null || ln != null) {
				firstName.setText(fn);
				lastName.setText(ln);
			} else {
				lastName.setText(contact.getFullName());
				firstName.setText("");
			}
			email.setText(contact.getEmail());

			deleteContact.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final Dialog dialog = XecureActivity.instance().displayDialog(getString(R.string.delete_text));
					Button delete = (Button) dialog.findViewById(R.id.delete_button);
					Button cancel = (Button) dialog.findViewById(R.id.cancel);

					delete.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							ContactDBHelper dbHelper = new ContactDBHelper(getActivity());
							dbHelper.deleteData(contact.getId());
							ContactsManager.getInstance().fetchContactsAsync();
							XecureActivity.instance().displayContacts(false);
							dialog.dismiss();
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
			});
		} else {
			deleteContact.setVisibility(View.INVISIBLE);
		}

		contactPicture = (ImageView) view.findViewById(R.id.contact_picture);
		if (contact != null) {
			XecureUtils.setImagePictureFromUri(getActivity(), contactPicture, contact.getPhotoUri(), contact.getPhotoUri());
		} else {
			contactPicture.setImageBitmap(ContactsManager.getInstance().getDefaultAvatarBitmap());
		}

		contactPicture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				pickImage();
				XecureActivity.instance().checkAndRequestCameraPermission();
			}
		});

		numbersAndAddresses = new ArrayList<XecureNumberOrAddress>();
//		sipAddresses = initSipAddressFields(contact);
		numbers = initNumbersFields(contact);

		addSipAddress = (ImageView) view.findViewById(R.id.add_address_field);
		if (getResources().getBoolean(R.bool.allow_only_one_sip_address)) {
			addSipAddress.setVisibility(View.GONE);
		}
		addSipAddress.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
			    if (iAddres == LIMIT_NUMBER) {
                    addSipAddress.setVisibility(View.GONE);
                    return;
                }
				addEmptyRowToAllowNewNumberOrAddress(sipAddresses,true);
                iAddres++;
			}
		});

		addNumber = (ImageView) view.findViewById(R.id.add_number_field);
		if (getResources().getBoolean(R.bool.allow_only_one_phone_number)) {
			addNumber.setVisibility(View.GONE);
		}
		addNumber.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
			    addEmptyRowToAllowNewNumberOrAddress(numbers,false);
				if (++iNumber == LIMIT_NUMBER){
					addNumber.setVisibility(View.GONE);
					return;
				}
			}
		});

//		lastName.requestFocus();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if(XecureActivity.isInstanciated()){
			XecureActivity.instance().hideTabBar(false);
		}

		// Force hide keyboard
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	}

	@Override
	public void onPause() {
		// Force hide keyboard
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		View view = getActivity().getCurrentFocus();
		if (imm != null && view != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}

		super.onPause();
	}

	private void pickImage() {
		pickedPhotoForContactUri = null;
		final List<Intent> cameraIntents = new ArrayList<Intent>();
		final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File file = new File(Environment.getExternalStorageDirectory(), getString(R.string.temp_photo_name));
		pickedPhotoForContactUri = Uri.fromFile(file);
		captureIntent.putExtra("outputX", PHOTO_SIZE);
		captureIntent.putExtra("outputY", PHOTO_SIZE);
		captureIntent.putExtra("aspectX", 0);
		captureIntent.putExtra("aspectY", 0);
		captureIntent.putExtra("scale", true);
		captureIntent.putExtra("return-data", false);
		captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, pickedPhotoForContactUri);
		cameraIntents.add(captureIntent);

		final Intent galleryIntent = new Intent();
		galleryIntent.setType("image/*");
		galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

		final Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.image_picker_title));
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

		startActivityForResult(chooserIntent, ADD_PHOTO);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ADD_PHOTO && resultCode == Activity.RESULT_OK) {
			if (data != null && data.getExtras() != null && data.getExtras().get("data") != null) {
				Bitmap bm = (Bitmap) data.getExtras().get("data");
				editContactPicture(null, bm);
			}
			else if (data != null && data.getData() != null) {
				Uri selectedImageUri = data.getData();
				try {
					Bitmap selectedImage = MediaStore.Images.Media.getBitmap(XecureManager.getInstance().getContext().getContentResolver(), selectedImageUri);
					selectedImage = Bitmap.createScaledBitmap(selectedImage, PHOTO_SIZE, PHOTO_SIZE, false);
					editContactPicture(null, selectedImage);
				} catch (IOException e) { Log.e(e); }
			}
			else if (pickedPhotoForContactUri != null) {
				String filePath = pickedPhotoForContactUri.getPath();
				editContactPicture(filePath, null);
			}
			else {
				File file = new File(Environment.getExternalStorageDirectory(), getString(R.string.temp_photo_name));
				if (file.exists()) {
					pickedPhotoForContactUri = Uri.fromFile(file);
					String filePath = pickedPhotoForContactUri.getPath();
					editContactPicture(filePath, null);
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void editContactPicture(String filePath, Bitmap image) {
		if (image == null) {
			image = BitmapFactory.decodeFile(filePath);
		}

		Bitmap scaledPhoto;
		/*int size = getThumbnailSize();
		if (size > 0) {
			scaledPhoto = Bitmap.createScaledBitmap(image, size, size, false);
		} else {
			scaledPhoto = Bitmap.createBitmap(image);
		}*/
		scaledPhoto = Bitmap.createScaledBitmap(image, 200, 200, false);
		image.recycle();

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		scaledPhoto.compress(Bitmap.CompressFormat.PNG , 0, stream);
		contactPicture.setImageBitmap(scaledPhoto);
		photoToAdd = stream.toByteArray();
	}

	private int getThumbnailSize() {
		int value = -1;
		Cursor c = XecureActivity.instance().getContentResolver().query(DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[] { DisplayPhoto.THUMBNAIL_MAX_DIM }, null, null, null);
		try {
			c.moveToFirst();
			value = c.getInt(0);
		} catch (Exception e) {
			Log.e(e);
		}
		return value;
	}

	private LinearLayout initNumbersFields(final XecureContact contact) {
		LinearLayout controls = (LinearLayout) view.findViewById(R.id.controls_numbers);
		controls.removeAllViews();

		if (contact != null) {
			for (XecureNumberOrAddress numberOrAddress : contact.getNumbersOrAddresses()) {
				if (!numberOrAddress.isSIPAddress()) {
					View view = displayNumberOrAddress(controls, numberOrAddress.getValue(), false);
					if (view != null)
						controls.addView(view);
				}
			}
		}

		if (newSipOrNumberToAdd != null) {
			boolean isSip = XecureUtils.isStrictSipAddress(newSipOrNumberToAdd) || !XecureUtils.isNumberAddress(newSipOrNumberToAdd);
			if(!isSip) {
				View view = displayNumberOrAddress(controls, newSipOrNumberToAdd, false);
				if (view != null)
					controls.addView(view);
			}
		}

		if (newDisplayName != null) {
			EditText lastNameEditText = (EditText) view.findViewById(R.id.contactLastName);
			if (view != null)
				lastNameEditText.setText(newDisplayName);
		}

		if (controls.getChildCount() == 0) {
			addEmptyRowToAllowNewNumberOrAddress(controls,false);
		}

		return controls;
	}

	private LinearLayout initSipAddressFields(final XecureContact contact) {
		LinearLayout controls = (LinearLayout) view.findViewById(R.id.controls_sip_address);
		controls.removeAllViews();

		if (contact != null) {
			for (XecureNumberOrAddress numberOrAddress : contact.getNumbersOrAddresses()) {
				if (numberOrAddress.isSIPAddress()) {
					View view = displayNumberOrAddress(controls, numberOrAddress.getValue(), true);
					if (view != null)
						controls.addView(view);
				}
			}
		}

		if (newSipOrNumberToAdd != null) {
			boolean isSip = XecureUtils.isStrictSipAddress(newSipOrNumberToAdd) || !XecureUtils.isNumberAddress(newSipOrNumberToAdd);
			if (isSip) {
				View view = displayNumberOrAddress(controls, newSipOrNumberToAdd, true);
				if (view != null)
					controls.addView(view);
			}
		}

		if (controls.getChildCount() == 0) {
			addEmptyRowToAllowNewNumberOrAddress(controls,true);
		}

		return controls;
	}

	private View displayNumberOrAddress(final LinearLayout controls, String numberOrAddress, boolean isSIP) {
		return displayNumberOrAddress(controls, numberOrAddress, isSIP, false);
	}

	@SuppressLint("InflateParams")
	private View displayNumberOrAddress(final LinearLayout controls, String numberOrAddress, boolean isSIP, boolean forceAddNumber) {
		String displayNumberOrAddress = numberOrAddress;
		if (isSIP) {
			if (firstSipAddressIndex == -1) {
				firstSipAddressIndex = controls.getChildCount();
			}
			displayNumberOrAddress = XecureUtils.getDisplayableUsernameFromAddress(numberOrAddress);
		}
		if ((getResources().getBoolean(R.bool.hide_phone_numbers_in_editor) && !isSIP) || (getResources().getBoolean(R.bool.hide_sip_addresses_in_editor) && isSIP)) {
			if (forceAddNumber)
				isSIP = !isSIP; // If number can't be displayed because we hide a sort of number, change that category
			else
				return null;
		}

		XecureNumberOrAddress tempNounoa;
		if (forceAddNumber) {
			tempNounoa = new XecureNumberOrAddress(null, isSIP);
		} else {
			if(isNewContact || newSipOrNumberToAdd != null) {
				tempNounoa = new XecureNumberOrAddress(numberOrAddress, isSIP);
			} else {
				tempNounoa = new XecureNumberOrAddress(null, isSIP, numberOrAddress);
			}
		}
		final XecureNumberOrAddress nounoa = tempNounoa;
		numbersAndAddresses.add(nounoa);

		final View view = inflater.inflate(R.layout.contact_edit_row, null);
		if (isSIP)
			view.findViewById(R.id.delete_field).setVisibility(View.GONE);

		final EditText noa = (EditText) view.findViewById(R.id.numoraddr);
		if (!isSIP) {
			noa.setInputType(InputType.TYPE_CLASS_PHONE);
		}
		noa.setText(displayNumberOrAddress);
		noa.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				nounoa.setValue(noa.getText().toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		if (forceAddNumber) {
			nounoa.setValue(noa.getText().toString());
		}

		ImageView delete = (ImageView) view.findViewById(R.id.delete_field);
		if ((getResources().getBoolean(R.bool.allow_only_one_phone_number) && !isSIP) || (getResources().getBoolean(R.bool.allow_only_one_sip_address) && isSIP)) {
			delete.setVisibility(View.GONE);
		}
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (contact != null) {
					contact.removeNumberOrAddress(nounoa);
				}
				numbersAndAddresses.remove(nounoa);
				view.setVisibility(View.GONE);
			}
		});
		return view;
	}

	@SuppressLint("InflateParams")
	private void addEmptyRowToAllowNewNumberOrAddress(final LinearLayout controls, final boolean isSip) {
		final View view = inflater.inflate(R.layout.contact_edit_row, null);
		if (isSip)
			view.findViewById(R.id.delete_field).setVisibility(View.GONE);
		final XecureNumberOrAddress nounoa = new XecureNumberOrAddress(null, isSip);

		final EditText noa = (EditText) view.findViewById(R.id.numoraddr);
		numbersAndAddresses.add(nounoa);
//		noa.setHint(isSip ? getString(R.string.sip_address) : getString(R.string.phone_number));
		if (!isSip) {
			noa.setInputType(InputType.TYPE_CLASS_PHONE);
		}
		noa.requestFocus();
		noa.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				nounoa.setValue(noa.getText().toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		final ImageView delete = (ImageView) view.findViewById(R.id.delete_field);
		if ((getResources().getBoolean(R.bool.allow_only_one_phone_number) && !isSip) || (getResources().getBoolean(R.bool.allow_only_one_sip_address) && isSip)) {
			delete.setVisibility(View.GONE);
		}
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				numbersAndAddresses.remove(nounoa);
				view.setVisibility(View.GONE);
				if (nounoa.isSIPAddress() && iAddres > 0 && --iAddres < LIMIT_NUMBER)
					addSipAddress.setVisibility(View.VISIBLE);
				else if (!nounoa.isSIPAddress() && iNumber > 0 && --iNumber < LIMIT_NUMBER)
					addNumber.setVisibility(View.VISIBLE);
			}
		});

		controls.addView(view);
	}

	public boolean isEmailValid(String email)
	{
		String regExpn =
				"^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
						+"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
						+"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
						+"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
						+"[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
						+"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

		CharSequence inputStr = email;

		Pattern pattern = Pattern.compile(regExpn,Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(inputStr);

		if(matcher.matches())
			return true;
		else
			return false;
	}
}