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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import me.oriley.crate.ImageAsset;
import me.oriley.cratesample.R;

public class CrateBitmapView extends TaggedCardView<ImageAsset> {

    private static final long ANIM_DURATION_MILLIS = 300;

    @NonNull
    private final ImageView mImageView;


    public CrateBitmapView(@NonNull Context context) {
        this(context, null);
    }

    public CrateBitmapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CrateBitmapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(R.layout.crate_bitmap_view);
        mImageView = (ImageView) getItemView();
        setSquare(true);
    }


    @Override
    public void animateCard() {
        getAnimator()
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .setDuration(ANIM_DURATION_MILLIS);

        getItemAnimator()
                .alpha(1f)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(ANIM_DURATION_MILLIS);
    }

    @Override
    public void clearCardAnimation() {
        clearAnimator();
        clearItemAnimator();

        mImageView.setAlpha(0f);
        setScaleX(0.85f);
        setScaleY(0.85f);
    }

    @Override
    public void initialise(@Nullable ImageAsset asset) {
        super.initialise(asset);
        mImageView.setImageBitmap(null);
        clearCardAnimation();
    }

    public void setBitmap(@Nullable Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
    }
}
