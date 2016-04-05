/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.oriley.cratesample.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import me.oriley.cratesample.R;

public class TagView extends View {

    private static final int TAG_RED = 0xFFF44336;
    private static final int TAG_GREEN = 0xFF4CAF50;

    @NonNull
    private final Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @NonNull
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    @NonNull
    private final Point mTextPoint = new Point();

    private final int mTagSize;

    private int mCount;

    @NonNull
    private String mCountText = String.valueOf(mCount);

    @Nullable
    private Bitmap mBitmap;

    private boolean mCached;


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
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(mTagSize * 0.4f);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        updateBitmap();
    }


    public void bumpCount() {
        mCount++;
        mCountText = String.valueOf(mCount);
        invalidate();
    }

    public void setCached(boolean cached) {
        if (mCached != cached) {
            mCached = cached;
            updateBitmap();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            int radius = mTagSize / 2;
            mTextPoint.x = w - radius;
            mTextPoint.y = (int) (radius - (mTextPaint.descent() + mTextPaint.ascent()) / 2);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mTagSize > 0 && mBitmap != null && !mBitmap.isRecycled()) {
            canvas.drawBitmap(mBitmap, canvas.getWidth() - mTagSize, 0, mBitmapPaint);
            canvas.drawText(mCountText, mTextPoint.x, mTextPoint.y, mTextPaint);
        }
    }

    private void updateBitmap() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }

        int radius = mTagSize / 2;
        RectF arcRect = new RectF(0, 0, mTagSize, mTagSize);
        Path path = new Path();
        path.reset();
        path.moveTo(0, 0);
        path.lineTo(0, radius);
        path.arcTo(arcRect, -180, -90);
        path.lineTo(mTagSize, mTagSize);
        path.lineTo(mTagSize, 0);
        path.close();

        Paint tagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tagPaint.setColor(mCached ? TAG_GREEN : TAG_RED);
        mBitmap = Bitmap.createBitmap(mTagSize, mTagSize, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(mBitmap);
        canvas.drawPath(path, tagPaint);
        invalidate();
    }
}