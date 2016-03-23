package me.oriley.cratesample.loaders;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.DecelerateInterpolator;
import me.oriley.crate.Crate;
import me.oriley.crate.ImageAsset;
import me.oriley.crate.loader.AssetLoader;
import me.oriley.cratesample.widget.CrateBitmapView;

@SuppressWarnings("unused")
public class CrateBitmapLoader extends AssetLoader<CrateBitmapView, ImageAsset, Bitmap> {

    private static final long ANIM_DURATION_MILLIS = 700;


    public CrateBitmapLoader(@NonNull Crate crate) {
        super(crate);
    }


    @Override
    protected void initialiseTarget(@NonNull CrateBitmapView view, @NonNull ImageAsset asset) {
        view.setBitmap(null);
        view.setBitmapScale(8f, 8f);
        view.setBitmapRotation(180f);
    }

    @Nullable
    @Override
    protected Result<Bitmap> load(@NonNull ImageAsset asset) {
        boolean cached = mCrate.isBitmapCached(asset);
        return new Result<>(mCrate.getBitmap(asset), asset, cached);
    }

    @Override
    protected void apply(@NonNull Result<Bitmap> result, @NonNull CrateBitmapView view) {
        view.setBitmap(result.payload);
        view.setTagColor(result.cached ? Color.GREEN : Color.RED);
        view.animateBitmap()
                .setStartDelay(0)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(ANIM_DURATION_MILLIS);
    }
}
