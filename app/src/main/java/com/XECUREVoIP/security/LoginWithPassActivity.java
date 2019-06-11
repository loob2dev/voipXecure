package com.XECUREVoIP.security;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.XECUREVoIP.R;
import com.XECUREVoIP.XecureActivity;
import com.XECUREVoIP.XecurePreferences;


public class LoginWithPassActivity extends Activity implements View.OnClickListener {
    private Button m_btnLogin;
    private EditText m_edtPass;
    private TextView m_txtTime;

    private boolean isForResult;

    public int iAttempt = 0;
    private int iSecond = 0;
    SharedPreferences m_prefDelay = null;
    SharedPreferences m_preXecue = null;

    final private int iDelay02 = 24 * 60 * 60;
    final private int iDelay01 = 5 * 60;
    final private int iDelay00 = 2 * 60;

    private static final int MINUTES_IN_AN_HOUR = 60;
    private static final int SECONDS_IN_A_MINUTE = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_app);

        isForResult = getIntent().getBooleanExtra(getResources().getString(R.string.for_result), false);

        m_txtTime = (TextView)findViewById(R.id.txtTime);

        m_prefDelay = getSharedPreferences("DelayTime", MODE_PRIVATE);
        m_btnLogin = (Button) findViewById(R.id.id_btnLogin);

        m_edtPass = (EditText)findViewById(R.id.id_edtPass);
        m_btnLogin.setOnClickListener(this);

        iAttempt = m_prefDelay.getInt("attempt", 0);
        iSecond = m_prefDelay.getInt("delay", 0);

        alertNotification();
    }

    @Override
    public void onClick(View v) {
        iSecond = 0;
        SharedPreferences.Editor editor = m_prefDelay.edit();
        m_preXecue = getSharedPreferences("Xecure", MODE_PRIVATE);
        String strCrrEncryptionPass = m_preXecue.getString("pass", "");
        SecurityUtils utils = new SecurityUtils(Build.ID);
        try {
            //decryption
            String strCrrDecpass = utils.decrypt(strCrrEncryptionPass);
            String strPass = m_edtPass.getText().toString();
            if (strCrrDecpass.equals(strPass)) {
                iAttempt = 0;
                editor.clear().commit();
                XecurePreferences.instance().firstLaunchSuccessful();
                if (isForResult)
                    setResult(RESULT_OK);
                else
                    startActivity(new Intent(LoginWithPassActivity.this, XecureActivity.class));
                finish();
            } else {
                m_edtPass.setError("Incorrect password");
            }
        } catch (Exception e) {
            e.printStackTrace();
            m_edtPass.setError("Incorrect password");
        }
        editor.putInt("attempt", ++iAttempt).commit();
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
            m_btnLogin.setVisibility(View.VISIBLE);
            m_prefDelay.edit().putInt("attempt", iAttempt).commit();
            Toast.makeText(this, R.string.illegal_user, Toast.LENGTH_SHORT).show();
            SharedPreferences.Editor editor = m_preXecue.edit();
            editor.putBoolean(getResources().getString(R.string.illegal_user), true).commit();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void showDelayTime(final String strWarning , final int iDelay) {
        m_txtTime.setVisibility(View.VISIBLE);
        m_btnLogin.setVisibility(View.GONE);
        m_edtPass.setText("");
        m_edtPass.setError(null);
        m_edtPass.invalidate();
        final SharedPreferences.Editor editor = m_prefDelay.edit();
        new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (++iSecond < iDelay){
                    String strTime = timeConversion(iDelay - iSecond);
                    m_txtTime.setText(strWarning + strTime);
                    sendEmptyMessageDelayed(0,1000);
                    editor.putInt("delay", iSecond).commit();
                }
                else {
                    m_txtTime.setText("");
                    m_txtTime.setVisibility(View.GONE);
                    m_btnLogin.setVisibility(View.VISIBLE);
                    editor.putInt("delay", iSecond).commit();
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
