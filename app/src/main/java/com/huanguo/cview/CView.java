package com.huanguo.cview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CView extends FrameLayout {


    public CView(@NonNull Context context) {
        super(context);
    }

    public CView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
