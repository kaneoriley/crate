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

import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import me.oriley.cratesample.BuildConfig;

public class CrateRecyclerView extends RecyclerView {

    private static final long BACKGROUND_COLOR_TRANSITION_MILLIS = 150;

    private static final float FLING_MIN_THRESHOLD = 3f;
    private static final float FLING_MAX_THRESHOLD = 6f;

    private static final int DRAG_IDLE_STATE_DELAY = 250;
    private static final int UPDATE_VELOCITY_DELAY = 50;

    private static final int FLING_BACKGROUND_COLOR = 0xFFF44336;
    private static final int IDLE_BACKGROUND_COLOR = 0xFFF1F8E9;

    private enum ScrollState {
        FLING, IDLE
    }

    @NonNull
    private final ArgbEvaluator mArgbEvaluator = new ArgbEvaluator();

    @NonNull
    private final Runnable mIdleRunnable = new Runnable() {
        @Override
        public void run() {
            setScrollState(ScrollState.IDLE);
        }
    };

    @NonNull
    private final Runnable mUpdateVelocityRunnable = new Runnable() {
        @Override
        public void run() {
            if (mScrollState == ScrollState.FLING) {
                long currentTime = System.currentTimeMillis();
                long interval = currentTime - mPreviousFlingUpdateMillis;
                long offset = mTotalScrollOffset - mPreviousFlingScrollOffset;

                float velocity = offset * 1000f / interval;

                mPreviousFlingScrollOffset = mTotalScrollOffset;
                mPreviousFlingUpdateMillis = currentTime;

                float absoluteVelocity = Math.abs(velocity);
                if (absoluteVelocity < mMinVelocity) {
                    setScrollState(ScrollState.IDLE);
                } else {
                    if (absoluteVelocity >= mMaxVelocity) {
                        setFlingScale(1.0f);
                    } else {
                        int velocitySpan = mMaxVelocity - mMinVelocity;
                        setFlingScale((absoluteVelocity - mMinVelocity) / velocitySpan);
                    }
                    postDelayed(mUpdateVelocityRunnable, UPDATE_VELOCITY_DELAY);
                }
            }
        }
    };

    @NonNull
    private ScrollState mScrollState = ScrollState.IDLE;

    @Nullable
    private ValueAnimator mBackgroundAnimator;

    private float mFriction = 1.0f;

    private float mFlingScale = 0f;

    private long mPreviousFlingUpdateMillis;

    private long mPreviousFlingScrollOffset;

    private long mTotalScrollOffset = Long.MAX_VALUE / 2;

    private int mMaxVelocity;

    private int mMinVelocity;

    private boolean mHorizontal;


    public CrateRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public CrateRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CrateRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mTotalScrollOffset += (mHorizontal ? dx : dy);
            }
        });

        if (BuildConfig.DEBUG) {
            setBackgroundColor(IDLE_BACKGROUND_COLOR);
        } else {
            setBackgroundColor(Color.WHITE);
        }
    }


    public void setFriction(float friction) {
        mFriction = friction;
    }

    public float getFlingScale() {
        return mFlingScale;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        removeCallbacks(mIdleRunnable);
        removeCallbacks(mUpdateVelocityRunnable);

        switch (state) {
            case SCROLL_STATE_IDLE:
                setScrollState(ScrollState.IDLE);
                break;
            case SCROLL_STATE_DRAGGING:
                postDelayed(mIdleRunnable, DRAG_IDLE_STATE_DELAY);
                break;
            case SCROLL_STATE_SETTLING:
                dispatchFling();
                break;
        }
    }

    private void dispatchFling() {
        removeCallbacks(mUpdateVelocityRunnable);
        mPreviousFlingUpdateMillis = System.currentTimeMillis();
        mPreviousFlingScrollOffset = mTotalScrollOffset;
        postDelayed(mUpdateVelocityRunnable, UPDATE_VELOCITY_DELAY);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        removeCallbacks(mIdleRunnable);

        mHorizontal = velocityX != 0;
        float velocity = (mHorizontal ? velocityX : velocityY) * mFriction;

        if (mScrollState != ScrollState.FLING) {
            int parentSize = mHorizontal ? getWidth() : getHeight();
            mMinVelocity = (int) (parentSize * FLING_MIN_THRESHOLD * mFriction);
            mMaxVelocity = (int) (parentSize * FLING_MAX_THRESHOLD * mFriction);

            int absoluteVelocity = (int) Math.abs(velocity);
            if (absoluteVelocity > mMinVelocity) {
                if (absoluteVelocity > mMaxVelocity) {
                    setFlingScale(1.0f);
                } else {
                    int velocitySpan = mMaxVelocity - mMinVelocity;
                    setFlingScale((absoluteVelocity - mMinVelocity) / velocitySpan);
                }
                setScrollState(ScrollState.FLING);
                dispatchFling();
            } else {
                setFlingScale(0f);
            }
        }

        return super.fling((int) (mHorizontal ? velocity : 0), (int) (mHorizontal ? 0 : velocity));
    }

    private void setFlingScale(float scale) {
        if (mFlingScale != scale) {
            if (BuildConfig.DEBUG) {
                if (mBackgroundAnimator != null) {
                    mBackgroundAnimator.cancel();
                }

                mBackgroundAnimator = ValueAnimator.ofObject(new FloatEvaluator(), mFlingScale, scale);
                mBackgroundAnimator.setDuration(BACKGROUND_COLOR_TRANSITION_MILLIS);
                mBackgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        mFlingScale = (float) animator.getAnimatedValue();
                        int newColor = (int) mArgbEvaluator.evaluate(mFlingScale, IDLE_BACKGROUND_COLOR, FLING_BACKGROUND_COLOR);
                        setBackgroundColor(newColor);
                    }
                });
                mBackgroundAnimator.start();
            }
        }
    }

    private void setScrollState(@NonNull ScrollState state) {
        if (mScrollState != state) {
            mScrollState = state;

            if (mScrollState == ScrollState.IDLE) {
                // Reset total offset. Good luck scrolling past it.
                setFlingScale(0f);
                mTotalScrollOffset = Long.MAX_VALUE / 2;
            }
        }
    }
}
