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

package me.oriley.crate.loader;

import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.util.Log;
import me.oriley.crate.Asset;
import me.oriley.crate.Crate;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.*;

@SuppressWarnings("unused")
public abstract class AssetLoader<T, A extends Asset, P> {

    private static final String TAG = AssetLoader.class.getSimpleName();
    private static final int NO_DELAY = -1;

    // From AsyncTask
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    @NonNull
    protected final Crate mCrate;

    @NonNull
    private final Map<T, A> mReferenceMap = Collections.synchronizedMap(new WeakHashMap<T, A>());

    @NonNull
    private final ScheduledThreadPoolExecutor mExecutorService;

    @NonNull
    private final Handler mHandler = new Handler();

    private final long mDelay;


    public AssetLoader(@NonNull Crate crate) {
        this(crate, 0);
    }

    public AssetLoader(@NonNull Crate crate, long loadDelayMillis) {
        mCrate = crate;
        mDelay = loadDelayMillis;

        mExecutorService = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE);
        mExecutorService.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        mExecutorService.setKeepAliveTime(0L, TimeUnit.MILLISECONDS);
    }


    public void loadInto(@NonNull T target, @NonNull A asset) {
        loadInto(target, asset, NO_DELAY);
    }

    public void loadInto(@NonNull T target, @NonNull A asset, long delay) {
        initialiseTarget(target, asset);
        mReferenceMap.put(target, asset);
        queueAsset(target, asset, delay);
    }

    protected abstract void initialiseTarget(@NonNull T target, @NonNull A asset);

    @NonNull
    protected abstract Result<P> load(@NonNull T target, @NonNull A asset);

    protected abstract void apply(@NonNull T target, @NonNull Result<P> result);

    private void queueAsset(@NonNull T target, @NonNull A asset, long delay) {
        PendingTarget p = new PendingTarget(target, asset);
        mExecutorService.schedule(new PayloadRunnable(p), delay >= 0 ? delay : mDelay, TimeUnit.MILLISECONDS);
    }

    @CallSuper
    public void dispose() {
        mHandler.removeCallbacksAndMessages(null);
        mExecutorService.shutdown();
    }

    private boolean isReused(@NonNull PendingTarget pendingTarget) {
        A tag = mReferenceMap.get(pendingTarget.target);
        return (tag == null || tag != pendingTarget.asset);
    }

    private final class PendingTarget {

        @NonNull
        private final T target;

        @NonNull
        private final A asset;

        PendingTarget(@NonNull T t, @NonNull A a) {
            target = t;
            asset = a;
        }
    }

    private final class PayloadRunnable implements Runnable {

        @NonNull
        final PendingTarget pendingTarget;

        PayloadRunnable(@NonNull PendingTarget p) {
            pendingTarget = p;
        }

        @Override
        public void run() {
            if (isReused(pendingTarget)) {
                return;
            }

            try {
                Result<P> p = load(pendingTarget.target, pendingTarget.asset);
                if (!isReused(pendingTarget)) {
                    mHandler.post(new UpdateUiRunnable(pendingTarget, p));
                }
            } catch (Throwable t) {
                Log.e(TAG, "error loading payload for asset " + pendingTarget.asset, t);
            }
        }
    }

    private final class UpdateUiRunnable implements Runnable {

        @NonNull
        final PendingTarget pendingTarget;

        @Nullable
        final Result<P> result;

        UpdateUiRunnable(@NonNull PendingTarget t, @Nullable Result<P> p) {
            pendingTarget = t;
            result = p;
        }

        @Override
        public void run() {
            if (!isReused(pendingTarget) && result != null) {
                apply(pendingTarget.target, result);
            }
        }
    }

    public final class Result<R> {

        @Nullable
        public final R payload;

        @NonNull
        public final A asset;

        public final boolean cached;

        public Result(@Nullable R payload, @NonNull A asset, boolean cached) {
            this.payload = payload;
            this.asset = asset;
            this.cached = cached;
        }
    }
}