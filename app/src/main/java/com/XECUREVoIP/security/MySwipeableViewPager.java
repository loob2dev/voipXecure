package com.XECUREVoIP.security;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.lang.reflect.Field;

public class MySwipeableViewPager extends ViewPager {
    private boolean b_Swip = true;

    public void enableSwip(Boolean b){
        b_Swip = b;
    }

    public MySwipeableViewPager(Context context) {
        super(context);
    }

    public MySwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return b_Swip? super.onInterceptTouchEvent(event) : false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return b_Swip ? super.onTouchEvent(event) : false;
    }
}
