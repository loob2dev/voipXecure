package com.XECUREVoIP.security.createSecurity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.assistant.AssistantActivity;
import com.XECUREVoIP.security.SecurityUtils;
import com.XECUREVoIP.security.changeSecurity.ChangeSecurityActivity;
import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;

import java.util.List;

public class GenPattenFragment extends Fragment {
    private TextView alertText;
    private Button btnRedraw;
    PatternLockView m_patternLockView;
    String strPass = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_create_patten, container, false);

        alertText = (TextView)view.findViewById(R.id.alertText);
        btnRedraw = (Button) view.findViewById(R.id.id_btnRedraw);
        btnRedraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnRedraw.setVisibility(View.GONE);
                m_patternLockView.clearPattern();
                strPass = "";
            }
        });

        m_patternLockView = view.findViewById(R.id.pattern_lock_view);
        m_patternLockView.addPatternLockListener(new PatternLockViewListener() {
            @Override
            public void onStarted() {
                ((GenSecurityActivity)getActivity()).viewPager.enableSwip(false);
                m_patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorD));
            }

            @Override
            public void onProgress(List<PatternLockView.Dot> progressPattern) {
            }

            @Override
            public void onComplete(List<PatternLockView.Dot> pattern) {
                ((GenSecurityActivity)getActivity()).viewPager.enableSwip(true);
                if (btnRedraw.getVisibility() == View.GONE){
                    if (pattern.size() < 4){
                        alertText.setText(R.string.set_app_pattern_alert_least);
                        m_patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorI));
                    }
                    else {
                        alertText.setText(R.string.set_app_pattern_alert_confirm);
                        btnRedraw.setVisibility(View.VISIBLE);
                    }
                }else {
                    if (pattern.size() < 4){
                        alertText.setText(R.string.set_app_pattern_alert_least);
                        m_patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorI));
                    }else if (strPass.equals(PatternLockUtils.patternToString(m_patternLockView, pattern))){
                        btnRedraw.setVisibility(View.GONE);
                        SharedPreferences pres = getContext().getSharedPreferences("Xecure", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pres.edit();
                        SecurityUtils utils = new SecurityUtils(Build.ID);
                        //encryption
                        try {
                            editor.putString("pattern", utils.encrypt(strPass)).commit();
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
                    }else {
                        alertText.setText(R.string.set_app_pattern_alert_not_match);
                        m_patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorI));
                    }
                }
                if (strPass.isEmpty()){
                    strPass = PatternLockUtils.patternToString(m_patternLockView, pattern);
                    m_patternLockView.clearPattern();
                }
            }

            @Override
            public void onCleared() {

            }
        });

        return view;
    }
}
