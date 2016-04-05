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
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;
import butterknife.Bind;
import butterknife.ButterKnife;
import me.oriley.crate.Asset;
import me.oriley.cratesample.BuildConfig;
import me.oriley.cratesample.R;

@SuppressWarnings("unused")
public abstract class TaggedCardView<T extends Asset> extends CardView {

    @Nullable
    @Bind(R.id.tagged_card_tag)
    TagView mTagView;

    @Nullable
    @Bind(R.id.tagged_card_item)
    View mItemView;

    @Nullable
    private ViewPropertyAnimator mAnimator;

    @Nullable
    private ViewPropertyAnimator mItemAnimator;

    private boolean mSquare;


    public TaggedCardView(@NonNull Context context) {
        this(context, null);
    }

    public TaggedCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaggedCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TaggedCardView);
            mSquare = a.getBoolean(R.styleable.TaggedCardView_forceSquare, false);
            a.recycle();
        }
    }


    final void inflate(@LayoutRes int layoutRes) {
        View.inflate(getContext(), layoutRes, this);
        ButterKnife.bind(this);

        if (!BuildConfig.DEBUG && mTagView != null) {
            mTagView.setVisibility(GONE);
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //noinspection SuspiciousNameCombination
        super.onMeasure(widthMeasureSpec, mSquare ? widthMeasureSpec : heightMeasureSpec);
    }

    @NonNull
    public ViewPropertyAnimator getAnimator() {
        clearAnimator();
        if (mAnimator == null) {
            mAnimator = animate();
        }
        return mAnimator;
    }

    public void clearAnimator() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
    }

    @NonNull
    public ViewPropertyAnimator getItemAnimator() {
        clearItemAnimator();
        if (mItemAnimator == null) {
            mItemAnimator = getItemView().animate();
        }
        return mItemAnimator;
    }

    public void clearItemAnimator() {
        if (mItemAnimator != null) {
            mItemAnimator.cancel();
        }
    }

    @NonNull
    public View getItemView() {
        if (mItemView == null) {
            throw new NullPointerException("Item view is null");
        }
        return mItemView;
    }

    public void setSquare(boolean square) {
        if (mSquare != square) {
            mSquare = square;
            requestLayout();
            invalidate();
        }
    }

    public void setCached(boolean cached) {
        if (mTagView != null) {
            mTagView.setCached(cached);
        }
    }

    @CallSuper
    public void initialise(@Nullable T asset) {
        if (mTagView != null) {
            mTagView.bumpCount();
        }
    }

    public abstract void animateCard();

    public abstract void clearCardAnimation();
}