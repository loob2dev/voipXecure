package com.XECUREVoIP.security.changeSecurity;


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
import com.XECUREVoIP.security.SecurityUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConfirmPassFragment extends Fragment implements View.OnClickListener{

    private View m_view = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        m_view = inflater.inflate(R.layout.fragment_confirm_pass, container, false);
        m_view.findViewById(R.id.btn_changePass).setOnClickListener(this);

        return m_view;
    }

    @Override
    public void onClick(View v) {
        SharedPreferences pref = getActivity().getSharedPreferences("Xecure", MODE_PRIVATE);
        String strCrrEncryptionOldPass = pref.getString("pass", "");

        EditText edtOldPass = (EditText)m_view.findViewById(R.id.id_edtOldPass);
        String strOldPass = edtOldPass.getText().toString();
        SecurityUtils utils = new SecurityUtils(Build.ID);
        try {
            //decryption
            String strCrrDecOldPass = utils.decrypt(strCrrEncryptionOldPass);
            if (!strCrrDecOldPass.equals(strOldPass)) {
                edtOldPass.setError("Incorrect password");
                return;
            }
        } catch (Exception e) {
            edtOldPass.setError("Incorrect password");
            e.printStackTrace();
            return;
        }
        ChangeSecurityActivity.viewPager.setCurrentItem(3);
    }
}
