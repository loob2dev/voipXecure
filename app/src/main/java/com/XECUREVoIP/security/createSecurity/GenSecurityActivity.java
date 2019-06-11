package com.XECUREVoIP.security.createSecurity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.security.FragmentViewPagerAdapter;
import com.XECUREVoIP.security.MySwipeableViewPager;

import java.util.ArrayList;
import java.util.List;

public class GenSecurityActivity extends FragmentActivity {

    public MySwipeableViewPager viewPager;
    private List<Fragment> fragmentList;
    private TextView tvTab1,tvTab2;
    private int currIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        setContentView(R.layout.activity_gen_pass);
        initView();
        initViewPager();
    }

    private void initView(){
        tvTab1= (TextView) findViewById(R.id.tv_tab1);
        tvTab2= (TextView) findViewById(R.id.tv_tab2);

        tvTab1.setOnClickListener(new tvOnClickListener(0));
        tvTab2.setOnClickListener(new tvOnClickListener(1));
    }

    private class tvOnClickListener implements View.OnClickListener{

        private int index=0;

        public tvOnClickListener(int index){
            this.index=index;
        }

        @Override
        public void onClick(View view) {
            viewPager.setCurrentItem(index);
        }
    }

    private void initViewPager(){
        viewPager= (MySwipeableViewPager) findViewById(R.id.viewpager);
        fragmentList=new ArrayList<Fragment>();
        fragmentList.add(new GenPassFragment());
        fragmentList.add(new GenPattenFragment());

        viewPager.setAdapter(new FragmentViewPagerAdapter(getSupportFragmentManager(), fragmentList));
        viewPager.setCurrentItem(0);
        tvTab1.setTextColor(getResources().getColor(R.color.colorN));
        viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
    }


    private class MyOnPageChangeListener implements ViewPager.OnPageChangeListener{

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            //hide keyboard
            hideKeyboard();
            //change the fragment
            currIndex=position;
            switch (position){
                case 0:
                    tvTab1.setTextColor(getResources().getColor(R.color.colorN));
                    tvTab2.setTextColor(getResources().getColor(R.color.colorH));
                    break;
                case 1:
                    tvTab1.setTextColor(getResources().getColor(R.color.colorH));
                    tvTab2.setTextColor(getResources().getColor(R.color.colorN));
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = this.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
