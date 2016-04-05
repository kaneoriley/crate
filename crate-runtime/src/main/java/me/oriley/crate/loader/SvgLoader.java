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

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
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

    @NonNull
    @Override
    protected Result<Bitmap> load(@NonNull ImageView view, @NonNull SvgAsset asset) {
        boolean cached = mCrate.isSvgCached(asset);
        return new Result<>(mCrate.getSvgBitmap(asset), asset, cached);
    }

    @Override
    protected void apply(@NonNull ImageView view, @NonNull Result<Bitmap> result) {
        view.setImageBitmap(result.payload);
        view.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(ANIM_DURATION_MILLIS);
    }
}