package me.oriley.cratesample;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import com.nanotasks.BackgroundWork;
import com.nanotasks.BetterTask;
import com.nanotasks.Completion;
import me.oriley.crate.Crate;
import me.oriley.crate.SvgAsset;

public class SvgImageView extends AppCompatImageView {

    private static final String TAG = SvgImageView.class.getSimpleName();

    private BetterTask<Bitmap> mLoadImageTask;

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

    public void loadSvgBitmap(@NonNull final Crate crate, @NonNull final SvgAsset asset) {
        if (mLoadImageTask != null) {
            mLoadImageTask.cancel(true);
        }
        setImageBitmap(null);
        setAlpha(0f);

        mLoadImageTask = new BetterTask<>(getContext(), new BackgroundWork<Bitmap>() {
            @Override
            public Bitmap doInBackground() throws Exception {
                return crate.getSvgBitmap(asset);
            }
        }, new Completion<Bitmap>() {
            @Override
            public void onSuccess(final Context context, final Bitmap result) {
                Log.i(TAG, "svg bitmap loaded");
                setImageBitmap(result);
                if (mAttached) {
                    animate().alpha(1f).setDuration(150);
                } else {
                    setAlpha(1f);
                }
                mLoadImageTask = null;
            }

            @Override
            public void onError(Context context, Exception e) {
                Log.e(TAG, "error loading bitmap for key: " + asset.getPath(), e);
                mLoadImageTask = null;
            }
        });
        mLoadImageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttached = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached = false;
    }
}
