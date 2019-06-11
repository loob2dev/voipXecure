package com.XECUREVoIP.security;

/*
PassSettingFragment.java
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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;


import com.XECUREVoIP.R;

import static android.content.Context.MODE_PRIVATE;

public class PassSettingFragment extends Fragment implements OnClickListener {
    private View m_view = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        m_view = inflater.inflate(R.layout.layout_lock, container, false);
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
                //encryption
                try {
                    editor.putString("pass", utils.encrypt(strNewPass)).commit();
//                    m_imgAppDetail.callOnClick();
                    Toast.makeText(PassSettingFragment.this.getActivity(),"Your password is changed", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
