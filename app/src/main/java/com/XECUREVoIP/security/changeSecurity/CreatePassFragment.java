package com.XECUREVoIP.security.changeSecurity;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.XECUREVoIP.R;
import com.XECUREVoIP.assistant.AssistantActivity;
import com.XECUREVoIP.security.SecurityUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class CreatePassFragment extends Fragment {

    private EditText m_edtConfirm;
    private EditText m_edtPass;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_create_pass, container, false);
        m_edtConfirm = (EditText)view.findViewById(R.id.id_edtConfirm);
        m_edtPass = (EditText)view.findViewById(R.id.id_edtPass);

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
                //clear the preferences
                editor.clear().commit();
                //encryption
                try {
                    editor.putString("pass", utils.encrypt(strPass)).commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    Toast.makeText(getActivity(),"Your password is set.", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            }
        });
        return view;
    }
}
