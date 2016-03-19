package me.oriley.cratesample.widget;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.util.AttributeSet;

public class FlatAppBarLayout extends AppBarLayout {

    public FlatAppBarLayout(Context context) {
        super(context);
    }

    public FlatAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public final void setElevation(float elevation) {
        super.setElevation(0);
    }
}
