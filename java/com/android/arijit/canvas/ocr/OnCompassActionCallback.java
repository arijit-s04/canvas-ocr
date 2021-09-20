package com.android.arijit.canvas.ocr;

public interface OnCompassActionCallback {
    void onDown(float dx, float dy);
    void onMove(float dx, float dy, float theta);
    void onUp(float dx, float dy, float theta);
    void onViewMove(float dx, float dy);
}
