package com.XECUREVoIP.security.changeSecurity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.StatusFragment;
import com.XECUREVoIP.security.FragmentViewPagerAdapter;
import com.XECUREVoIP.security.MySwipeableViewPager;
import com.XECUREVoIP.security.createSecurity.GenSecurityActivity;

import java.util.ArrayList;
import java.util.List;

public class ChangeSecurityActivity extends AppCompatActivity implements View.OnClickListener{

    public static MySwipeableViewPager viewPager;
    private List<Fragment> fragmentList;
    private ImageView cancel, back;
    private TextView title;
    private StatusFragment status;
    public static final int CHOOSE_SECURE = 0;
    public static final int CHANGE_PATTERN = 1;
    public static final int CHANGE_PASS = 2;
    public static final int CREATE_PATTERN = 3;
    public static final int CREATE_PASS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pass);

        back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(this);
        cancel = (ImageView) findViewById(R.id.cancel);
        cancel.setOnClickListener(this);
        title = (TextView) findViewById(R.id.title);
        status.enableSideMenu(false);

        findViewById(R.id.id_btnSecure).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChangeSecurityActivity.this, GenSecurityActivity.class);
                intent.putExtra("lock", true);
                startActivity(intent);
                finish();
            }
        });

        if (!isLocked())
            return;

        setEnableBack(false);
        initPageViewer();
    }

    private void initPageViewer() {
        viewPager= (MySwipeableViewPager) findViewById(R.id.container);
        viewPager.enableSwip(false);

        fragmentList=new ArrayList<Fragment>();
        if (isPatten()){
            fragmentList.add(new ChangePattenFragment());
            fragmentList.add(new ChoosePassFragment());
            fragmentList.add(new ConfirmPattenFragment());
            fragmentList.add(new CreatePassFragment());
        }else {
            fragmentList.add(new ChangePassFragment());
            fragmentList.add(new ChoosePassFragment());
            fragmentList.add(new ConfirmPassFragment());
            fragmentList.add(new CreatePatternFragment());
        }

        viewPager.setAdapter(new FragmentViewPagerAdapter(getSupportFragmentManager(), fragmentList));
        viewPager.setCurrentItem(1);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position){
                    case 0:
                        setEnableBack(true);
                        initTitleAndBackButton(false);
                        break;
                    case 1:
                        setEnableBack(false);
                        break;
                    case 2:
                        setEnableBack(true);
                        initTitleAndBackButton(true);
                        break;
                    case 3:
                        setEnableBack(true);
                        initTitleAndBackButton(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.cancel) {
            finish();
        } else if (id == R.id.back) {
            viewPager.setCurrentItem(1);
        }
    }

    public void setEnableBack(boolean enableBack) {
        if (enableBack == false)
            back.setVisibility(View.INVISIBLE);
        else
            back.setVisibility(View.VISIBLE);
    }

    private boolean isPatten() {
        SharedPreferences preXecue = getSharedPreferences("Xecure", MODE_PRIVATE);
        String strCrrEncryptionPass = preXecue.getString("pass", "");

        return strCrrEncryptionPass.isEmpty()? true: false;
    }

    public void initTitleAndBackButton(boolean bBack){
        if (bBack)
            back.setImageResource(R.drawable.back);
        else
            back.setImageResource(R.drawable.forward);
    }

    private boolean isLocked() {
        SharedPreferences preXecue = getSharedPreferences("Xecure", MODE_PRIVATE);
        String strCrrEncryptionPass = preXecue.getString("pass", "");
        String strCrrEncryptionPattern = preXecue.getString("pattern", "");
        if (strCrrEncryptionPass.isEmpty() && strCrrEncryptionPattern.isEmpty())
            return false;

        return true;
    }

    public void updateStatusFragment(StatusFragment fragment) {
        status = fragment;
    }
}
