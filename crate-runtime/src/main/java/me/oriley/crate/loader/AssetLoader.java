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
import android.support.annotation.Nullable;
import android.util.Log;
import me.oriley.crate.Asset;
import me.oriley.crate.Crate;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public abstract class AssetLoader<T, A extends Asset, P> {

    private static final String TAG = AssetLoader.class.getSimpleName();

    // From AsyncTask
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    @NonNull
    protected final Crate mCrate;

    @NonNull
    private final Map<T, A> mReferenceMap = Collections.synchronizedMap(new WeakHashMap<T, A>());

    @NonNull
    private final ExecutorService mExecutorService;

    @NonNull
    private final Handler mHandler = new Handler();


    protected AssetLoader(@NonNull Crate crate) {
        mCrate = crate;
        mExecutorService = Executors.newFixedThreadPool(MAXIMUM_POOL_SIZE);
    }


    public void loadInto(@NonNull T target, @NonNull A asset) {
        mReferenceMap.put(target, asset);
        queueAsset(target, asset);
        initialiseTarget(target, asset);
    }

    protected abstract void initialiseTarget(@NonNull T target, @NonNull A asset);

    @Nullable
    protected abstract Result<P> load(@NonNull A asset);

    protected abstract void apply(@NonNull Result<P> result, @NonNull T target);

    private void queueAsset(@NonNull T target, @NonNull A asset) {
        PendingTarget p = new PendingTarget(target, asset);
        mExecutorService.submit(new PayloadRunnable(p));
    }

    public void dispose() {
        mHandler.removeCallbacksAndMessages(null);
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
                Result<P> p = load(pendingTarget.asset);
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
            if (!isReused(pendingTarget)) {
                if (result != null) {
                    apply(result, pendingTarget.target);
                }
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