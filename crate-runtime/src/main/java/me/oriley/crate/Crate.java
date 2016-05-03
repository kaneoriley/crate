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
import java.util.zip.GZIPInputStream;

@SuppressWarnings("unused")
public final class Crate {

    private static final String TAG = Crate.class.getSimpleName();

    @NonNull
    private final AssetManager mAssetManager;

    @NonNull
    private final CrateSvg.Parser mSvgParser = CrateSvg.getParser();

    @NonNull
    public final CrateDictionary mDictionary;

    @NonNull
    public final CrateDictionary.AssetsClass assets;

    private final boolean DEBUG;


    public Crate(@NonNull Context context) {
        mAssetManager = context.getApplicationContext().getAssets();
        mDictionary = new CrateDictionary();

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
        Bitmap bitmap = null;
        try {
            InputStream stream = open(imageAsset);
            //noinspection TryFinallyCanBeTryWithResources
            try {
                bitmap = BitmapFactory.decodeStream(stream);
            } finally {
                //noinspection ThrowFromFinallyBlock
                stream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load bitmap for key: " + key, e);
            e.printStackTrace();
        }
        return bitmap;
    }

    @Nullable
    public Typeface getTypeface(@NonNull FontAsset fontAsset) {
        String key = fontAsset.mPath;
        Typeface typeface = null;
        try {
            typeface = Typeface.createFromAsset(mAssetManager, key);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load typeface for key: " + key, e);
            e.printStackTrace();
        }
        return typeface;
    }

    @NonNull
    public Bitmap createSvgBitmap(@NonNull Picture picture) {
        int desiredWidth = picture.getWidth();
        int desiredHeight = picture.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(desiredWidth, desiredHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPicture(picture, new Rect(0, 0, desiredWidth, desiredHeight));
        return bitmap;
    }

    @Nullable
    public Bitmap getSvgBitmap(@NonNull SvgAsset svgAsset) {
        String key = svgAsset.mPath;
        Picture picture = getSvgPicture(svgAsset);
        if (picture == null) {
            if (DEBUG) Log.d(TAG, "Picture is null for key: " + key);
            return null;
        }

        return createSvgBitmap(picture);
    }

    @Nullable
    public PictureDrawable getSvgDrawable(@NonNull SvgAsset svgAsset) {
        Picture picture = getSvgPicture(svgAsset);
        return picture != null ? new PictureDrawable(picture) : null;
    }

    @Nullable
    public Picture getSvgPicture(@NonNull SvgAsset svgAsset) {
        String key = svgAsset.getPath();
        Picture picture = null;
        try {
            InputStream stream = open(svgAsset);
            //noinspection TryFinallyCanBeTryWithResources
            try {
                picture = mSvgParser.parseSvg(stream);
            } finally {
                //noinspection ThrowFromFinallyBlock
                stream.close();
            }
        } catch (IOException | SvgParseException e) {
            Log.e(TAG, "Failed to load SVG for key: " + key, e);
            e.printStackTrace();
        }
        return picture;
    }
}
