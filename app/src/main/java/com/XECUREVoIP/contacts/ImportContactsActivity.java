package com.XECUREVoIP.contacts;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.StatusFragment;
import com.XECUREVoIP.XecureContact;
import com.XECUREVoIP.XecureNumberOrAddress;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import dmax.dialog.SpotsDialog;

public class ImportContactsActivity extends AppCompatActivity implements View.OnClickListener{

    ArrayAdapter<String> adapterCompany, adapterDepartment, adapterSubDepartment;
    AutoCompleteTextView company, department, sub_department;
    ImageView back, btnImport;
    TextView import_error, import_success, import_failed;
    private StatusFragment status;

    AVLoadingIndicatorView progress;

    android.app.AlertDialog alertDlg;

    RequestQueue requestQueue;

    final static String URL_BASE = "https://xecure.systems/mobile/";
    final static String URL_BY_ALL = URL_BASE + "getContactsByAll";
    final static String URL_BY_COMPANY = URL_BASE + "getContactsByCompany";
    final static String URL_BY_COMPANY_DEPARTMENT = URL_BASE + "getContactsByCompanyAndDepartment";
    final static String URL_GET_COMPANIES = URL_BASE + "getCompanies";
    final static String URL_GET_DEPARTMENTS = URL_BASE + "getDepartments";
    final static String URL_GET_SUB_DEPARTMENTS = URL_BASE + "getSubDepartments";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_contacts);
        //init view
        back = findViewById(R.id.back);
        back.setOnClickListener(this);

        company = findViewById(R.id.contactCompany);
        department = findViewById(R.id.contactDepartment);
        sub_department = findViewById(R.id.contactSubDepartment);
        company.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getDepartment();
            }
        });
        department.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getSubDepartment();
            }
        });

        btnImport = findViewById(R.id.id_btnImport);
        btnImport.setOnClickListener(this);

        progress = findViewById(R.id.progress);
        progress.hide();

        import_error = findViewById(R.id.error);
        import_success = findViewById(R.id.success);
        import_failed = findViewById(R.id.failed);

        //create adapter and init text view
        adapterCompany = new ArrayAdapter<String>(this,android.R.layout.select_dialog_item, new ArrayList<String>());
        adapterDepartment = new ArrayAdapter<String>(this,android.R.layout.select_dialog_item, new ArrayList<String>());
        adapterSubDepartment = new ArrayAdapter<String>(this,android.R.layout.select_dialog_item, new ArrayList<String>());

        company.setAdapter(adapterCompany);
        company.setThreshold(1);
        department.setAdapter(adapterDepartment);
        department.setThreshold(1);
        sub_department.setAdapter(adapterSubDepartment);
        sub_department.setThreshold(1);

        // Creates the Volley request queue
        alertDlg = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Loading...")
                .build();
        alertDlg.show();
        requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(companyRequest);

        status.enableSideMenu(false);
    }

    private void getContacts(String nameCompany, String nameDepartment, String nameSubDepartment) {
        String url = "";
        if (!nameCompany.isEmpty() && !nameDepartment.isEmpty() && !nameSubDepartment.isEmpty())
            url = getContactsByAll(nameCompany, nameDepartment, nameSubDepartment);
        else if (!nameCompany.isEmpty() && !nameDepartment.isEmpty())
            url = getContactsByComapanyAndDepartment(nameCompany, nameDepartment);
        else if (!nameCompany.isEmpty())
            url = getContactsByComapny(nameCompany);
        else
            return;
        JsonObjectRequest request = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String success = response.getString("success");
                            if (success.equals("true")){

                                JSONArray array =  response.getJSONArray("data");
                                ImportTask task = new ImportTask(array);
                                task.execute();
                            } else {
                                import_error.setVisibility(View.VISIBLE);
                                btnImport.setEnabled(true);
                                progress.smoothToHide();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        import_error.setVisibility(View.VISIBLE);
                        btnImport.setEnabled(true);
                        progress.smoothToHide();
                    }
                });
        requestQueue.add(request);
    }

    private String getContactsByComapny(String nameCompany) {
        Uri.Builder builder = Uri.parse(URL_BY_COMPANY).buildUpon();
        builder.appendQueryParameter("company_name", nameCompany);

        return builder.build().toString();
    }

    private String getContactsByComapanyAndDepartment(String nameCompany, String nameDepartment) {
        Uri.Builder builder = Uri.parse(URL_BY_COMPANY_DEPARTMENT).buildUpon();
        builder.appendQueryParameter("company_name", nameCompany);
        builder.appendQueryParameter("department_name", nameDepartment);

        return builder.build().toString();
    }

    private String getContactsByAll(String nameCompany, String nameDepartment, String nameSubDepartment) {
        Uri.Builder builder = Uri.parse(URL_BY_ALL).buildUpon();
        builder.appendQueryParameter("company_name", nameCompany);
        builder.appendQueryParameter("department_name", nameDepartment);
        builder.appendQueryParameter("sub_department_name", nameSubDepartment);

        return builder.build().toString();
    }

    private void getSubDepartment() {
        Uri.Builder builder = Uri.parse(URL_GET_SUB_DEPARTMENTS).buildUpon();
        builder.appendQueryParameter("company_name", company.getText().toString());
        builder.appendQueryParameter("department_name", department.getText().toString());
        String url = builder.build().toString();
        JsonArrayRequest subdepartmentRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        adapterSubDepartment.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                adapterSubDepartment.add(response.get(i).toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        adapterSubDepartment.notifyDataSetChanged();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String str = error.toString();
                    }
                });
        requestQueue.add(subdepartmentRequest);
    }

    private void getDepartment() {
        Uri.Builder builder = Uri.parse(URL_GET_DEPARTMENTS).buildUpon();
        builder.appendQueryParameter("company_name", company.getText().toString());
        String url = builder.build().toString();
        JsonArrayRequest departmentRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        adapterDepartment.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                adapterDepartment.add(response.get(i).toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        adapterDepartment.notifyDataSetChanged();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String str = error.toString();
                    }
                });
        requestQueue.add(departmentRequest);
    }

    // init company names request
    JsonArrayRequest companyRequest = new JsonArrayRequest
            (Request.Method.GET, URL_GET_COMPANIES, null, new Response.Listener<JSONArray>() {

                @Override
                public void onResponse(JSONArray response) {
                    adapterCompany.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            adapterCompany.add(response.get(i).toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    adapterCompany.notifyDataSetChanged();
                    alertDlg.dismiss();
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    alertDlg.dismiss();
                }
            }
            );

    @Override
    public void onClick(View v) {
        import_error.setVisibility(View.INVISIBLE);
        int id = v.getId();
        switch (id){
            case R.id.back:
                finish();
                break;
            case R.id.id_btnImport:
                String nameCompany = company.getText().toString();
                String nameDepartment = department.getText().toString();
                String nameSubDepartment = sub_department.getText().toString();
                if (nameCompany.isEmpty()){
                    company.setError("Type company name");
                    return;
                }
                btnImport.setEnabled(false);
                import_success.setVisibility(View.INVISIBLE);
                import_failed.setVisibility(View.INVISIBLE);

                getContacts(nameCompany, nameDepartment, nameSubDepartment);
                progress.smoothToShow();
        }
    }

    public void updateStatusFragment(StatusFragment fragment) {
        status = fragment;
    }

    class ImportTask extends AsyncTask<String, String, String>{

        final String ID_DOWNLOAD = "download";
        final int CONTACTS_DOWNLOAD = 0x01;
        final int PROGRESS_MAX = 100;
        int PROGRESS_CURRENT = 0;

        NotificationManagerCompat notificationManager;
        NotificationCompat.Builder mBuilder;

        private final JSONArray m_array;

        public ImportTask(JSONArray array) {
            m_array = array;
            //init download notification
            notificationManager = NotificationManagerCompat.from(ImportContactsActivity.this);
            mBuilder = new NotificationCompat.Builder(ImportContactsActivity.this, ID_DOWNLOAD);
            mBuilder.setContentTitle("Download Contacts")
                    .setContentText("wait in a sec.")
                    .setSmallIcon(R.drawable.linphone_notification_icon)
                    .setPriority(NotificationCompat.PRIORITY_LOW);
        }

        @Override
        protected String doInBackground(String... strings) {
            mBuilder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT + 20, false);
            notificationManager.notify(CONTACTS_DOWNLOAD, mBuilder.build());
            boolean bImported = false;
            try {
                int step = m_array.length() > 0 ? (PROGRESS_MAX - PROGRESS_CURRENT) / m_array.length()
                        : 0;
                for (int i = 0; i < m_array.length(); i++) {
                    JSONObject object = m_array.getJSONObject(i);
                    String strCompany = !object.getString("company_name").equals("null") ?
                            object.getString("company_name") : "";
                    String strDepartment = !object.getString("department_name").equals("null") ?
                            object.getString("department_name") : "";
                    String strSubDepartment = !object.getString("sub_department_name").equals("null") ?
                            object.getString("sub_department_name") : "";
                    String strSipAccount = !object.getString("sip_address").equals("null") ?
                            object.getString("sip_address") : "";
                    String strFirstName = !object.getString("first_name").equals("null") ?
                            object.getString("first_name") : "";
                    String strLastName = !object.getString("last_name").equals("null") ?
                            object.getString("last_name") : "";
                    String strEmailAddress = !object.getString("email_address").equals("null")?
                            object.getString("email_address") : "";
                    String strOfficePhone = !object.getString("office_phone_number").equals("null") ?
                            object.getString("office_phone_number") : "";
                    String strMobilePhone = !object.getString("mobile_phone_number").equals("null") ?
                            object.getString("mobile_phone_number") : "";
                    //add contact
                    XecureContact contact = new XecureContact();
                    contact.setFirstNameAndLastName(strFirstName, strLastName);
                    contact.setEmail(strEmailAddress);
                    contact.addNumberOrAddress(new XecureNumberOrAddress(strSipAccount, true));
                    contact.addNumberOrAddress(new XecureNumberOrAddress(strMobilePhone, false));
                    contact.addNumberOrAddress(new XecureNumberOrAddress(strOfficePhone, false));
                    contact.setCompany(strCompany);
                    contact.setDepartment(strDepartment);
                    contact.setSubDepartment(strSubDepartment);
                    contact.setShare(true);
                    ContactDBHelper dbHelper = new ContactDBHelper(ImportContactsActivity.this);
                    if (!dbHelper.isExisting(contact)) {
                        contact.save(ImportContactsActivity.this);
                        bImported = true;
                    }
                    //display progress
                    mBuilder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT += i * step, false);
                    notificationManager.notify(CONTACTS_DOWNLOAD, mBuilder.build());
                }
                mBuilder.setProgress(PROGRESS_MAX, PROGRESS_MAX, false);
                notificationManager.notify(CONTACTS_DOWNLOAD, mBuilder.build());
            }catch (Exception e){
                e.printStackTrace();
            }
            PROGRESS_CURRENT = 0;
            if (bImported){
                mBuilder.setContentText("Download complete")
                        .setProgress(0,0,false);
                notificationManager.notify(CONTACTS_DOWNLOAD, mBuilder.build());

                return "success";
            }else {
                mBuilder.setContentText("Download failed")
                        .setProgress(0,0,false);
                notificationManager.notify(CONTACTS_DOWNLOAD, mBuilder.build());

                return "failed";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            btnImport.setEnabled(true);
            progress.smoothToHide();
            if (s.equals("success"))
                import_success.setVisibility(View.VISIBLE);
            else if (s.equals("failed"))
                import_failed.setVisibility(View.VISIBLE);
            super.onPostExecute(s);
        }
    }
}
