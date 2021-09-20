package com.android.arijit.canvas.ocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.slider.RangeSlider;

public class TotalView extends LinearLayout implements OnCompassActionCallback {
    Context context;
    View mRangeSlider;
    CompassView mCompassView;
    LinearLayout ll;
    private float curHX = 0, curHY = 0;
    private RangeSlider rs;
    public static final int HEIGHT = 2000;
    public static final int WIDTH = 2000;
    public static float TX =-1, TY =-1;
    OnCompassDraw drawListener;

    public TotalView(Context context) {
        super(context);
        init(context);
    }

    public TotalView(MainActivity context, OnCompassDraw paint) {
        super(context);
        this.drawListener = paint;
        init(context);
    }

    public TotalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TotalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public TotalView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    void init(Context context){
        this.context = context;
        mRangeSlider = LayoutInflater.from(this.context).inflate(R.layout.layout_slider, this);
        ll = mRangeSlider.findViewById(R.id.ll);
        this.mCompassView = new CompassView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int)CompassView.WIDTH, (int)CompassView.HEIGHT);
//        lp.gravity = Gravity.CENTER;
        ll.addView(mCompassView, lp);
        this.mCompassView.setOnCompassCallbackListener(this);

        rs = mRangeSlider.findViewById(R.id.radius_slider_embed);

        rs.addOnChangeListener((slider, value, fromUser) -> {
            mCompassView.setCurRadius(value);
//            mCompassView.setScaleX(value);
            mCompassView.invalidate();
        });

    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if(ll == null)
            super.addView(child, index, params);
        else
            ll.addView(child, params);
    }

    @Override
    public void onDown(float x, float y) {
        calculatePosition();
        drawListener.onCompassDrawStart(x+curHX, y+curHY);
    }

    @Override
    public void onMove(float dx, float dy, float theta) {
        drawListener.onCompassDrawMove(curHX+dx, curHY+dy);
    }

    @Override
    public void onUp(float dx, float dy, float theta) {
        drawListener.onCompassDrawUp(curHX+dx, curHY+dy);
    }

    @Override
    public void onViewMove(float dx, float dy) {
        if(TX == -1 && TY == -1){
            TX = getX();
            TY = getY();
        }
        CompassView.adjustViewTranslation(this, dx, dy);
    }

    private void calculatePosition(){
        if(TX == -1){
            TX = getX();
            TY = getY();
        }
        float[] values = new float[9];
        Matrix mat = this.getMatrix();
        mat.getValues(values);
        float dX = values[Matrix.MTRANS_X];
        float dY = values[Matrix.MTRANS_Y];
        curHX = TX + dX;
        curHY = TY + dY;
    }

}
