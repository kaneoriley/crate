/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package me.oriley.crate;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.*;
import android.graphics.drawable.PictureDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import me.oriley.crate.CrateSvg.SvgParseException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

@SuppressWarnings("unused")
public final class Crate {

    private static final String TAG = Crate.class.getSimpleName();

    @NonNull
    private final AssetManager mAssetManager;

    private final boolean DEBUG;

    @NonNull
    private final HashMap<String, Typeface> mTypefaceCache = new HashMap<>();

    @NonNull
    private final HashMap<String, Bitmap> mBitmapCache = new HashMap<>();

    @NonNull
    private final HashMap<String, PictureDrawable> mSvgCache = new HashMap<>();

    @NonNull
    private final CrateSvg.Parser mSvgParser = CrateSvg.getParser();

    @NonNull
    public final CrateDictionary mDictionary;

    @NonNull
    public final CrateDictionary.AssetsClass assets;

    private final boolean mTypefaceCacheEnabled;

    private final boolean mBitmapCacheEnabled;

    private final boolean mSvgCacheEnabled;

    private Crate(@NonNull Context context,
                  boolean typefaceCacheEnabled,
                  boolean bitmapCacheEnabled,
                  boolean svgCacheEnabled) {
        mAssetManager = context.getApplicationContext().getAssets();
        mDictionary = new CrateDictionary();

        mTypefaceCacheEnabled = typefaceCacheEnabled;
        mBitmapCacheEnabled = bitmapCacheEnabled;
        mSvgCacheEnabled = svgCacheEnabled;

        // Ugly, but helps keep with desired code style
        assets = mDictionary.assets;
        DEBUG = mDictionary.mDebug;
    }

    @NonNull
    public InputStream open(@NonNull Asset asset) throws IOException {
        return open(asset, AssetManager.ACCESS_STREAMING);
    }

    @NonNull
    public InputStream open(@NonNull Asset asset, int mode) throws IOException {
        InputStream stream = mAssetManager.open(asset.getPath(), mode);
        if (asset.isGzipped()) {
            stream = new GZIPInputStream(stream);
        }
        return stream;
    }

