package me.oriley.cratesample.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import me.oriley.cratesample.R;

public class TagView extends View {

    @NonNull
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @NonNull
    private final Path mPath = new Path();

    private final int mTagSize;

    @ColorInt
    private int mTagColor = Color.TRANSPARENT;


    public TagView(@NonNull Context context) {
        this(context, null);
    }

    public TagView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        int tagSize = 0;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TagView);
            tagSize = a.getDimensionPixelSize(R.styleable.TagView_tagSize, 0);
            a.recycle();
        }

        mTagSize = tagSize;
    }


    public void setTagColor(@ColorInt int color) {
        if (mTagColor != color) {
            mTagColor = color;
            mPaint.setColor(mTagColor);
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            mPath.reset();
            mPath.moveTo(w - mTagSize, 0);
            mPath.lineTo(w, 0);
            mPath.lineTo(w, mTagSize);
            mPath.lineTo(w - mTagSize, 0);
            mPath.close();
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mTagColor != Color.TRANSPARENT && mTagSize > 0) {
            canvas.drawPath(mPath, mPaint);
        }
    }
}