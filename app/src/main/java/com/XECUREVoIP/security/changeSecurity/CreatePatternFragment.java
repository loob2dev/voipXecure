package com.XECUREVoIP.security.changeSecurity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.XECUREVoIP.R;
import com.XECUREVoIP.security.SecurityUtils;
import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class CreatePatternFragment extends Fragment implements PatternLockViewListener, View.OnClickListener{
    PatternLockView patternLockView;
    private String strTmpPass;
    private int iSequence = 0;
    private TextView alertText;
    private Button btnRedraw;
    SharedPreferences preXecue;

   @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
       View view = inflater.inflate(R.layout.fragment_create_pattern, container, false);
       patternLockView = view.findViewById(R.id.pattern_lock_view);
       patternLockView.addPatternLockListener(this);
       btnRedraw = view.findViewById(R.id.id_btnRedraw);
       btnRedraw.setOnClickListener(this);
       alertText = view.findViewById(R.id.alertText);

       preXecue = getActivity().getSharedPreferences("Xecure", MODE_PRIVATE);

       return view;
    }

    @Override
    public void onClick(View v) {
       //notify to user
        alertText.setText(R.string.set_app_pattern_notify);
        alertText.setTextColor(getResources().getColor(R.color.colorC));
        //hide the redraw button
        btnRedraw.setVisibility(View.INVISIBLE);
        patternLockView.clearPattern();
        strTmpPass = "";
        iSequence = 0;
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
        if (pattern.size() < 4){
            patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorI));
            patternLockView.clearPattern();
            
            return;
        }
        String strPass = PatternLockUtils.patternToString(patternLockView, pattern);
        switch (iSequence){
            case 0:
                //memorize the password as template
                strTmpPass = strPass;
                btnRedraw.setVisibility(View.VISIBLE);
                //notify to user
                iSequence++;
                alertText.setText(R.string.set_app_pattern_alert_confirm);
                alertText.setTextColor(getResources().getColor(R.color.colorC));
                break;
            case 1:
                if (strTmpPass.equals(strPass)){
                    //init the repository
                    SharedPreferences.Editor editor = preXecue.edit();
                    editor.clear().commit();
                    //store the encryption pass
                    SecurityUtils utils = new SecurityUtils(Build.ID);try {
                        editor.putString("pattern", utils.encrypt(strPass)).commit();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        Toast.makeText(getActivity(),"Your pattern is set", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                }else {
                    //notify to user
                    alertText.setText(R.string.set_app_pattern_alert_not_match);
                    alertText.setTextColor(getResources().getColor(R.color.colorP));
                }
        }
        patternLockView.clearPattern();
    }

    @Override
    public void onCleared() {

    }
}