    @Nullable
    public Bitmap getBitmap(@NonNull ImageAsset imageAsset) {
        String key = imageAsset.mPath;
        if (mBitmapCacheEnabled && mBitmapCache.containsKey(key)) {
            Bitmap bitmap = mBitmapCache.get(key);
            if (bitmap != null && !bitmap.isRecycled()) {
                if (DEBUG) Log.d(TAG, "Using cached bitmap for key: " + key);
                return bitmap;
            } else {
                if (DEBUG) Log.d(TAG, "Ejecting recycled bitmap for key: " + key);
                mBitmapCache.remove(key);
            }
        }
        Bitmap bitmap = null;
        try {
            InputStream stream = open(imageAsset);
            //noinspection TryFinallyCanBeTryWithResources
            try {
                bitmap = BitmapFactory.decodeStream(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load bitmap for key: " + key, e);
            e.printStackTrace();
        } finally {
            if (bitmap != null) {
                if (DEBUG) Log.d(TAG, "Bitmap loaded for key: " + key);
                cacheBitmap(key, bitmap);
            }
        }
        return bitmap;
    }

    private void cacheBitmap(@NonNull String key, @NonNull Bitmap bitmap) {
        if (mBitmapCacheEnabled) {
            mBitmapCache.put(key, bitmap);
        }
    }

    @Nullable
    public Typeface getTypeface(@NonNull FontAsset fontAsset) {
        String key = fontAsset.mPath;
        if (mTypefaceCacheEnabled && mTypefaceCache.containsKey(key)) {
            if (DEBUG) Log.d(TAG, "Using cached typeface for key: " + key);
            return mTypefaceCache.get(key);
        }
        Typeface typeface = null;
        try {
            typeface = Typeface.createFromAsset(mAssetManager, key);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load typeface for key: " + key, e);
            e.printStackTrace();
        } finally {
            if (typeface != null) {
                if (DEBUG) Log.d(TAG, "Typeface loaded for key: " + key);
                cacheTypeface(key, typeface);
            }
        }
        return typeface;
    }

    private void cacheTypeface(@NonNull String key, @NonNull Typeface typeface) {
        if (mTypefaceCacheEnabled) {
            mTypefaceCache.put(key, typeface);
        }
    }

    @Nullable
    public Bitmap getSvgBitmap(@NonNull SvgAsset svgAsset) {
        return getSvgBitmap(svgAsset, 1f);
    }

    @Nullable
    public Bitmap getSvgBitmap(@NonNull SvgAsset svgAsset, float scale) {
        String key = svgAsset.mPath;

        PictureDrawable pictureDrawable = getSvgDrawable(svgAsset);
        if (pictureDrawable == null) {
            if (DEBUG) Log.d(TAG, "PictureDrawable is null for key: " + key);
            return null;
        }

        int desiredWidth = (int) (pictureDrawable.getIntrinsicWidth() * scale);
        int desiredHeight = (int) (pictureDrawable.getIntrinsicHeight() * scale);
        Bitmap bitmap = Bitmap.createBitmap(desiredWidth, desiredHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPicture(pictureDrawable.getPicture(), new Rect(0, 0, desiredWidth, desiredHeight));

        return bitmap;
    }

    @Nullable
    public PictureDrawable getSvgDrawable(@NonNull SvgAsset svgAsset) {
        String key = svgAsset.getPath();
        if (mSvgCacheEnabled && mSvgCache.containsKey(key)) {
            if (DEBUG) Log.d(TAG, "Using cached PictureDrawable for key: " + key);
            return mSvgCache.get(key);
        }

        PictureDrawable drawable = null;
        try {
            InputStream stream = open(svgAsset);
            //noinspection TryFinallyCanBeTryWithResources
            try {
                drawable = mSvgParser.parseSvg(stream);
            } finally {
                stream.close();
            }
        } catch (IOException | SvgParseException e) {
            Log.e(TAG, "Failed to load SVG for key: " + key, e);
            e.printStackTrace();
        } finally {
            if (drawable != null) {
                if (DEBUG) Log.d(TAG, "SVG loaded for key: " + key);
                cachePictureDrawable(key, drawable);
            }
        }
        return drawable;
    }

    private void cachePictureDrawable(@NonNull String key, @NonNull PictureDrawable drawable) {
        if (mSvgCacheEnabled) {
            mSvgCache.put(key, drawable);
        }
    }

    public void clear() {
        clearTypefaceCache();
        clearBitmapCache();
        clearSvgCache();
    }

    public void clearTypefaceCache() {
        mTypefaceCache.clear();
    }

    public void clearTypefaceCache(@NonNull FontAsset... assets) {
        for (FontAsset asset : assets) {
            mTypefaceCache.remove(asset.getPath());
        }
    }

    public void clearBitmapCache() {
        for (Bitmap bitmap : mBitmapCache.values()) {
            bitmap.recycle();
        }
        mBitmapCache.clear();
    }

    public void clearBitmapCache(@NonNull ImageAsset... assets) {
        for (ImageAsset asset : assets) {
            mBitmapCache.remove(asset.getPath());
        }
    }

    public void clearSvgCache() {
        mSvgCache.clear();
    }

    public void clearSvgCache(@NonNull SvgAsset... assets) {
        for (SvgAsset asset : assets) {
            mSvgCache.remove(asset.getPath());
        }
    }

    public static final class Builder {

        @NonNull
        private Context mContext;

        private boolean mTypefaceCacheEnabled;

        private boolean mBitmapCacheEnabled;

        private boolean mSvgCacheEnabled;

        public Builder(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        @NonNull
        public Builder setTypefaceCacheEnabled(boolean enabled) {
            mTypefaceCacheEnabled = enabled;
            return this;
        }

        @NonNull
        public Builder setBitmapCacheEnabled(boolean enabled) {
            mBitmapCacheEnabled = enabled;
            return this;
        }

        @NonNull
        public Builder setSvgCacheEnabled(boolean enabled) {
            mSvgCacheEnabled = enabled;
            return this;
        }

        @NonNull
        public Crate build() {
            return new Crate(mContext, mTypefaceCacheEnabled, mBitmapCacheEnabled, mSvgCacheEnabled);
        }
    }
}
