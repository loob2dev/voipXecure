package com.XECUREVoIP.account;
/*
AccountPreferencesFragment.java
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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.XECUREVoIP.FragmentsAvailable;
import com.XECUREVoIP.R;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecureManager;
import com.XECUREVoIP.XecurePreferences;
import com.XECUREVoIP.XecureUtils;
import com.XECUREVoIP.assistant.AssistantActivity;
import com.XECUREVoIP.ui.PreferencesListFragment;


import org.linphone.core.LinphoneAccountCreator;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneNatPolicy;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;

import java.util.ArrayList;
import java.util.List;

public class AccountPreferencesFragment extends PreferencesListFragment implements LinphoneAccountCreator.LinphoneAccountCreatorListener {
	private int n;
	private boolean isNewAccount=false;
	private XecurePreferences mPrefs;
	private EditTextPreference mProxyPreference;
	private ListPreference mTransportPreference;
	private XecurePreferences.AccountBuilder builder;
	private LinphoneAccountCreator accountCreator;
	private ProgressDialog progress;

	public AccountPreferencesFragment() {
		super(R.xml.account_preferences);
		mPrefs = XecurePreferences.instance();
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceScreen screen = getPreferenceScreen();
		n = getArguments().getInt("Account", 0);
		if(n == mPrefs.getAccountCount()) {
			isNewAccount = true;
			builder = new XecurePreferences.AccountBuilder(XecureManager.getLc());
		}
		initAccountPreferencesFields(screen);

		// Force hide keyboard
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		//remove preferences
		removePrefences();
	}

	private void removePrefences() {
		PreferenceCategory prtP = (PreferenceCategory)findPreference(getString(R.string.pref_advanced_key));
		Preference prfLink = findPreference(getString(R.string.pref_link_account_key));
		Preference prfICE = findPreference(getString(R.string.pref_ice_enable_key));
		Preference prfTransfer = findPreference(getString(R.string.pref_transport_key));
		EditTextPreference prfSTUN = (EditTextPreference)findPreference(getString(R.string.pref_stun_server_key));
		EditTextPreference prfPrefix = (EditTextPreference)findPreference(getString(R.string.pref_prefix_key));
		CheckBoxPreference prfAVPF = (CheckBoxPreference)findPreference(getString(R.string.pref_avpf_key));
		prtP.removePreference(prfLink);
		prtP.removePreference(prfICE);
		prtP.removePreference(prfTransfer);
		prtP.removePreference(prfSTUN);
		prtP.removePreference(prfPrefix);
		prtP.removePreference(prfAVPF);

		PreferenceCategory prtPM = (PreferenceCategory)findPreference(getString(R.string.pref_manage_key));
		Preference prfDelAccount = findPreference(getString(R.string.pref_del_account_key));
		prtPM.removePreference(prfDelAccount);

		PreferenceCategory pref_sipaccount_key = (PreferenceCategory)findPreference(getString(R.string.pref_sipaccount_key));
		Preference auth_userid = findPreference("auth_userid");
		Preference display_key = findPreference("display_key");
		pref_sipaccount_key.removePreference(auth_userid);
		pref_sipaccount_key.removePreference(display_key);

		PreferenceCategory pref_advanced_key = (PreferenceCategory)findPreference(getString(R.string.pref_advanced_key));
		Preference pref_expire_key = findPreference(getString(R.string.pref_expire_key));
		Preference pref_avpf_rr_interval_key = findPreference(getString(R.string.pref_avpf_rr_interval_key));
		Preference pref_escape_plus_key = findPreference(getString(R.string.pref_escape_plus_key));
		Preference pref_proxy_key = findPreference(getString(R.string.pref_proxy_key));
		pref_advanced_key.removePreference(pref_expire_key);
		pref_advanced_key.removePreference(pref_avpf_rr_interval_key);
		pref_advanced_key.removePreference(pref_escape_plus_key);
		pref_advanced_key.removePreference(pref_proxy_key);

		PreferenceScreen account_setting = getPreferenceScreen();
		PreferenceCategory pref_manage_key = (PreferenceCategory)findPreference(getString(R.string.pref_manage_key));
		account_setting.removePreference(pref_manage_key);
	}

	public static boolean isEditTextEmpty(String s) {
	      return s.equals("");  // really empty.
	}

	OnPreferenceChangeListener usernameChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if(isEditTextEmpty(newValue.toString())) return false;
			if (isNewAccount) {
				builder.setUsername(newValue.toString());
			} else {
				mPrefs.setAccountUsername(n, newValue.toString());
			}
			preference.setSummary(newValue.toString());
			return true;
		}
	};
	OnPreferenceChangeListener useridChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (isNewAccount) {
				builder.setUserId(newValue.toString());
			} else {
				mPrefs.setAccountUserId(n, newValue.toString());
			}
			preference.setSummary(newValue.toString());
			return true;
		}
	};
	OnPreferenceChangeListener passwordChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if(isEditTextEmpty(newValue.toString())) return false;
			if (isNewAccount) {
				builder.setPassword(newValue.toString());
			} else {
				mPrefs.setAccountPassword(n, newValue.toString());
			}
			return true;
		}
	};
	OnPreferenceChangeListener domainChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if(isEditTextEmpty(newValue.toString())) return false;
			if (isNewAccount) {
				builder.setDomain(newValue.toString());
			} else {
				mPrefs.setAccountDomain(n, newValue.toString());
				//change proxy
				String value = "sip:" + newValue.toString() + ":8161;transport=tls";
				mPrefs.setAccountProxy(n, value);
				preference.setSummary(mPrefs.getAccountProxy(n));

				if (mTransportPreference != null) {
					mTransportPreference.setSummary(mPrefs.getAccountTransportString(n));
					mTransportPreference.setValue(mPrefs.getAccountTransportKey(n));
				}
			}
			preference.setSummary(newValue.toString());
			return true;
		}
	};
	OnPreferenceChangeListener displayNameChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (isNewAccount) {
				builder.setDisplayName(newValue.toString());
			} else {
				mPrefs.setAccountDisplayName(n, newValue.toString());
			}
			preference.setSummary(newValue.toString());
			return true;
		}
	};
	OnPreferenceChangeListener proxyChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String value = newValue.toString();
			if (isNewAccount) {
				builder.setProxy(newValue.toString());
				preference.setSummary(newValue.toString());
			} else {
				mPrefs.setAccountProxy(n, value);
				preference.setSummary(mPrefs.getAccountProxy(n));

				if (mTransportPreference != null) {
					mTransportPreference.setSummary(mPrefs.getAccountTransportString(n));
					mTransportPreference.setValue(mPrefs.getAccountTransportKey(n));
				}
			}
			return true;
		}
	};
	OnPreferenceChangeListener outboundProxyChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (isNewAccount) {
				builder.setOutboundProxyEnabled((Boolean) newValue);
			} else {
				mPrefs.setAccountOutboundProxyEnabled(n, (Boolean) newValue);
			}
			return true;
		}
	};
	OnPreferenceChangeListener expiresChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (isNewAccount) {
				builder.setExpires(newValue.toString());
			} else {
				mPrefs.setExpires(n, newValue.toString());
			}
			preference.setSummary(newValue.toString());
			return true;
		}
	};
	OnPreferenceChangeListener prefixChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String value = newValue.toString();
			preference.setSummary(value);
			if (isNewAccount) {
				builder.setPrefix(value);
			} else {
				mPrefs.setPrefix(n, value);
			}
			return true;
		}
	};
	OnPreferenceChangeListener avpfChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			boolean value = (Boolean) newValue;
			if (isNewAccount) {
				builder.setAvpfEnabled(value);
			} else {
				mPrefs.enableAvpf(n, value);
			}
			return true;
		}
	};
	OnPreferenceChangeListener avpfRRIntervalChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String value = newValue.toString();
			try {
				int intValue = Integer.parseInt(value);
				if ((intValue < 1) || (intValue > 5)) {
					return false;
				}
			} catch (NumberFormatException nfe) { }
			if (isNewAccount) {
				//TODO
			} else {
				mPrefs.setAvpfRRInterval(n, value);
			}
			preference.setSummary(value);
			return true;
		}
	};
	OnPreferenceChangeListener escapeChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			boolean value = (Boolean) newValue;
			if (isNewAccount) {
				//TODO
			} else {
				mPrefs.setReplacePlusByZeroZero(n, value);
			}
			return true;
		}
	};
	OnPreferenceClickListener linkAccountListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Intent assistant = new Intent();
			assistant.setClass(XecureActivity.instance(), AssistantActivity.class);
			assistant.putExtra("LinkPhoneNumber", true);
			assistant.putExtra("FromPref", true);
			assistant.putExtra("AccountNumber", n);
			startActivity(assistant);
			return true;
		}
	};
	OnPreferenceChangeListener disableChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			boolean value = (Boolean) newValue;
			if (isNewAccount) {
				builder.setEnabled(!value);
			} else {
				mPrefs.setAccountEnabled(n, !value);
			}
			return true;
		}
	};
	OnPreferenceChangeListener transportChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String key = newValue.toString();
			if (isNewAccount) {
				//TODO
				//builder.setTransport(transport);
			} else {
				mPrefs.setAccountTransport(n, key);
				preference.setSummary(mPrefs.getAccountTransportString(n));
				preference.setDefaultValue(mPrefs.getAccountTransportKey(n));
				if (mProxyPreference != null) {
					String newProxy = mPrefs.getAccountProxy(n);
					mProxyPreference.setSummary(newProxy);
					mProxyPreference.setText(newProxy);
				}
			}
			return true;
		}
	};

	OnPreferenceChangeListener iceChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			boolean value = (Boolean) newValue;
			if (isNewAccount) {
			} else {
				mPrefs.setAccountIce(n, value);
				((CheckBoxPreference)preference).setChecked(mPrefs.getAccountIce(n));
			}
			return true;
		}
	};

	OnPreferenceChangeListener stunTurnChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String value = newValue.toString();
			if (isNewAccount) {
			} else {
				mPrefs.setAccountStunServer(n, value);
				preference.setSummary(value);
			}
			return true;
		}
	};

	private void initAccountPreferencesFields(PreferenceScreen parent) {
		boolean isDefaultAccount = mPrefs.getDefaultAccountIndex() == n;
		LinphoneNatPolicy natPolicy = null;
		if (XecureManager.getLcIfManagerNotDestroyedOrNull() != null &&
				XecureManager.getLc().getProxyConfigList() != null &&
				XecureManager.getLc().getProxyConfigList().length > n) {
			LinphoneProxyConfig proxy = XecureManager.getLc().getProxyConfigList()[n];
			natPolicy = proxy.getNatPolicy();
			if (natPolicy == null) {
				natPolicy = XecureManager.getLc().createNatPolicy();
				proxy.edit();
				proxy.setNatPolicy(natPolicy);
				proxy.done();
			}
		}

		accountCreator = LinphoneCoreFactory.instance().createAccountCreator(XecureManager.getLc()
				, XecurePreferences.instance().getXmlrpcUrl());
		accountCreator.setListener(this);

    	PreferenceCategory account = (PreferenceCategory) getPreferenceScreen().findPreference(getString(R.string.pref_sipaccount_key));
    	EditTextPreference username = (EditTextPreference) account.getPreference(0);
		username.setOnPreferenceChangeListener(usernameChangedListener);
		if (!isNewAccount){
			username.setText(mPrefs.getAccountUsername(n));
			username.setSummary(username.getText());
		}

    	EditTextPreference userid = (EditTextPreference) account.getPreference(1);
		userid.setOnPreferenceChangeListener(useridChangedListener);
		if (!isNewAccount){
			userid.setText(mPrefs.getAccountUserId(n));
			userid.setSummary(userid.getText());
		}

    	EditTextPreference password = (EditTextPreference) account.getPreference(2);
		password.setOnPreferenceChangeListener(passwordChangedListener);
		if(!isNewAccount){
			password.setText(mPrefs.getAccountPassword(n));
		}

    	EditTextPreference domain = (EditTextPreference) account.getPreference(3);
    	domain.setOnPreferenceChangeListener(domainChangedListener);
		if (!isNewAccount){
			domain.setText(mPrefs.getAccountDomain(n));
			domain.setSummary(domain.getText());
		}

    	EditTextPreference displayName = (EditTextPreference) account.getPreference(4);
		displayName.setOnPreferenceChangeListener(displayNameChangedListener);
		if (!isNewAccount){
			displayName.setText(mPrefs.getAccountDisplayName(n));
			displayName.setSummary(displayName.getText());
		}

    	PreferenceCategory advanced = (PreferenceCategory) getPreferenceScreen().findPreference(getString(R.string.pref_advanced_key));
		mTransportPreference = (ListPreference) advanced.getPreference(0);
    	initializeTransportPreference(mTransportPreference);
		mTransportPreference.setOnPreferenceChangeListener(transportChangedListener);
		if(!isNewAccount){
			mTransportPreference.setSummary(mPrefs.getAccountTransportString(n));
		}

		CheckBoxPreference ice = (CheckBoxPreference) advanced.getPreference(1);
		ice.setOnPreferenceChangeListener(iceChangedListener);
		if (natPolicy != null)
			ice.setChecked(natPolicy.iceEnabled());

		EditTextPreference stunTurn = (EditTextPreference) advanced.getPreference(2);
		stunTurn.setOnPreferenceChangeListener(stunTurnChangedListener);
		if (natPolicy != null) {
			stunTurn.setText(natPolicy.getStunServer());
			stunTurn.setSummary(natPolicy.getStunServer());
		}

		mProxyPreference = (EditTextPreference) advanced.getPreference(3);
		mProxyPreference.setOnPreferenceChangeListener(proxyChangedListener);
		if (!isNewAccount){
			mProxyPreference.setText(mPrefs.getAccountProxy(n));
			mProxyPreference.setSummary("".equals(mProxyPreference.getText()) || (mProxyPreference.getText() == null) ? getString(R.string.pref_help_proxy) : mProxyPreference.getText());
		}

    	CheckBoxPreference outboundProxy = (CheckBoxPreference) advanced.getPreference(4);
    	outboundProxy.setOnPreferenceChangeListener(outboundProxyChangedListener);
		if (!isNewAccount){
			outboundProxy.setChecked(mPrefs.isAccountOutboundProxySet(n));
		}

    	EditTextPreference expires = (EditTextPreference) advanced.getPreference(5);
    	expires.setOnPreferenceChangeListener(expiresChangedListener);
		if(!isNewAccount){
			expires.setText(mPrefs.getExpires(n));
			expires.setSummary(mPrefs.getExpires(n));
		}

    	EditTextPreference prefix = (EditTextPreference) advanced.getPreference(6);
    	prefix.setOnPreferenceChangeListener(prefixChangedListener);
		if(!isNewAccount){
			String prefixValue = mPrefs.getPrefix(n);
			prefix.setText(prefixValue);
			prefix.setSummary(prefixValue);
		}

		CheckBoxPreference avpf = (CheckBoxPreference) advanced.getPreference(7);
		avpf.setOnPreferenceChangeListener(avpfChangedListener);
		if (!isNewAccount){
			avpf.setChecked(mPrefs.avpfEnabled(n));
		}

		EditTextPreference avpfRRInterval = (EditTextPreference) advanced.getPreference(8);
		avpfRRInterval.setOnPreferenceChangeListener(avpfRRIntervalChangedListener);
		if (!isNewAccount){
			avpfRRInterval.setText(mPrefs.getAvpfRRInterval(n));
			avpfRRInterval.setSummary(mPrefs.getAvpfRRInterval(n));
		}

    	CheckBoxPreference escape = (CheckBoxPreference) advanced.getPreference(9);
		escape.setOnPreferenceChangeListener(escapeChangedListener);
		if(!isNewAccount){
			escape.setChecked(mPrefs.getReplacePlusByZeroZero(n));
		}

		Preference linkAccount = advanced.getPreference(10);
		linkAccount.setOnPreferenceClickListener(linkAccountListener);

		PreferenceCategory manage = (PreferenceCategory) getPreferenceScreen().findPreference(getString(R.string.pref_manage_key));
		final CheckBoxPreference disable = (CheckBoxPreference) manage.getPreference(0);
    	disable.setEnabled(true);
    	disable.setOnPreferenceChangeListener(disableChangedListener);
		if(!isNewAccount){
			disable.setChecked(!mPrefs.isAccountEnabled(n));
		}

    	CheckBoxPreference mainAccount = (CheckBoxPreference) manage.getPreference(1);
		mainAccount.setChecked(isDefaultAccount);
		mainAccount.setEnabled(!mainAccount.isChecked());
		mainAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				mPrefs.setDefaultAccount(n);
				disable.setEnabled(false);
				disable.setChecked(false);
				preference.setEnabled(false);
				return true;
			}
		});
		if(!isNewAccount){
			mainAccount.setEnabled(!mainAccount.isChecked());
		}

		final Preference changePassword = manage.getPreference(2);
		if (mPrefs.getAccountDomain(n).compareTo(getString(R.string.default_domain)) == 0) {
			changePassword.setEnabled(!isNewAccount);
			changePassword.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					final AlertDialog.Builder alert = new AlertDialog.Builder(XecureActivity.instance());
					LayoutInflater inflater = XecureActivity.instance().getLayoutInflater();
					View layout = inflater.inflate(R.layout.new_password, null);
					final EditText pass1 = (EditText) layout.findViewById(R.id.password1);
					final EditText pass2 = (EditText) layout.findViewById(R.id.password2);
					alert.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							LinphoneAccountCreator.PasswordCheck status = accountCreator.setPassword(pass1.getText().toString());
							if (status.equals(LinphoneAccountCreator.PasswordCheck.Ok)) {
								if (pass1.getText().toString().compareTo(pass2.getText().toString()) == 0) {
									accountCreator.setUsername(mPrefs.getAccountUsername(n));
									accountCreator.setHa1(mPrefs.getAccountHa1(n));
									LinphoneAccountCreator.RequestStatus req_status = accountCreator.updatePassword(pass1.getText().toString());
									if (!req_status.equals(LinphoneAccountCreator.RequestStatus.Ok)) {
										XecureUtils.displayErrorAlert(XecureUtils.errorForRequestStatus(req_status)
												, XecureActivity.instance());
									} else {
										progress = ProgressDialog.show(XecureActivity.instance(), null, null);
										Drawable d = new ColorDrawable(ContextCompat.getColor(XecureActivity.instance(), R.color.colorE));
										d.setAlpha(200);
										progress.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
										progress.getWindow().setBackgroundDrawable(d);
										progress.setContentView(R.layout.progress_dialog);
										progress.show();
									}
								} else {
									XecureUtils.displayErrorAlert(getString(R.string.wizard_passwords_unmatched)
											, XecureActivity.instance());
								}
								return;
							}
							XecureUtils.displayErrorAlert(XecureUtils.errorForPasswordStatus(status), XecureActivity.instance());
						}
					});

					alert.setView(layout);
					alert.show();
					return true;
				}
			});
		} else {
			changePassword.setEnabled(false);
		}

		final Preference delete = manage.getPreference(3);
    	delete.setEnabled(!isNewAccount);
    	delete.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        public boolean onPreferenceClick(Preference preference) {
	    		mPrefs.deleteAccount(n);
				XecureActivity.instance().displaySettings();
				XecureActivity.instance().refreshAccounts();
	        	return true;
	        }
        });
	}

	private void initializeTransportPreference(ListPreference pref) {
		List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		entries.add(getString(R.string.pref_transport_udp));
		values.add(getString(R.string.pref_transport_udp_key));
		entries.add(getString(R.string.pref_transport_tcp));
		values.add(getString(R.string.pref_transport_tcp_key));

		if (!getResources().getBoolean(R.bool.disable_all_security_features_for_markets)) {
			entries.add(getString(R.string.pref_transport_tls));
			values.add(getString(R.string.pref_transport_tls_key));
		}
		setListPreferenceValues(pref, entries, values);

		if (! isNewAccount) {
			pref.setSummary(mPrefs.getAccountTransportString(n));
			pref.setDefaultValue(mPrefs.getAccountTransportKey(n));
			pref.setValueIndex(entries.indexOf(mPrefs.getAccountTransportString(n)));
		} else {

			pref.setSummary(getString(R.string.pref_transport_udp));
			pref.setDefaultValue(getString(R.string.pref_transport_udp));
			pref.setValueIndex(entries.indexOf(getString(R.string.pref_transport_udp)));
		}
	}

	private static void setListPreferenceValues(ListPreference pref, List<CharSequence> entries, List<CharSequence> values) {
		CharSequence[] contents = new CharSequence[entries.size()];
		entries.toArray(contents);
		pref.setEntries(contents);
		contents = new CharSequence[values.size()];
		values.toArray(contents);
		pref.setEntryValues(contents);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (XecureActivity.isInstanciated()) {
			XecureActivity.instance().selectMenu(FragmentsAvailable.SETTINGS);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (XecureActivity.isInstanciated()) {
			try {
				if(isNewAccount){
					for (int i = 0; i < XecurePreferences.instance().getAccountCount(); i++)
						XecurePreferences.instance().deleteAccount(i);
					builder.saveNewAccount();
				}
			} catch (LinphoneCoreException e) {
				Log.e(e);
			}
			XecureActivity.instance().isNewProxyConfig();
			XecureManager.getLc().refreshRegisters();
			XecureActivity.instance().hideTopBar();
		}
	}

	@Override
	public void onAccountCreatorIsAccountUsed(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
	}

	@Override
	public void onAccountCreatorAccountCreated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

	}

	@Override
	public void onAccountCreatorAccountActivated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

	}

	@Override
	public void onAccountCreatorAccountLinkedWithPhoneNumber(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

	}

	@Override
	public void onAccountCreatorPhoneNumberLinkActivated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

	}

	@Override
	public void onAccountCreatorIsAccountActivated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

	}

	@Override
	public void onAccountCreatorPhoneAccountRecovered(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

	}

	@Override
	public void onAccountCreatorIsAccountLinked(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

	}

	@Override
	public void onAccountCreatorIsPhoneNumberUsed(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
	}

	@Override
	public void onAccountCreatorPasswordUpdated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
		if (progress != null) progress.dismiss();
		if (status.equals(LinphoneAccountCreator.RequestStatus.Ok)) {
			mPrefs.setAccountPassword(n, accountCreator.getPassword());
			PreferenceCategory account = (PreferenceCategory) getPreferenceScreen().findPreference(getString(R.string.pref_sipaccount_key));
			((EditTextPreference) account.getPreference(2)).setText(mPrefs.getAccountPassword(n));
			XecureUtils.displayErrorAlert(getString(R.string.pref_password_changed), XecureActivity.instance());
		} else {
			XecureUtils.displayErrorAlert(XecureUtils.errorForRequestStatus(status), XecureActivity.instance());
		}
	}
}
