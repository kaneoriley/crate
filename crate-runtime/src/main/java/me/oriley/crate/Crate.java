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
import android.content.res.AssetFileDescriptor;
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
import java.util.zip.GZIPInputStream;

@SuppressWarnings("unused")
public final class Crate {

    private static final String TAG = Crate.class.getSimpleName();

    @NonNull
    private final AssetManager mAssetManager;

    private final boolean DEBUG;

    @NonNull
    private final CrateCache<FontAsset, Typeface> mTypefaceCache;

    @NonNull
    private final CrateCache<ImageAsset, Bitmap> mBitmapCache;

    @NonNull
    private final CrateCache<SvgAsset, PictureDrawable> mSvgCache;

    @NonNull
    private final CrateSvg.Parser mSvgParser = CrateSvg.getParser();

    @NonNull
    public final CrateDictionary mDictionary;

    @NonNull
    public final CrateDictionary.AssetsClass assets;

    private Crate(@NonNull Context context,
                  int typefaceCacheMaxSize,
                  int bitmapCacheMaxSize,
                  int svgCacheMaxSize) {
        mAssetManager = context.getApplicationContext().getAssets();
        mDictionary = new CrateDictionary();

        mTypefaceCache = new CrateCache<>(typefaceCacheMaxSize, true);
        mBitmapCache = new CrateCache<>(bitmapCacheMaxSize, true);
        mSvgCache = new CrateCache<>(svgCacheMaxSize, true);

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

    @NonNull
    public final AssetFileDescriptor openFd(@NonNull Asset asset) throws IOException {
        return mAssetManager.openFd(asset.getPath());
    }

    @Nullable
    public Bitmap getBitmap(@NonNull ImageAsset imageAsset) {
        String key = imageAsset.mPath;
        if (mBitmapCache.containsKey(imageAsset)) {
            Bitmap bitmap = mBitmapCache.get(imageAsset);
            if (bitmap != null && !bitmap.isRecycled()) {
                if (DEBUG) Log.d(TAG, "Using cached bitmap for key: " + key);
                return bitmap;
            } else {
                if (DEBUG) Log.d(TAG, "Ejecting recycled bitmap for key: " + key);
                mBitmapCache.remove(imageAsset);
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
                cacheBitmap(imageAsset, bitmap);
            }
        }
        return bitmap;
    }

    public boolean isBitmapCached(@NonNull ImageAsset key) {
        return mBitmapCache.containsKey(key);
    }

    private void cacheBitmap(@NonNull ImageAsset key, @NonNull Bitmap bitmap) {
        if (mBitmapCache.maxSize() > 0) {
            mBitmapCache.put(key, bitmap);
        }
    }

    @Nullable
    public Typeface getTypeface(@NonNull FontAsset fontAsset) {
        String key = fontAsset.mPath;
        if (mTypefaceCache.containsKey(fontAsset)) {
            if (DEBUG) Log.d(TAG, "Using cached typeface for key: " + key);
            return mTypefaceCache.get(fontAsset);
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
                cacheTypeface(fontAsset, typeface);
            }
        }
        return typeface;
    }

    public boolean isTypefaceCached(@NonNull FontAsset key) {
        return mTypefaceCache.containsKey(key);
    }

    private void cacheTypeface(@NonNull FontAsset key, @NonNull Typeface typeface) {
        if (mTypefaceCache.maxSize() > 0) {
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
        if (mSvgCache.containsKey(svgAsset)) {
            if (DEBUG) Log.d(TAG, "Using cached PictureDrawable for key: " + key);
            return mSvgCache.get(svgAsset);
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
                cachePictureDrawable(svgAsset, drawable);
            }
        }
        return drawable;
    }

    public boolean isSvgDrawableCached(@NonNull SvgAsset key) {
        return mSvgCache.containsKey(key);
    }

    private void cachePictureDrawable(@NonNull SvgAsset key, @NonNull PictureDrawable drawable) {
        if (mSvgCache.maxSize() > 0) {
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
            mTypefaceCache.remove(asset);
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
            mBitmapCache.remove(asset);
        }
    }

    public void clearSvgCache() {
        mSvgCache.clear();
    }

    public void clearSvgCache(@NonNull SvgAsset... assets) {
        for (SvgAsset asset : assets) {
            mSvgCache.remove(asset);
        }
    }

    public static final class Builder {

        @NonNull
        private Context mContext;

        private int mTypefaceCacheMaxSize;

        private int mBitmapCacheMaxSize;

        private int mSvgCacheMaxSize;

        public Builder(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        @NonNull
        public Builder typefaceCacheMaxSize(int maxSize) {
            mTypefaceCacheMaxSize = maxSize;
            return this;
        }

        @NonNull
        public Builder bitmapCacheMaxSize(int maxSize) {
            mBitmapCacheMaxSize = maxSize;
            return this;
        }

        @NonNull
        public Builder svgCacheMaxSize(int maxSize) {
            mSvgCacheMaxSize = maxSize;
            return this;
        }

        @NonNull
        public Crate build() {
            return new Crate(mContext, mTypefaceCacheMaxSize, mBitmapCacheMaxSize, mSvgCacheMaxSize);
        }
    }
}
