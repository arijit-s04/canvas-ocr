package com.android.arijit.canvas.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.digitalink.Ink;

import java.util.ArrayList;

public class DrawView extends View {
    private final String TAG = "DrawView";
    private static final float TOUCH_TOLERANCE = 4;
    private static final int ARM_LENGTH = 500;
    private float mX, mY, cX, cY;
    private Path mPath;
    private Ink.Builder inkBuilder = Ink.builder();
    private Ink.Stroke.Builder strokeBuilder;
    private int mMode = 0;
    private float mRadius = 0f;
    private boolean settingCenter=false;
    private Paint mPaint;
    private Path centPoint, pencil;
    private Paint cenPaint, penPaint;
    private double comTheta = 0;

    private ArrayList<Stroke> paths = new ArrayList<>();

    private int currentColor;
    private int strokeWidth;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();

        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAlpha(0xff);

        cenPaint = new Paint(mPaint);
        cenPaint.setStrokeWidth(5);
        cenPaint.setColor(Color.RED);
        cX = cY = 500f;

        penPaint = new Paint(mPaint);
        penPaint.setColor(Color.BLUE);
        penPaint.setStrokeWidth(8);
    }

    public void init(int height, int width) {

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = Color.BLACK;

        strokeWidth = 12;
    }

    public int getmMode() {
        return mMode;
    }

    public void setmMode(int mMode) {
        this.mMode = mMode;
    }

    public boolean isSettingCenter() {
        return settingCenter;
    }

    public void setSettingCenter(@Nullable Boolean set) {
        if(set!=null){
            this.settingCenter = set.booleanValue();
        }
        else {
            this.settingCenter = !this.settingCenter;
            if (this.settingCenter) {
                centPoint = new Path();
                createCross(cX, cY);
                pencil = new Path();
                float[] res = getArmPoint();
                createPencil(res[0],res[1]);
            }
            else if (mMode != 1) {
                centPoint.reset();
                comTheta = 0;
            }
        }
        invalidate();
    }

    public float getmRadius() {
        return mRadius;
    }

    public void setmRadius(float mRadius) {
        this.mRadius = mRadius;
        pencil = new Path();
        float[] res = getArmPoint();
        createPencil(res[0],res[1]);
        invalidate();
    }

    public void setColor(int color) {
        currentColor = color;
    }

    public void setStrokeWidth(int width) {
        strokeWidth = width;
    }

    public void undo() {
        if (paths.size() != 0) {
            paths.remove(paths.size() - 1);
            invalidate();
        }
    }

    public void clear() {
        paths.clear();
        for (Stroke fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mCanvas.drawPath(fp.path, mPaint);
        }
        if(centPoint!=null)
            centPoint.reset();
        inkBuilder = Ink.builder();
        strokeBuilder = null;
        invalidate();
    }

    public Bitmap save() {
        return mBitmap;
    }
    public Ink getInk(){
        return inkBuilder.build();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        int backgroundColor = Color.WHITE;
        mCanvas.drawColor(backgroundColor);

        for (Stroke fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mCanvas.drawPath(fp.path, mPaint);
        }
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        if(centPoint!=null && (settingCenter || mMode == 1)) {
            mCanvas.drawPath(pencil, penPaint);
            mCanvas.drawPath(centPoint, cenPaint);
        }

        canvas.restore();
    }


    private void touchStart(float x, float y) {
        mPath = new Path();
        Stroke fp = new Stroke(currentColor, strokeWidth, mPath);
        paths.add(fp);

        mPath.reset();

        mPath.moveTo(x, y);

        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
        if(mMode == 1){
            comTheta = Math.atan((y-cY)/(x-cX));
            //check 2nd quad o 3rd
            if(x<cX){
                comTheta += Math.PI;
            }
            pencil.reset();
            createPencil(x,y);
        }
    }
    private void touchUp() {
        mPath.lineTo(mX, mY);
    }

    private void dragCenter(float x, float y){
        cX = x;
        cY = y;
        centPoint.reset();
        createCross(cX, cY);
        pencil.reset();
        float[] res = getArmPoint();
        createPencil(res[0],res[1]);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        long t = System.currentTimeMillis();
        if(settingCenter){
            switch (event.getAction()){
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    dragCenter(x,y);
                    invalidate();
                    break;
            }

            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(mMode == 1){
                    float[] result = getCorrespondingPoint(x, y);
                    x = result[0];
                    y = result[1];
                }
                else {
                    strokeBuilder = Ink.Stroke.builder();
                    strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                }
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if(mMode == 1){
                    float[] res = getCorrespondingPoint(x, y);
                    x = res[0];y = res[1];
                }
                else
                    strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if(mMode == 1){}
                else {
                    strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                    inkBuilder.addStroke(strokeBuilder.build());
                    strokeBuilder = null;
                }
                touchUp();
                invalidate();
                break;
        }
        return true;
    }

    private float[] getCorrespondingPoint(float x, float y){
        float modV = (float) Math.sqrt(((x-cX)*(x-cX) + (y-cY)*(y-cY)));
        float result[] = new float[2];
        result[0] = ((mRadius*x - mRadius*cX)/modV) + cX;
        result[1] = ((mRadius*y - mRadius*cY)/modV) + cY;
        return result;
    }

    private double distanceBetween(float x, float y, float mx, float my){
        double res;
        double A = y - my;
        double B = mx - x;
        double C = y - x*(y - my)*(x - mx);

        res = Math.abs(A*cX + B*cY+ C);
        res = res / (Math.sqrt(A*A + B*B));

        return res;
    }
    private double d2p(float x1, float y1, float x2 , float y2){
        return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    }

    private void createCross(float x, float y){
        centPoint.moveTo(x-10,y-10);
        centPoint.lineTo(x+10, y+10);
        centPoint.moveTo(x-10, y+10);
        centPoint.lineTo(x+10, y-10);
    }

    private void createPencil(float x, float y){
//        pencil.moveTo(x,y);
//        pencil.lineTo(x-40, y+60);
//        pencil.moveTo(x-40, y+60);
//        pencil.lineTo(x-40,y+250);
//        pencil.moveTo(x-40,y+250);
//        pencil.lineTo(x+40, y+250);
//        pencil.moveTo(x+40, y+250);
//        pencil.lineTo(x+40, y+60);
//        pencil.moveTo(x+40, y+60);
//        pencil.lineTo(x,y);
        double h = Math.sqrt(ARM_LENGTH*ARM_LENGTH - ((mRadius*mRadius)/4));
        double modP = Math.sqrt((y-cY)*(y-cY) + (x-cX)*(x-cX))/2;
        double topX = ((cY-y)*h*-1)/(2*modP) + (x+cX)/2;
        double topY = ((x-cX)*h*-1)/(2*modP) + (y+cY)/2;
        pencil.moveTo((float) topX, (float) topY);
        pencil.lineTo(cX, cY);
        pencil.moveTo((float) topX, (float) topY);
        pencil.lineTo(x,y);
    }

    private float[] getArmPoint(){
        float[] result = new float[2];
        result[0] = (float) (cX + mRadius*Math.cos(comTheta));
        result[1] = (float) (cY + mRadius*Math.sin(comTheta));

        return result;
    }

}
