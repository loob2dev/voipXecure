package com.XECUREVoIP.security;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.XECUREVoIP.R;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecurePreferences;
import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;

import java.util.List;

public class LoginWithPatternActivity extends AppCompatActivity {

    PatternLockView m_patternLockView;
    private TextView m_txtTime;

    private boolean isForResult;

    final private int iDelay02 = 24 * 60 * 60;
    final private int iDelay01 = 5 * 60;
    final private int iDelay00 = 2 * 60;

    public int iAttempt = 0;
    private int iSecond = 0;
    SharedPreferences m_prefDelay = null;
    SharedPreferences m_preXecue = null;
    
    private String m_strCrrEncryptionPass = "";
    private String m_strPass = "";

    private static final int MINUTES_IN_AN_HOUR = 60;
    private static final int SECONDS_IN_A_MINUTE = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_with_patten);

        m_txtTime = (TextView)findViewById(R.id.alertText);

        m_prefDelay = getSharedPreferences("DelayTime", MODE_PRIVATE);

        iAttempt = m_prefDelay.getInt("attempt", 0);
        iSecond = m_prefDelay.getInt("delay", 0);

        m_preXecue = getSharedPreferences("Xecure", MODE_PRIVATE);
        m_strCrrEncryptionPass = m_preXecue.getString("pattern", "");

        m_patternLockView = findViewById(R.id.pattern_lock_view);
        m_patternLockView.addPatternLockListener(new PatternLockViewListener() {
            @Override
            public void onStarted() {
                m_patternLockView.setCorrectStateColor(getResources().getColor(R.color.colorD));
            }

            @Override
            public void onProgress(List<PatternLockView.Dot> progressPattern) {

            }

            @Override
            public void onComplete(List<PatternLockView.Dot> pattern) {
                if (pattern.size() < 4){
                    m_txtTime.setText(R.string.set_app_pattern_alert_least);
                    m_txtTime.setTextColor(getResources().getColor(R.color.colorP));
                } else {
                    SecurityUtils utils = new SecurityUtils(Build.ID);
                    SharedPreferences.Editor editor = m_prefDelay.edit();
                    try {
                        //decryption
                        String strCrrDecpass = utils.decrypt(m_strCrrEncryptionPass);
                        m_strPass = PatternLockUtils.patternToString(m_patternLockView, pattern);
                        if (strCrrDecpass.equals(m_strPass)) {
                            iAttempt = 0;
                            editor.clear().commit();
                            XecurePreferences.instance().firstLaunchSuccessful();
                            if (isForResult)
                                setResult(RESULT_OK);
                            else
                                startActivity(new Intent(LoginWithPatternActivity.this, XecureActivity.class));
                            finish();
                        }else {
                            m_txtTime.setText(R.string.sign_app_pattern_incorrect);
                            m_txtTime.setTextColor(getResources().getColor(R.color.colorP));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        m_txtTime.setText(R.string.sign_app_pattern_incorrect);
                        m_txtTime.setTextColor(getResources().getColor(R.color.colorP));
                    }
                    editor.putInt("attempt", ++iAttempt).commit();
                    iSecond = 0;
                }
                m_patternLockView.clearPattern();
                new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        alertNotification();
                    }
                }.sendEmptyMessageDelayed(0, 1000);
            }

            @Override
            public void onCleared() {
            }
        });
        alertNotification();
    }

    private void alertNotification() {
        if (iAttempt == 4) {
            showDelayTime("XECURE is locked for ", iDelay00);
            return;
        } else if (iAttempt == 8) {
            showDelayTime("XECURE is locked for ", iDelay01);
            return;
        } else if (iAttempt == 10) {
            showDelayTime("XECURE is locked for ", iDelay02);
            return;
        } else if (iAttempt > 20){
            m_prefDelay.edit().putInt("attempt", iAttempt).commit();
            setResult(RESULT_CANCELED);
            Toast.makeText(this, R.string.illegal_user, Toast.LENGTH_SHORT).show();
            SharedPreferences.Editor editor = m_preXecue.edit();
            editor.putBoolean(getResources().getString(R.string.illegal_user), true).commit();
            finish();
        }
    }

    private void showDelayTime(final String strWarning , final int iDelay) {
        final SharedPreferences.Editor editor = m_prefDelay.edit();
        new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (++iSecond < iDelay){
                    String strTime = timeConversion(iDelay - iSecond);
                    m_txtTime.setTextColor(getResources().getColor(R.color.colorP));
                    m_txtTime.setText(strWarning + strTime);
                    sendEmptyMessageDelayed(0,1000);
                    editor.putInt("delay", iSecond).commit();
                    m_patternLockView.setInputEnabled(false);
                    m_patternLockView.clearPattern();
                }
                else {
                    m_txtTime.setTextColor(getResources().getColor(R.color.colorC));
                    m_txtTime.setText(R.string.set_app_pattern_notify);
                    editor.putInt("delay", iSecond).commit();
                    m_patternLockView.setInputEnabled(true);
                }
            }
        }.sendEmptyMessageDelayed(0, 1000);
    }

    private static String timeConversion(int totalSeconds) {
        int hours = totalSeconds / MINUTES_IN_AN_HOUR / SECONDS_IN_A_MINUTE;
        int minutes = (totalSeconds - (hoursToSeconds(hours)))
                / SECONDS_IN_A_MINUTE;
        int seconds = totalSeconds
                - ((hoursToSeconds(hours)) + (minutesToSeconds(minutes)));
        if (hours == 0 && minutes == 0)
            return seconds + " s";
        if (hours == 0)
            return minutes + " m " + seconds + " s";
        return hours + " h " + minutes + " m " + seconds + " s";
    }

    private static int hoursToSeconds(int hours) {
        return hours * MINUTES_IN_AN_HOUR * SECONDS_IN_A_MINUTE;
    }

    private static int minutesToSeconds(int minutes) {
        return minutes * SECONDS_IN_A_MINUTE;
    }

}
