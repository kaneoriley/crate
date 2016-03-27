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

package me.oriley.cratesample.loaders;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
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

    @NonNull
    @Override
    protected Result<Bitmap> load(@NonNull CrateSvgView view, @NonNull SvgAsset asset) {
        boolean cached = mCrate.isSvgDrawableCached(asset);
        return new Result<>(mCrate.getSvgBitmap(asset), asset, cached);
    }

    @Override
    protected void apply(@NonNull CrateSvgView view, @NonNull Result<Bitmap> result) {
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