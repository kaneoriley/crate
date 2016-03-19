package me.oriley.crate.loader;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import me.oriley.crate.Crate;
import me.oriley.crate.ImageAsset;

@SuppressWarnings("unused")
public class ImageLoader extends ViewLoader<ImageView, ImageAsset, Bitmap> {

    private static final long ANIM_DURATION_MILLIS = 700;


    public ImageLoader(@NonNull Crate crate) {
        super(crate);
    }


    @Override
    protected void initialiseView(@NonNull ImageView view) {
        view.setImageBitmap(null);
        view.setScaleX(8f);
        view.setScaleY(8f);
        view.setRotation(180f);
    }

    @Nullable
    @Override
    protected Bitmap load(ImageAsset asset) {
        return mCrate.getBitmap(asset);
    }

    @Override
    protected void apply(@NonNull Bitmap payload, @NonNull ImageView view) {
        view.setImageBitmap(payload);
        view.animate().setStartDelay(0).scaleX(1f).scaleY(1f).rotation(0f)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(ANIM_DURATION_MILLIS);
    }
}
