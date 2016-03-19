package me.oriley.crate.loader;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import me.oriley.crate.Crate;
import me.oriley.crate.SvgAsset;

@SuppressWarnings("unused")
public final class SvgLoader extends ViewLoader<ImageView, SvgAsset, Bitmap> {

    private static final long ANIM_DURATION_MILLIS = 250;


    public SvgLoader(@NonNull Crate crate) {
        super(crate);
    }


    @Override
    protected void initialiseView(@NonNull ImageView view) {
        view.setImageBitmap(null);
        view.setAlpha(0f);
        view.setScaleX(0.3f);
        view.setScaleY(0.3f);
    }

    @Nullable
    @Override
    protected Bitmap load(SvgAsset asset) {
        return mCrate.getSvgBitmap(asset);
    }

    @Override
    protected void apply(@NonNull Bitmap payload, @NonNull ImageView view) {
        view.setImageBitmap(payload);
        view.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(ANIM_DURATION_MILLIS);
    }
}