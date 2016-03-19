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

package me.oriley.cratesample.tasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;

@SuppressWarnings("unused")
public class CrateTask<C, T> extends AsyncTask<Void, Void, T> {

    private static final String TAG = CrateTask.class.getSimpleName();

    @NonNull
    private final WeakReference<C> mCaller;

    @NonNull
    private final Worker<C, T> mWorker;

    @Nullable
    private final Finisher<C, T> mFinisher;

    private Exception mException;

    public CrateTask(@NonNull C caller,
                     @NonNull Worker<C, T> worker,
                     @Nullable Finisher<C, T> finisher) {
        mCaller = new WeakReference<>(caller);
        mWorker = worker;
        mFinisher = finisher;
    }

    protected final T doInBackground(Void... params) {
        C caller = mCaller.get();
        if (caller != null) {
            try {
                return mWorker.doInBackground(caller);
            } catch (Exception e) {
                mException = e;
            }
        }

        return null;
    }

    protected final void onPostExecute(T result) {
        if (!isCancelled()) {
            if (mException == null) {
                onSuccess(result);
            } else {
                Log.w(TAG, mException);
                onError(mException);
            }
        }
    }

    protected void onSuccess(T result) {
        C caller = mCaller.get();
        if (mFinisher != null && caller != null) {
            mFinisher.onSuccess(caller, result);
        }
    }

    protected void onError(Exception e) {
        C caller = mCaller.get();
        if (mFinisher != null && caller != null) {
            mFinisher.onError(caller, e);
        }
    }

    private void dispose() {

    }

    @NonNull
    public CrateTask<C, T> execute() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return this;
    }

    public interface Worker<C, T> {
        T doInBackground(@NonNull C caller) throws Exception;
    }

    public interface Finisher<C, T> {
        void onSuccess(@NonNull C caller, @NonNull T result);

        void onError(@NonNull C caller, @NonNull Exception e);
    }
}