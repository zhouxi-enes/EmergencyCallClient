package org.enes.lanvideocall.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class MyFAB extends com.robertlevonyan.views.customfloatingactionbutton.FloatingActionButton {


    public MyFAB(Context context) {
        super(context);
    }

    public MyFAB(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyFAB(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setTextSize();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setTextSize();
    }

    @Override
    public void setFabType(int fabType) {
        super.setFabType(fabType);
        setTextSize();
    }

    @Override
    public void setFabSize(int fabSize) {
        super.setFabSize(fabSize);
        setTextSize();
    }

    @Override
    public void setFabText(String fabText) {
        super.setFabText(fabText);
        setTextSize();

    }

    @Override
    public void setFabTextAllCaps(boolean fabTextAllCaps) {
        super.setFabTextAllCaps(fabTextAllCaps);
        setTextSize();
    }

    @Override
    public void setFabTextColor(int fabTextColor) {
        super.setFabTextColor(fabTextColor);
        setTextSize();

    }

    @Override
    public void setFabElevation(float fabElevation) {
        super.setFabElevation(fabElevation);
        setTextSize();

    }

    @Override
    public void setFabColor(int fabColor) {
        super.setFabColor(fabColor);
        setTextSize();
    }

    @Override
    public void setFabIcon(Drawable fabIcon) {
        super.setFabIcon(fabIcon);
        setTextSize();
    }

    @Override
    public void setFabIconColor(int fabIconColor) {
        super.setFabIconColor(fabIconColor);
        setTextSize();
    }

    @Override
    public void setFabIconPosition(int fabIconPosition) {
        super.setFabIconPosition(fabIconPosition);
        setTextSize();
    }

    private void setTextSize() {
        setTextSize(22);
    }
}
