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

import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.OvershootInterpolator;
import me.oriley.crate.Crate;
import me.oriley.crate.FontAsset;
import me.oriley.crate.loader.AssetLoader;
import me.oriley.cratesample.widget.CrateFontView;

@SuppressWarnings("unused")
public final class CrateFontLoader extends AssetLoader<CrateFontView, FontAsset, Typeface> {

    private static final long ANIM_DURATION_MILLIS = 500;

    private final int mInitialTranslation;


    public CrateFontLoader(@NonNull Crate crate, int parentWidth) {
        super(crate);
        mInitialTranslation = (int) (parentWidth * 0.5f);
    }


    @Override
    protected void initialiseTarget(@NonNull CrateFontView view, @NonNull FontAsset asset) {
        view.setText(asset.getFontName());
        view.setTextAlpha(0f);
        view.setTextTranslation(mInitialTranslation, 0);
    }

    @Nullable
    @Override
    protected Result<Typeface> load(@NonNull FontAsset asset) {
        boolean cached = mCrate.isTypefaceCached(asset);
        return new Result<>(mCrate.getTypeface(asset), asset, cached);
    }

    @Override
    protected void apply(@NonNull Result<Typeface> result, @NonNull CrateFontView view) {
        view.setTextTypeface(result.payload);
        view.setTagColor(result.cached ? Color.GREEN : Color.RED);
        view.animateText()
                .alpha(1f)
                .translationX(0)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .setDuration(ANIM_DURATION_MILLIS);
    }
}