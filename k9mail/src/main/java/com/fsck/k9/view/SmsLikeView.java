package com.fsck.k9.view;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


public class SmsLikeView extends SlidingPaneLayout implements SlidingPaneLayout.PanelSlideListener{

    public SmsLikeView(Context context){
        super(context);
        setPanelSlideListener(this);
    }

    public SmsLikeView(Context context,AttributeSet attrs) {
        super(context,attrs);
        init();
        setPanelSlideListener(this);
    }

    public SmsLikeView(Context context,AttributeSet attrs,int defStyle) {
        super(context,attrs,defStyle);
        init();
        setPanelSlideListener(this);
    }

    private void init() {
        setSliderFadeColor(getResources().getColor(android.R.color.transparent));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void onPanelSlide(View panel,float slideOffset) {
            Log.e("Menu offset ", String.valueOf(slideOffset));
    }

    @Override
    public void onPanelOpened(View panel) {
            Log.e("Menu open","yes");
    }

    @Override
    public void onPanelClosed(View panel) {
            Log.e("Menu open","no");
    }
}
