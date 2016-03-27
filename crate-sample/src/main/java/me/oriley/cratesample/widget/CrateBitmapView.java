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
import android.graphics.Bitmap;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import butterknife.Bind;
import butterknife.ButterKnife;
import me.oriley.cratesample.R;

public class CrateBitmapView extends CardView {

    @Bind(R.id.image_view)
    ImageView mImageView;

    @Bind(R.id.tag_view)
    TagView mTagView;


    public CrateBitmapView(@NonNull Context context) {
        this(context, null);
    }

    public CrateBitmapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CrateBitmapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.crate_bitmap_view, this);
        ButterKnife.bind(this);
    }


    @NonNull
    public ViewPropertyAnimator animateBitmap() {
        return mImageView.animate();
    }

    public void setBitmap(@Nullable Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
    }

    public void setBitmapRotation(float rotation) {
        mImageView.setRotation(rotation);
    }

    public void setBitmapScale(float scaleX, float scaleY) {
        mImageView.setScaleX(scaleX);
        mImageView.setScaleY(scaleY);
    }

    public void setTagColor(@ColorInt int color) {
        mTagView.setTagColor(color);
    }
}
