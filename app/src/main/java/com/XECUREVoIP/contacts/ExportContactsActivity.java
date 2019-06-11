package com.XECUREVoIP.contacts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.StatusFragment;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecureContact;
import com.XECUREVoIP.XecureNumberOrAddress;
import com.XECUREVoIP.XecureUtils;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportContactsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener{
    private ListView contactsList;
    private LayoutInflater mInflater;
    private ImageView selectAll, deselectAll, upload, back;
    private boolean isSearchMode;
    private EditText searchField;

    private StatusFragment status;

    AVLoadingIndicatorView progress;

    private final String URL_BASE = "https://xecure.systems/mobile/";
    private final String URL_EXPORT_CONTACTS = URL_BASE + "addContact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_contacts);

        searchField = (EditText) findViewById(R.id.searchField);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchContacts();
            }
        });

        contactsList = (ListView) findViewById(R.id.contactsList);
        contactsList.setOnItemClickListener(this);

        contactsList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        final ContactsListAdapter adapter = new ContactsListAdapter(new ArrayList<XecureContact>());
        contactsList.setAdapter(adapter);
        ContactsManager.getInstance().getUnSharedContacts(new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                adapter.add(msg.obj);
            }
        }, searchField.getText().toString());

        mInflater = getLayoutInflater();
        isSearchMode = false;

        selectAll = findViewById(R.id.select_all);
        selectAll.setOnClickListener(this);
        deselectAll = findViewById(R.id.deselect_all);
        deselectAll.setOnClickListener(this);

        upload = findViewById(R.id.upload);
        upload.setOnClickListener(this);
        upload.setEnabled(false);

        back = findViewById(R.id.back);
        back.setOnClickListener(this);

        progress = findViewById(R.id.progress);
        progress.hide();
        status.enableSideMenu(false);

        hideSoftKeyboard();
    }

    private void searchContacts() {
        ((ContactsListAdapter)contactsList.getAdapter()).clear();
        ContactsManager.getInstance().getUnSharedContacts(new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                ((ContactsListAdapter)contactsList.getAdapter()).add(msg.obj);
            }
        }, searchField.getText().toString());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.back:
                finish();
                break;
            case R.id.select_all:
                deselectAll.setVisibility(View.VISIBLE);
                selectAll.setVisibility(View.GONE);
                selectAllList(true);
                break;
            case R.id.deselect_all:
                deselectAll.setVisibility(View.GONE);
                selectAll.setVisibility(View.VISIBLE);
                selectAllList(false);
                break;
            case R.id.upload:
                final Dialog dialog = XecureActivity.instance().displayImportDialog(ExportContactsActivity.this, getString(R.string.share_text));
                Button btn_import = (Button) dialog.findViewById(R.id.ok_button);
                Button btn_cancel = (Button) dialog.findViewById(R.id.cancel);

                btn_import.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        progress.smoothToShow();
                        ExportTask task = new ExportTask();
                        task.execute();
                        dialog.dismiss();
                    }
                });

                btn_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();

                    }
                });
                dialog.show();
        }
    }

    public void updateStatusFragment(StatusFragment fragment) {
        status = fragment;
    }

    class ContactsListAdapter extends BaseAdapter implements SectionIndexer {
        public void add(Object obj) {
            contacts.add((XecureContact) obj);
            notifyDataSetChanged();
        }

        public void remove(XecureContact contact) {
            contacts.remove(contact);
            notifyDataSetChanged();
        }

        public void clear() {
            contacts.clear();
        }

        private class ViewHolder {
            public CheckBox delete;
            public ImageView linphoneFriend;
            public TextView name;
            public LinearLayout separator;
            public TextView separatorText;
            public ImageView contactPicture;
            public TextView company;
            public TextView department;
            public TextView comma;
            //public ImageView friendStatus;

            public ViewHolder(View view) {
                delete = (CheckBox) view.findViewById(R.id.delete);
                linphoneFriend = (ImageView) view.findViewById(R.id.friendLinphone);
                name = (TextView) view.findViewById(R.id.name);
                separator = (LinearLayout) view.findViewById(R.id.separator);
                separatorText = (TextView) view.findViewById(R.id.separator_text);
                contactPicture = (ImageView) view.findViewById(R.id.contact_picture);
                company = (TextView) view.findViewById(R.id.contactCompany);
                department = (TextView) view.findViewById(R.id.contactDepartment);
                comma = (TextView) view.findViewById(R.id.comma);
            }
        }

        private List<XecureContact> contacts;
        String[] sections;
        ArrayList<String> sectionsList;
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();

        ContactsListAdapter(List<XecureContact> contactsList) {
            updateDataSet(contactsList);
        }

        public void updateDataSet(List<XecureContact> contactsList) {
            contacts = contactsList;

            map = new LinkedHashMap<String, Integer>();
            String prevLetter = null;
            for (int i = 0; i < contacts.size(); i++) {
                XecureContact contact = contacts.get(i);
                String fullName = contact.getFullName();
                if (fullName == null || fullName.isEmpty()) {
                    continue;
                }
                String firstLetter = fullName.substring(0, 1).toUpperCase(Locale.getDefault());
                if (!firstLetter.equals(prevLetter)) {
                    prevLetter = firstLetter;
                    map.put(firstLetter, i);
                }
            }
            sectionsList = new ArrayList<String>(map.keySet());
            sections = new String[sectionsList.size()];
            sectionsList.toArray(sections);

            notifyDataSetChanged();
        }

        public int getCount() {
            return contacts.size();
        }

        public Object getItem(int position) {
            if (position >= getCount()) return null;
            return contacts.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = null;
            XecureContact contact = (XecureContact) getItem(position);
            if (contact == null) return null;

            ContactsListAdapter.ViewHolder holder = null;
            if (convertView != null) {
                view = convertView;
                holder = (ContactsListAdapter.ViewHolder) view.getTag();
            } else {
                view = mInflater.inflate(R.layout.contact_cell, parent, false);
                holder = new ContactsListAdapter.ViewHolder(view);
                view.setTag(holder);
            }

            holder.name.setText(contact.getFullName());

            if (!isSearchMode) {
                if (getPositionForSection(getSectionForPosition(position)) != position) {
                    holder.separator.setVisibility(View.GONE);
                } else {
                    holder.separator.setVisibility(View.VISIBLE);
                    String fullName = contact.getFullName();
                    if (fullName != null && !fullName.isEmpty()) {
                        holder.separatorText.setText(String.valueOf(fullName.charAt(0)));
                    }
                }
            } else {
                holder.separator.setVisibility(View.GONE);
            }

            if (contact.hasPhoto()) {
                XecureUtils.setThumbnailPictureFromUri(XecureActivity.instance(), holder.contactPicture, contact.getThumbnailUri());
            } else {
                holder.contactPicture.setImageBitmap(ContactsManager.getInstance().getDefaultAvatarBitmap());
            }

            boolean isOrgVisible = getResources().getBoolean(R.bool.display_contact_organization);
            String cmp = contact.getCompany();
            if (cmp != null && !cmp.isEmpty() && isOrgVisible) {
                holder.company.setText(cmp);
                holder.company.setVisibility(View.VISIBLE);
            } else {
                holder.company.setVisibility(View.GONE);
            }
            String department = contact.getDepartment();
            if (department != null && !department.isEmpty()){
                if (cmp != null && !cmp.isEmpty() && isOrgVisible)
                    holder.comma.setVisibility(View.VISIBLE);
                holder.department.setText(department);
                holder.department.setVisibility(View.VISIBLE);
            } else {
                holder.department.setVisibility(View.GONE);
            }

            holder.delete.setVisibility(View.VISIBLE);
            holder.delete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    contactsList.setItemChecked(position, b);
                    if(getNbItemsChecked() == getCount()){
                        deselectAll.setVisibility(View.VISIBLE);
                        selectAll.setVisibility(View.GONE);
                        upload.setEnabled(true);
                    } else {
                        if(getNbItemsChecked() == 0){
                            deselectAll.setVisibility(View.GONE);
                            selectAll.setVisibility(View.VISIBLE);
                            upload.setEnabled(false);
                        } else {
                            deselectAll.setVisibility(View.GONE);
                            selectAll.setVisibility(View.VISIBLE);
                            upload.setEnabled(true);
                        }
                    }
                }
            });
            if (contactsList.isItemChecked(position)) {
                holder.delete.setChecked(true);
            } else {
                holder.delete.setChecked(false);
            }
            final ViewHolder finalHolder = holder;
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finalHolder.delete.setChecked(!finalHolder.delete.isChecked());
                }
            });
            return view;
        }

        @Override
        public Object[] getSections() {
            return sections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            if (sectionIndex >= sections.length || sectionIndex < 0) {
                return 0;
            }
            return map.get(sections[sectionIndex]);
        }

        @Override
        public int getSectionForPosition(int position) {
            if (position >= contacts.size() || position < 0) {
                return 0;
            }
            XecureContact contact = contacts.get(position);
            String fullName = contact.getFullName();
            if (fullName == null || fullName.isEmpty()) {
                return 0;
            }
            String letter = fullName.substring(0, 1).toUpperCase(Locale.getDefault());
            return sectionsList.indexOf(letter);
        }
    }
    public int getNbItemsChecked(){
        int size = contactsList.getAdapter().getCount();
        int nb = 0;
        for(int i=0; i<size; i++) {
            if(contactsList.isItemChecked(i)) {
                nb ++;
            }
        }
        return nb;
    }

    private void selectAllList(boolean isSelectAll){
        int size = contactsList.getAdapter().getCount();
        for(int i=0; i<size; i++) {
            contactsList.setItemChecked(i,isSelectAll);
        }
    }

    private class ExportTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            int size = contactsList.getAdapter().getCount();

            for (int i = size - 1; i >= 0; i--) {
                if (contactsList.isItemChecked(i)) {
                    final XecureContact contact = (XecureContact) contactsList.getAdapter().getItem(i);
                    String company = contact.getCompany();
                    String department = contact.getDepartment();
                    String sub_department = contact.getSubDepartment();
                    ArrayList<String> arrayAdress = new ArrayList<String>();
                    ArrayList<String> arrayNumber = new ArrayList<String>();
                    for (XecureNumberOrAddress noa : contact.getNumbersOrAddresses()){
                        if (noa.isSIPAddress())
                            arrayAdress.add(noa.getValue());
                        else
                            arrayNumber.add(noa.getValue());
                    }
                    String sip_address = null;
                    if (arrayAdress.size() == 1){
                        sip_address = arrayAdress.get(0);
                    }
                    String first_name = contact.getFirstName();
                    String last_name = contact.getLastName();
                    String email_address = contact.getEmail();
                    String mobile_phone = null;
                    String office_phone = null;
                    if (arrayNumber.size() == 1)
                        mobile_phone = arrayNumber.get(0);
                    if (arrayNumber.size() == 2){
                        mobile_phone = arrayNumber.get(0);
                        office_phone = arrayNumber.get(1);
                    }
                    //post
                    Uri.Builder builder = Uri.parse(URL_EXPORT_CONTACTS).buildUpon();
                    builder.appendQueryParameter("company_name", company);
                    builder.appendQueryParameter("department_name", department);
                    builder.appendQueryParameter("sub_department_name", sub_department);
                    builder.appendQueryParameter("sip_address", sip_address);
                    builder.appendQueryParameter("first_name", first_name);
                    builder.appendQueryParameter("last_name", last_name);
                    builder.appendQueryParameter("email_address", email_address);
                    builder.appendQueryParameter("office_phone_number", office_phone);
                    builder.appendQueryParameter("mobile_phone_number", mobile_phone);
                    String url = builder.build().toString();
                    StringRequest request = new StringRequest(url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            contact.setShare(true);
                            contact.update(ExportContactsActivity.this);
                            ((ContactsListAdapter)contactsList.getAdapter()).remove(contact);

                            if (response.equals("success")){

                            }else {

                            }
                            searchContacts();
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });
                    RequestQueue requestQueue = Volley.newRequestQueue(ExportContactsActivity.this);
                    requestQueue.add(request);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            progress.smoothToHide();
        }
    }

    public void hideSoftKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

}
