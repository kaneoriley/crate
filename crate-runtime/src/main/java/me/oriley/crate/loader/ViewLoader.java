package me.oriley.crate.loader;

import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import me.oriley.crate.Asset;
import me.oriley.crate.Crate;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public abstract class ViewLoader<V extends View, A extends Asset, P> {

    private static final String TAG = ViewLoader.class.getSimpleName();

    // From AsyncTask
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    @NonNull
    final Crate mCrate;

    @NonNull
    private final Map<V, A> mReferenceMap = Collections.synchronizedMap(new WeakHashMap<V, A>());

    @NonNull
    private final ExecutorService mExecutorService;

    @NonNull
    private final Handler mHandler = new Handler();


    ViewLoader(@NonNull Crate crate) {
        mCrate = crate;
        mExecutorService = Executors.newFixedThreadPool(MAXIMUM_POOL_SIZE);
    }


    public void loadInto(@NonNull V view, @NonNull A asset) {
        mReferenceMap.put(view, asset);
        queueAsset(view, asset);
        initialiseView(view);
    }

    protected abstract void initialiseView(@NonNull V view);

    @Nullable
    protected abstract P load(A asset);

    protected abstract void apply(@NonNull P payload, @NonNull V view);

    private void queueAsset(@NonNull V view, @NonNull A asset) {
        PendingView p = new PendingView(view, asset);
        mExecutorService.submit(new PayloadRunnable(p));
    }

    public void dispose() {
        mHandler.removeCallbacksAndMessages(null);
    }

    private boolean isReused(@NonNull PendingView pendingView) {
        A tag = mReferenceMap.get(pendingView.view);
        return (tag == null || tag != pendingView.asset);
    }

    final class PendingView {

        @NonNull
        private final V view;

        @NonNull
        private final A asset;

        PendingView(@NonNull V v, @NonNull A a) {
            view = v;
            asset = a;
        }
    }

    final class PayloadRunnable implements Runnable {

        @NonNull
        final PendingView pendingView;

        PayloadRunnable(@NonNull PendingView p) {
            pendingView = p;
        }

        @Override
        public void run() {
            if (isReused(pendingView)) {
                return;
            }

            try {
                P p = load(pendingView.asset);
                if (!isReused(pendingView)) {
                    mHandler.post(new UpdateUiRunnable(pendingView, p));
                }
            } catch (Throwable t) {
                Log.e(TAG, "error loading payload for asset " + pendingView.asset, t);
            }
        }
    }

    final class UpdateUiRunnable implements Runnable {

        @NonNull
        final PendingView pendingView;

        @Nullable
        final P payload;

        UpdateUiRunnable(@NonNull PendingView v, @Nullable P p) {
            pendingView = v;
            payload = p;
        }

        @Override
        public void run() {
            if (!isReused(pendingView)) {
                if (payload != null) {
                    apply(payload, pendingView.view);
                }// else {
                    // TODO: Set placeholder image?
//                }
            }
        }
    }
}