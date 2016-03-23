package me.oriley.crate.loader;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import me.oriley.crate.Crate;
import me.oriley.crate.SvgAsset;

@SuppressWarnings("unused")
public final class SvgLoader extends AssetLoader<ImageView, SvgAsset, Bitmap> {

    private static final long ANIM_DURATION_MILLIS = 250;


    public SvgLoader(@NonNull Crate crate) {
        super(crate);
    }


    @Override
    protected void initialiseTarget(@NonNull ImageView view, @NonNull SvgAsset asset) {
        view.setImageBitmap(null);
        view.setAlpha(0f);
        view.setScaleX(0.3f);
        view.setScaleY(0.3f);
    }

    @Nullable
    @Override
    protected Result<Bitmap> load(@NonNull SvgAsset asset) {
        boolean cached = mCrate.isSvgDrawableCached(asset);
        return new Result<>(mCrate.getSvgBitmap(asset), asset, cached);
    }

    @Override
    protected void apply(@NonNull Result<Bitmap> result, @NonNull ImageView view) {
        view.setImageBitmap(result.payload);
        view.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(ANIM_DURATION_MILLIS);
    }
}