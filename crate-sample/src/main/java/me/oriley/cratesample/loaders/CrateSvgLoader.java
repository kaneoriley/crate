package me.oriley.cratesample.loaders;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.OvershootInterpolator;
import me.oriley.crate.Crate;
import me.oriley.crate.SvgAsset;
import me.oriley.crate.loader.AssetLoader;
import me.oriley.cratesample.widget.CrateSvgView;

@SuppressWarnings("unused")
public final class CrateSvgLoader extends AssetLoader<CrateSvgView, SvgAsset, Bitmap> {

    private static final long ANIM_DURATION_MILLIS = 250;


    public CrateSvgLoader(@NonNull Crate crate) {
        super(crate);
    }


    @Override
    protected void initialiseTarget(@NonNull CrateSvgView view, @NonNull SvgAsset asset) {
        view.setBitmap(null);
        view.setBitmapAlpha(0f);
        view.setBitmapScale(0.3f, 0.3f);
    }

    @Nullable
    @Override
    protected Result<Bitmap> load(@NonNull SvgAsset asset) {
        boolean cached = mCrate.isSvgDrawableCached(asset);
        return new Result<>(mCrate.getSvgBitmap(asset), asset, cached);
    }

    @Override
    protected void apply(@NonNull Result<Bitmap> result, @NonNull CrateSvgView view) {
        view.setBitmap(result.payload);
        view.setTagColor(result.cached ? Color.GREEN : Color.RED);
        view.animateBitmap()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(ANIM_DURATION_MILLIS);
    }
}