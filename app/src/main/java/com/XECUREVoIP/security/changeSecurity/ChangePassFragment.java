package com.XECUREVoIP.security.changeSecurity;


import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.XECUREVoIP.R;
import com.XECUREVoIP.security.PassSettingFragment;
import com.XECUREVoIP.security.SecurityUtils;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChangePassFragment extends Fragment implements View.OnClickListener {

    private View m_view = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        m_view = (RelativeLayout) inflater.inflate(R.layout.fragment_change_pass, container, false);
        m_view.findViewById(R.id.btn_changePass).setOnClickListener(this);

        return m_view;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.btn_changePass:
                SharedPreferences pref = getActivity().getSharedPreferences("Xecure", MODE_PRIVATE);
                String strCrrEncryptionOldPass = pref.getString("pass", "");

                EditText edtOldPass = (EditText)m_view.findViewById(R.id.id_edtOldPass);
                EditText edtNewPass = (EditText)m_view.findViewById(R.id.id_edtNewPass);
                EditText edtConPass = (EditText)m_view.findViewById(R.id.id_edtConPass);
                String strOldPass = edtOldPass.getText().toString();
                String strNewPass = edtNewPass.getText().toString();
                String strConfirmPass = edtConPass.getText().toString();
                SecurityUtils utils = new SecurityUtils(Build.ID);
                try {
                    //decryption
                    String strCrrDecOldPass = utils.decrypt(strCrrEncryptionOldPass);
                    if (!strCrrDecOldPass.equals(strOldPass)) {
                        edtOldPass.setError("Incorrect password");
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (strNewPass.isEmpty() || strNewPass.length() < 6){
                    edtNewPass.setError("Enter a password at least six letters");
                    return;
                }
                if (!strNewPass.equals(strConfirmPass)){
                    edtConPass.setError("The password you entered do not match. Try again.");
                    return;
                }


                SharedPreferences.Editor editor = pref.edit();
                //init the repository
                editor.clear().commit();
                //encryption
                try {
                    editor.putString("pass", utils.encrypt(strNewPass)).commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    Toast.makeText(getActivity(),"Your password is changed", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
        }
    }
}
