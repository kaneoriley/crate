/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package me.oriley.cratesample;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import me.oriley.crate.Crate;
import me.oriley.crate.SvgAsset;
import me.oriley.cratesample.tasks.CrateTask;
import me.oriley.cratesample.tasks.CrateTask.Finisher;
import me.oriley.cratesample.tasks.CrateTask.Worker;

import java.lang.ref.WeakReference;

@SuppressWarnings("unused")
public class SvgImageView extends ImageView {

    private static final String TAG = SvgImageView.class.getSimpleName();

    private static final long ANIM_DURATION_MILLIS = 75;

    @NonNull
    private WeakReference<Crate> mCrate = new WeakReference<>(null);

    @NonNull
    private WeakReference<SvgAsset> mAsset = new WeakReference<>(null);

    @Nullable
    private CrateTask<SvgImageView, Bitmap> mLoadImageTask;

    private boolean mAttached;

    public SvgImageView(Context context) {
        super(context);
    }

    public SvgImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SvgImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SvgImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void loadSvgBitmap(@NonNull final Crate crate, @NonNull final SvgAsset asset) {
        clearTask(true, true);
        setImageBitmap(null);
        setAlpha(0f);
        mCrate = new WeakReference<>(crate);
        mAsset = new WeakReference<>(asset);
        mLoadImageTask = startTask(this, crate, asset);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttached = true;

        Crate crate = mCrate.get();
        SvgAsset asset = mAsset.get();
        if (crate != null && asset != null) {
            mLoadImageTask = startTask(this, crate, asset);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached = false;
        clearTask(true, false);
    }

    protected boolean isAttached() {
        return mAttached;
    }

    private void clearTask(boolean cancel, boolean clearReferences) {
        if (mLoadImageTask != null && cancel) {
            mLoadImageTask.cancel(true);
        }
        if (clearReferences){
            mCrate = new WeakReference<>(null);
            mAsset = new WeakReference<>(null);
        }
        mLoadImageTask = null;
    }

    // Static task creator so no outer class is leaked
    private static CrateTask<SvgImageView, Bitmap> startTask(@NonNull SvgImageView callerView,
                                                             @NonNull final Crate crate,
                                                             @NonNull final SvgAsset asset) {
        return new CrateTask<>(callerView, new Worker<SvgImageView, Bitmap>() {
            @Override
            public Bitmap doInBackground(@NonNull SvgImageView view) throws Exception {
                return crate.getSvgBitmap(asset);
            }
        }, new Finisher<SvgImageView, Bitmap>() {
            @Override
            public void onSuccess(@NonNull SvgImageView view, @NonNull Bitmap result) {
                Log.i(TAG, "svg bitmap loaded");
                view.setImageBitmap(result);
                if (view.isAttached()) {
                    view.animate().alpha(1f).setDuration(ANIM_DURATION_MILLIS);
                } else {
                    view.setAlpha(1f);
                }
                view.clearTask(false, true);
            }

            @Override
            public void onError(@NonNull SvgImageView view, @NonNull Exception e) {
                Log.e(TAG, "error loading bitmap for key: " + asset.getPath(), e);
                view.clearTask(false, true);
            }
        }).execute();
    }
}
