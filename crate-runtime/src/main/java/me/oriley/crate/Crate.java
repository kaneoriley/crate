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
                mBitmapCache.put(key, bitmap);
            }
        }
        return bitmap;
    }

    @Nullable
    public Typeface getTypeface(@NonNull FontAsset fontAsset) {
        String key = fontAsset.mPath;
        if (mTypefaceCache.containsKey(key)) {
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
                mTypefaceCache.put(key, typeface);
            }
        }
        return typeface;
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
        if (mSvgCache.containsKey(key)) {
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
                mSvgCache.put(key, drawable);
            }
        }
        return drawable;
    }

    public void clear() {
        clearBitmapCache();
        clearTypefaceCache();
        clearSvgCache();
    }

    public void clearBitmapCache() {
        for (Bitmap bitmap : mBitmapCache.values()) {
            bitmap.recycle();
        }
        mBitmapCache.clear();
    }

    public void clearSvgCache() {
        mSvgCache.clear();
    }

    public void clearTypefaceCache() {
        mTypefaceCache.clear();
    }

}
