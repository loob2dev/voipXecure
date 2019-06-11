package com.XECUREVoIP.security.createSecurity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.XECUREVoIP.R;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.assistant.AssistantActivity;
import com.XECUREVoIP.security.SecurityUtils;
import com.XECUREVoIP.security.changeSecurity.ChangeSecurityActivity;

public class GenPassFragment extends Fragment {
    private EditText m_edtConfirm;
    private EditText m_edtPass;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_generate_pass, container, false);

        View.OnClickListener listner = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setFocusableInTouchMode(true);
            }
        };
        m_edtConfirm = (EditText)view.findViewById(R.id.id_edtConfirm);
        m_edtConfirm.setFocusable(false);
        m_edtConfirm.setOnClickListener(listner);
        m_edtPass = (EditText)view.findViewById(R.id.id_edtPass);
        m_edtPass.setFocusable(false);
        m_edtPass.setOnClickListener(listner);

        view.findViewById(R.id.id_btnSet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strPass = m_edtPass.getText().toString();
                String strConfirm = m_edtConfirm.getText().toString();
                if (strPass.isEmpty() || strPass.length() < 6){
                    m_edtPass.setError("Enter a password at least six letters");
                    return;
                }
                if (strConfirm.isEmpty()){
                    m_edtConfirm.setError("Enter a confirm password");
                    return;
                }
                if (!strPass.toString().equals(strConfirm)){
                    m_edtConfirm.setError("The password you entered do not match. Try again.");
                    return;
                }

                SharedPreferences pres = getContext().getSharedPreferences("Xecure", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = pres.edit();
                SecurityUtils utils = new SecurityUtils(Build.ID);
                //encryption
                try {
                    editor.putString("pass", utils.encrypt(strPass)).commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (getActivity().getIntent().getBooleanExtra("Lock", false))
                        startActivity(new Intent(getActivity(), ChangeSecurityActivity.class));
                    if (AssistantActivity.instance() != null)
                        AssistantActivity.instance().success();
                    else
                        startActivity(new Intent(getActivity(), XecureActivity.class));
                    getActivity().finish();
                }
            }
        });
        return view;
    }
}
