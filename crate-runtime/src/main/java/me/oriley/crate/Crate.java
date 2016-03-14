package me.oriley.crate;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

@SuppressWarnings("unused")
public final class Crate {

    @NonNull
    private final AssetManager mAssetManager;

    private final boolean DEBUG;

    @NonNull
    private final HashMap<String, Typeface> mTypefaceCache = new HashMap<>();

    @NonNull
    private final HashMap<String, Bitmap> mBitmapCache = new HashMap<>();

    @NonNull
    public final CrateDictionary mDictionary;

    @NonNull
    public final CrateDictionary.AssetsClass assets;

    public Crate(@NonNull Context context) {
        mAssetManager = context.getApplicationContext().getAssets();
        mDictionary = new CrateDictionary();

        // Ugly, but helps keep with desired code style
        assets = mDictionary.assets;
        DEBUG = mDictionary.mDebug;
    }

    @NonNull
    public InputStream open(@NonNull Asset asset) throws IOException {
        return mAssetManager.open(asset.mPath);
    }

    @NonNull
    public InputStream open(@NonNull Asset asset, int mode) throws IOException {
        return mAssetManager.open(asset.mPath, mode);
    }

    @Nullable
    public Bitmap getBitmap(@NonNull ImageAsset imageAsset) {
        String key = imageAsset.mPath;
        if (mBitmapCache.containsKey(key)) {
            Bitmap bitmap = mBitmapCache.get(key);
            if (bitmap != null && !bitmap.isRecycled()) {
                if (DEBUG) Log.d("Crate", "Using cached bitmap for key: " + key);
                return bitmap;
            } else {
                if (DEBUG) Log.d("Crate", "Ejecting recycled bitmap for key: " + key);
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
            Log.e("Crate", "Failed to load bitmap for key: " + key, e);
            e.printStackTrace();
        } finally {
            if (bitmap != null) {
                if (DEBUG) Log.d("Crate", "Bitmap loaded for key: " + key);
                mBitmapCache.put(key, bitmap);
            }
        }
        return bitmap;
    }

    @Nullable
    public Typeface getTypeface(@NonNull FontAsset fontAsset) {
        String key = fontAsset.mPath;
        if (mTypefaceCache.containsKey(key)) {
            if (DEBUG) Log.d("Crate", "Using cached typeface for key: " + key);
            return mTypefaceCache.get(key);
        }
        Typeface typeface = null;
        try {
            typeface = Typeface.createFromAsset(mAssetManager, key);
        } catch (RuntimeException e) {
            Log.e("Crate", "Failed to load typeface for key: " + key, e);
            e.printStackTrace();
        } finally {
            if (typeface != null) {
                if (DEBUG) Log.d("Crate", "Typeface loaded for key: " + key);
                mTypefaceCache.put(key, typeface);
            }
        }
        return typeface;
    }

    public void clear() {
        clearBitmapCache();
        clearTypefaceCache();
    }

    public void clearBitmapCache() {
        for (Bitmap bitmap : mBitmapCache.values()) {
            bitmap.recycle();
        }
        mBitmapCache.clear();
    }

    public void clearTypefaceCache() {
        mTypefaceCache.clear();
    }

}
