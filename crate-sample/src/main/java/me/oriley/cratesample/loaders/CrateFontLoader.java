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

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import me.oriley.crate.Crate;
import me.oriley.crate.FontAsset;
import me.oriley.cratesample.fragments.FontListFragment.FontHolder;

@SuppressWarnings("unused")
public final class CrateFontLoader extends AssetLoader<FontHolder, FontAsset, Typeface> {


    public CrateFontLoader(@NonNull Crate crate, long loadDelayMillis) {
        super(crate, loadDelayMillis, 200, true);
    }


    @Override
    protected boolean initialiseTarget(@NonNull FontHolder holder, @NonNull FontAsset asset) {
        holder.initialise(asset);
        return true;
    }

    @NonNull
    @Override
    protected Result<Typeface> load(@NonNull FontHolder holder, @NonNull FontAsset asset) {
        boolean cached = mCache.containsKey(asset);
        Typeface typeface = mCache.get(asset);
        if (typeface == null) {
            typeface = mCrate.getTypeface(asset);
            if (typeface != null) {
                mCache.put(asset, typeface);
            }
        }
        return new Result<>(typeface, asset, cached);
    }

    @Override
    protected void apply(@NonNull final FontHolder holder, @NonNull Result<Typeface> result) {
        holder.view.setTypeface(result.payload);
        holder.view.setCached(result.cached);
        holder.loaded = true;
        holder.animateIfReady();
    }
}