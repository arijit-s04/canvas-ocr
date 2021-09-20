package com.android.arijit.canvas.ocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.provider.Contacts;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CompassView extends View {

    float DP_8;
    Context context;
    Paint mPaint;
    float curX=0,curY=0;
    float centerX=WIDTH/2, centerY=HEIGHT/2;
    float DEFAULT_RADIUS;
    float pencil_width = 60, pencil_height = 120;
    private Path legs, pencil, handle, nonPencil;
    public static final float WIDTH = 1200, HEIGHT = 1200;
    float curRadius, theta = 0;
    private float CX=-1, CY=-1;
    private final static float ARM_LENGTH = HEIGHT/2f;
    Region pencilRegion, handleRegion, nonPencilRegion;

    boolean move = false, draw = false, nonDraw = false;

    OnCompassActionCallback listener;

    public CompassView(Context context) {
        super(context);
        init(context);
    }

    public CompassView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CompassView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    void init(Context context){
        this.context = context;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(8f);
        DP_8 = pxFromDp(context, 8);

        legs = new Path();
        handle = new Path();
        pencil = new Path();
        nonPencil = new Path();
        curRadius = DEFAULT_RADIUS = DP_8/8f;
        pencilRegion = new Region();
        handleRegion = new Region();
        nonPencilRegion = new Region();
        createPencil(centerX+curRadius, centerY);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(Color.BLUE);
        canvas.drawPath(legs, mPaint);
        canvas.drawPath(pencil, mPaint);
        mPaint.setColor(context.getColor(android.R.color.holo_green_light));
        canvas.drawPath(handle, mPaint);
        mPaint.setColor(Color.RED);
        canvas.drawPath(nonPencil, mPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(CX == -1 && CY == -1){
            CX = getX();CY = getY();
        }
        float x = event.getX(0), y = event.getY(0);
        if(event.getActionMasked() == MotionEvent.ACTION_DOWN){
            curX = x;
            curY = y;
            if(pencilRegion.contains((int) x, (int) y)) {
                draw = true;
                float[] res = getCorrespondingPoint(x, y);
                theta = (float) Math.atan((res[1]-centerY)/(res[0]-centerX));
                //check 2nd quad or 3rd
                if(res[0]<centerX){
                    theta += Math.PI;
                }
                listener.onDown(res[0]+CX, res[1]+CY);
                return true;
            }
            else if( handleRegion.contains((int) x, (int) y) ) {
                move = true;
                return true;
            }
            else if(  nonPencilRegion.contains((int) x, (int) y)){
                nonDraw = true;

                float[] res = getCorrespondingPoint(x, y);
                theta = (float) Math.atan((res[1]-centerY)/(res[0]-centerX));
                //check 2nd quad or 3rd
                if(res[0]<centerX){
                    theta += Math.PI;
                }
                return true;
            }
            return false;
        }
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_MOVE:
                if(move){
                    listener.onViewMove((x - curX), (y-curY));
                }
                else if(draw || nonDraw){
                    float[] res = getCorrespondingPoint(x, y);
                    theta = (float) Math.atan((res[1]-centerY)/(res[0]-centerX));
                    //check 2nd quad or 3rd
                    if(res[0]<centerX){
                        theta += Math.PI;
                    }
                    legs.reset();
                    createPencil(res[0],res[1]);
                    if(draw)
                        listener.onMove(res[0]+CX, res[1]+CY, 0);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if(draw)
                    listener.onUp(0,0,0);
                nonDraw = move = draw = false;
                break;
        }
        return true;
    }

    public static void adjustViewTranslation(View view, float deltaX, float deltaY) {
        float[] deltaVector = {deltaX, deltaY};
        view.getMatrix().mapVectors(deltaVector);
        float translateX = view.getTranslationX() + deltaVector[0];
        float translateY = view.getTranslationY() + deltaVector[1];
        view.setTranslationX(translateX);
        view.setTranslationY(translateY);
    }

    public void setOnCompassCallbackListener(OnCompassActionCallback listener){
        this.listener = listener;
    }

    public static float pxFromDp(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    private void pencilStructure(float x, float y){
        pencil.reset();
        pencil.moveTo(x,y);
        pencil.lineTo(x-pencil_width/(2*getScaleX()), y-pencil_height/4);
        pencil.lineTo(x-pencil_width/(2*getScaleX()),y-pencil_height);
        pencil.lineTo(x+pencil_width/(2*getScaleX()), y-pencil_height);
        pencil.lineTo(x+pencil_width/(2*getScaleX()), y-pencil_height/4);
        pencil.lineTo(x,y);
    }

    private boolean isInPencil(float x, float y, float X, float Y) {
        return ( x>= X-pencil_width/2 && x <= X+pencil_width/2
                && y>= Y-pencil_height && y<= Y );
    }

    public void setCurRadius(float value) {
        this.curRadius = value*DEFAULT_RADIUS;
        float[] rt = getArmPoint();
        createPencil(rt[0], rt[1]);
    }

    private float[] getArmPoint(){
        float[] result = new float[2];
        result[0] = (float) (centerX + curRadius*Math.cos(theta));
        result[1] = (float) (centerY + curRadius*Math.sin(theta));

        return result;
    }

    private float[] getCorrespondingPoint(float x, float y){
        float modV = (float) Math.sqrt(((x-centerX)*(x-centerX) + (y-centerY)*(y-centerY)));
        float[] result = new float[2];
        result[0] = ((curRadius*x - curRadius*centerX)/modV) + centerX;
        result[1] = ((curRadius*y - curRadius*centerY)/modV) + centerY;
        return result;
    }

    private void createPencil(float x, float y){
        legs.reset();
        double h = Math.sqrt(ARM_LENGTH*ARM_LENGTH - ((curRadius*curRadius)/4));
        double modP = Math.sqrt((y-centerY)*(y-centerY) + (x-centerX)*(x-centerX))/2;
        double topX = ((centerY-y)*h*-1)/(2*modP) + (x+centerX)/2;
        double topY = ((x-centerX)*h*-1)/(2*modP) + (y+centerY)/2;
        legs.moveTo((float) topX, (float) topY);
        legs.lineTo(centerX, centerY);
        legs.moveTo((float) topX, (float) topY);
        legs.lineTo(x,y);
           //\\
          //==\\
         //====\\
        //======\\
        handle.reset();
        float tmpx,tmpy;
        float hanWidth = 3*DP_8;
        tmpx = (float) (topX - hanWidth*Math.cos(theta)/2);
        tmpy = (float) (topY - hanWidth*Math.sin(theta)/2);
        handle.moveTo(tmpx, tmpy);
        tmpx -= hanWidth*Math.sin(theta);
        tmpy += hanWidth*Math.cos(theta);
        handle.lineTo(tmpx, tmpy);
        tmpx += hanWidth*Math.cos(theta);
        tmpy += hanWidth*Math.sin(theta);
        handle.lineTo(tmpx, tmpy);
        tmpx += hanWidth*Math.sin(theta);
        tmpy -= hanWidth*Math.cos(theta);
        handle.lineTo(tmpx, tmpy );
        handle.close();
        setRegionFromPath(handle, handleRegion);
           //\\
          //==\\
         //====\\
        //======\\
        float nonPX, nonPY;
        pencil.reset();
        tmpx = (float) (x - pencil_width*Math.cos(theta)/2);
        tmpy = (float) (y - pencil_width*Math.sin(theta)/2);
        pencil.moveTo(tmpx, tmpy);
        tmpx += pencil_height*Math.sin(theta);
        tmpy -= pencil_height*Math.cos(theta);
        nonPX = tmpx;nonPY = tmpy;
        pencil.lineTo(tmpx, tmpy);
        tmpx += pencil_width*Math.cos(theta);
        tmpy += pencil_width*Math.sin(theta);
        pencil.lineTo(tmpx, tmpy);
        tmpx -= pencil_height*Math.sin(theta);
        tmpy += pencil_height*Math.cos(theta);
        pencil.lineTo(tmpx, tmpy );
        pencil.close();
        setRegionFromPath(pencil, pencilRegion);
           //\\
          //==\\
         //====\\
        //======\\
        nonPencil.reset();
        nonPencil.moveTo(nonPX, nonPY);
        nonPX += pencil_height*Math.sin(theta);
        nonPY -= pencil_height*Math.cos(theta);
        nonPX = nonPX;nonPY = nonPY;
        nonPencil.lineTo(nonPX, nonPY);
        nonPX += pencil_width*Math.cos(theta);
        nonPY += pencil_width*Math.sin(theta);
        nonPencil.lineTo(nonPX, nonPY);
        nonPX -= pencil_height*Math.sin(theta);
        nonPY += pencil_height*Math.cos(theta);
        nonPencil.lineTo(nonPX, nonPY );
        nonPencil.close();
        setRegionFromPath(nonPencil, nonPencilRegion);
    }

    private float calculateAngle(float x, float y) {
        float tanT1 = (y - centerY)/(x- centerX);
        float tanT2 = (curY - centerY)/(curX- centerX);
        float tanT1_T2 = (tanT1 - tanT2)/(1 + tanT1*tanT2);
        double theta = (float) Math.atan(tanT1_T2);
        return (float) Math.toDegrees(theta);
    }

    private void calculatePosition(){
        float[] values = new float[9];
        Matrix mat = this.getMatrix();
        mat.getValues(values);
        float dX = values[Matrix.MTRANS_X];
        float dY = values[Matrix.MTRANS_Y];
        curX = CX + dX;
        curY = CY + dY;

    }


    private void setRegionFromPath(Path mPath, Region mRegion) {
        RectF rectF = new RectF();
        mPath.computeBounds(rectF, true);
        Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int)rectF.right, (int)rectF.bottom);
        mRegion.setPath(mPath, new Region(rect));
    }


}
