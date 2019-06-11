package com.XECUREVoIP.security.changeSecurity;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.security.SecurityUtils;
import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConfirmPattenFragment extends Fragment implements PatternLockViewListener {
    private PatternLockView patternLockView;
    private String strCurrentPass = "";
    private TextView alertText;
    SharedPreferences preXecue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_confirm_patten, container, false);
        patternLockView = view.findViewById(R.id.pattern_lock_view);
        patternLockView.addPatternLockListener(this);
        alertText = view.findViewById(R.id.alertText);

        //get the current password
        preXecue = getActivity().getSharedPreferences("Xecure", MODE_PRIVATE);
        String strCrrEncryptionPass = preXecue.getString("pattern", "");
        SecurityUtils utils = new SecurityUtils(Build.ID);
        try {
            strCurrentPass = utils.decrypt(strCrrEncryptionPass);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }

    @Override
    public void onStarted() {
        patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorD));
    }

    @Override
    public void onProgress(List<PatternLockView.Dot> progressPattern) {

    }

    @Override
    public void onComplete(List<PatternLockView.Dot> pattern) {
        String strPass = PatternLockUtils.patternToString(patternLockView, pattern);
        if (pattern.size() < 4){
            patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorI));
            patternLockView.clearPattern();

            return;
        }else if(strCurrentPass.equals(strPass)) {
            //notify to user
            ChangeSecurityActivity.viewPager.setCurrentItem(4);
        }else {
            //notify to user
            patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorI));
            alertText.setText(R.string.sign_app_pattern_incorrect);
            alertText.setTextColor(getResources().getColor(R.color.colorP));
        }
    }

    @Override
    public void onCleared() {

    }
}
