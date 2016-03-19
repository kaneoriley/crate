/*
* Copyright (C) 2015 Vincent Mi
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package me.oriley.cratesample.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.support.annotation.*;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.oriley.cratesample.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.oriley.cratesample.widget.RoundedDrawable.DEFAULT_BORDER_COLOR;

@SuppressWarnings("UnusedDeclaration")
@Accessors(prefix = "m")
public class RoundedImageView extends AppCompatImageView {

    private static final String TAG = RoundedImageView.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            Corner.TOP_LEFT, Corner.TOP_RIGHT,
            Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT
    })
    public @interface Corner {
        int TOP_LEFT = 0;
        int TOP_RIGHT = 1;
        int BOTTOM_RIGHT = 2;
        int BOTTOM_LEFT = 3;
    }

    // Constants for tile mode attributes
    private static final int TILE_MODE_UNDEFINED = -2;
    private static final int TILE_MODE_CLAMP = 0;
    private static final int TILE_MODE_REPEAT = 1;
    private static final int TILE_MODE_MIRROR = 2;

    public static final float DEFAULT_RADIUS = 0f;

    public static final int DEFAULT_BORDER_WIDTH = 0;

    public static final Shader.TileMode DEFAULT_TILE_MODE = Shader.TileMode.CLAMP;

    private static final ScaleType[] SCALE_TYPES = {
            ScaleType.MATRIX,
            ScaleType.FIT_XY,
            ScaleType.FIT_START,
            ScaleType.FIT_CENTER,
            ScaleType.FIT_END,
            ScaleType.CENTER,
            ScaleType.CENTER_CROP,
            ScaleType.CENTER_INSIDE
    };

    @NonNull
    private final float[] mCornerRadii = new float[]{DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS};

    @Nullable
    private Drawable mBackgroundDrawable;

    private float mBorderWidth = DEFAULT_BORDER_WIDTH;

    @Nullable
    private ColorFilter mColorFilter;

    @Nullable
    private Drawable mDrawable;

    @Getter
    @NonNull
    private ScaleType mScaleType = ScaleType.FIT_CENTER;

    @Getter
    @NonNull
    private ColorStateList mBorderColors = ColorStateList.valueOf(DEFAULT_BORDER_COLOR);

    @Getter
    @NonNull
    private Shader.TileMode mTileModeX = DEFAULT_TILE_MODE;

    @Getter
    @NonNull
    private Shader.TileMode mTileModeY = DEFAULT_TILE_MODE;

    private int mResource;

    private int mBackgroundResource;

    @Getter
    private boolean mMutateBackground;

    @Getter
    private boolean mOval;

    private boolean mHasColorFilter;

    private boolean mColorMod;

    public RoundedImageView(@NonNull Context context) {
        this(context, null);
    }

    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView, defStyle, 0);

            int index = a.getInt(R.styleable.RoundedImageView_android_scaleType, -1);
            if (index >= 0) {
                setScaleType(SCALE_TYPES[index]);
            }

            float cornerRadiusOverride =
                    a.getDimensionPixelSize(R.styleable.RoundedImageView_android_radius, -1);

            mCornerRadii[Corner.TOP_LEFT] =
                    a.getDimensionPixelSize(R.styleable.RoundedImageView_android_topLeftRadius, -1);
            mCornerRadii[Corner.TOP_RIGHT] =
                    a.getDimensionPixelSize(R.styleable.RoundedImageView_android_topRightRadius, -1);
            mCornerRadii[Corner.BOTTOM_RIGHT] =
                    a.getDimensionPixelSize(R.styleable.RoundedImageView_android_bottomRightRadius, -1);
            mCornerRadii[Corner.BOTTOM_LEFT] =
                    a.getDimensionPixelSize(R.styleable.RoundedImageView_android_bottomLeftRadius, -1);

            boolean any = false;
            for (int i = 0, len = mCornerRadii.length; i < len; i++) {
                if (mCornerRadii[i] < 0) {
                    mCornerRadii[i] = 0f;
                } else {
                    any = true;
                }
            }

            if (!any) {
                if (cornerRadiusOverride < 0) {
                    cornerRadiusOverride = DEFAULT_RADIUS;
                }
                for (int i = 0, len = mCornerRadii.length; i < len; i++) {
                    mCornerRadii[i] = cornerRadiusOverride;
                }
            }

            mBorderWidth = a.getDimensionPixelSize(R.styleable.RoundedImageView_borderWidth, DEFAULT_BORDER_WIDTH);

            ColorStateList borderColors = a.getColorStateList(R.styleable.RoundedImageView_borderColor);
            if (borderColors != null) {
                mBorderColors = borderColors;
            }

            mMutateBackground = a.getBoolean(R.styleable.RoundedImageView_mutateBackground, false);
            mOval = a.getBoolean(R.styleable.RoundedImageView_asOval, false);

            int tileModeIndex = a.getInt(R.styleable.RoundedImageView_tileMode, TILE_MODE_UNDEFINED);
            if (tileModeIndex != TILE_MODE_UNDEFINED) {
                Shader.TileMode tileMode = parseTileMode(tileModeIndex);
                if (tileMode != null) {
                    setTileModeX(tileMode);
                    setTileModeY(tileMode);
                }
            }

            tileModeIndex = a.getInt(R.styleable.RoundedImageView_tileModeX, TILE_MODE_UNDEFINED);
            if (tileModeIndex != TILE_MODE_UNDEFINED) {
                Shader.TileMode tileMode = parseTileMode(tileModeIndex);
                if (tileMode != null) {
                    setTileModeX(tileMode);
                }
            }

            tileModeIndex = a.getInt(R.styleable.RoundedImageView_tileModeY, TILE_MODE_UNDEFINED);
            if (tileModeIndex != TILE_MODE_UNDEFINED) {
                Shader.TileMode tileMode = parseTileMode(tileModeIndex);
                if (tileMode != null) {
                    setTileModeY(tileMode);
                }
            }

            a.recycle();
        }

        updateDrawableAttrs();
        updateBackgroundDrawableAttrs(true);

        if (mMutateBackground) {
            // when setBackground() is called by View constructor, mMutateBackground is not loaded from the attribute,
            // so it's false by default, what doesn't allow to create the RoundedDrawable. At this point, after load
            // mMutateBackground and updated BackgroundDrawable to RoundedDrawable, the View's background drawable needs
            // to be changed to this new drawable.

            //noinspection deprecation
            super.setBackgroundDrawable(mBackgroundDrawable);
        }
    }

    @Nullable
    private static Shader.TileMode parseTileMode(int tileMode) {
        switch (tileMode) {
            case TILE_MODE_CLAMP:
                return Shader.TileMode.CLAMP;
            case TILE_MODE_REPEAT:
                return Shader.TileMode.REPEAT;
            case TILE_MODE_MIRROR:
                return Shader.TileMode.MIRROR;
            default:
                return null;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    public void setScaleType(@NonNull ScaleType scaleType) {
        if (mScaleType != scaleType) {
            mScaleType = scaleType;

            switch (scaleType) {
                case CENTER:
                case CENTER_CROP:
                case CENTER_INSIDE:
                case FIT_CENTER:
                case FIT_START:
                case FIT_END:
                case FIT_XY:
                    super.setScaleType(ScaleType.FIT_XY);
                    break;
                default:
                    super.setScaleType(scaleType);
                    break;
            }

            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            invalidate();
        }
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        mResource = 0;
        mDrawable = RoundedDrawable.fromDrawable(drawable);
        updateDrawableAttrs();
        super.setImageDrawable(mDrawable);
    }

    @Override
    public void setImageBitmap(@Nullable Bitmap bm) {
        mResource = 0;
        mDrawable = RoundedDrawable.fromBitmap(bm);
        updateDrawableAttrs();
        super.setImageDrawable(mDrawable);
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        if (mResource != resId) {
            mResource = resId;
            mDrawable = resolveResource();
            updateDrawableAttrs();
            super.setImageDrawable(mDrawable);
        }
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        super.setImageURI(uri);
        setImageDrawable(getDrawable());
    }

    @Nullable
    private Drawable resolveResource() {
        Resources res = getResources();
        if (res == null) {
            return null;
        }

        Drawable d = null;

        if (mResource != 0) {
            try {
                //noinspection deprecation
                d = res.getDrawable(mResource);
            } catch (Exception e) {
                Log.w(TAG, "Unable to find resource: " + mResource, e);
                // Don't try again.
                mResource = 0;
            }
        }
        return RoundedDrawable.fromDrawable(d);
    }

    @Override
    public void setBackground(@Nullable Drawable background) {
        //noinspection deprecation
        setBackgroundDrawable(background);
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        if (mBackgroundResource != resId) {
            mBackgroundResource = resId;
            mBackgroundDrawable = resolveBackgroundResource();
            //noinspection deprecation
            setBackgroundDrawable(mBackgroundDrawable);
        }
    }

    @Override
    public void setBackgroundColor(@ColorInt int color) {
        mBackgroundDrawable = new ColorDrawable(color);
        //noinspection deprecation
        setBackgroundDrawable(mBackgroundDrawable);
    }

    @Nullable
    private Drawable resolveBackgroundResource() {
        Resources res = getResources();
        if (res == null) {
            return null;
        }

        Drawable d = null;

        if (mBackgroundResource != 0) {
            try {
                //noinspection deprecation
                d = res.getDrawable(mBackgroundResource);
            } catch (Exception e) {
                Log.w(TAG, "Unable to find resource: " + mBackgroundResource, e);
                // Don't try again.
                mBackgroundResource = 0;
            }
        }
        return RoundedDrawable.fromDrawable(d);
    }

    private void updateDrawableAttrs() {
        updateAttrs(mDrawable, mScaleType);
    }

    private void updateBackgroundDrawableAttrs(boolean convert) {
        if (mMutateBackground) {
            if (convert) {
                mBackgroundDrawable = RoundedDrawable.fromDrawable(mBackgroundDrawable);
            }
            updateAttrs(mBackgroundDrawable, ScaleType.FIT_XY);
        }
    }

    @Override
    public void setColorFilter(@NonNull ColorFilter cf) {
        if (mColorFilter != cf) {
            mColorFilter = cf;
            mHasColorFilter = true;
            mColorMod = true;
            applyColorMod();
            invalidate();
        }
    }

    private void applyColorMod() {
        // Only mutate and apply when modifications have occurred. This should
        // not reset the mColorMod flag, since these filters need to be
        // re-applied if the Drawable is changed.
        if (mDrawable != null && mColorMod) {
            mDrawable = mDrawable.mutate();
            if (mHasColorFilter) {
                mDrawable.setColorFilter(mColorFilter);
            }
            // TODO: support, eventually...
            //mDrawable.setXfermode(mXfermode);
            //mDrawable.setAlpha(mAlpha * mViewAlphaScale >> 8);
        }
    }

    private void updateAttrs(@Nullable Drawable drawable, @NonNull ScaleType scaleType) {
        if (drawable == null) {
            return;
        }

        if (drawable instanceof RoundedDrawable) {
            RoundedDrawable roundedDrawable = (RoundedDrawable) drawable;

            roundedDrawable.setScaleType(scaleType)
                    .setBorderWidth(mBorderWidth)
                    .setBorderColor(mBorderColors)
                    .setOval(mOval)
                    .setTileModeX(mTileModeX)
                    .setTileModeY(mTileModeY);

            roundedDrawable.setCornerRadius(
                    mCornerRadii[Corner.TOP_LEFT],
                    mCornerRadii[Corner.TOP_RIGHT],
                    mCornerRadii[Corner.BOTTOM_RIGHT],
                    mCornerRadii[Corner.BOTTOM_LEFT]);

            applyColorMod();
        } else if (drawable instanceof LayerDrawable) {
            // loop through layers to and set drawable attrs
            LayerDrawable ld = ((LayerDrawable) drawable);
            for (int i = 0, layers = ld.getNumberOfLayers(); i < layers; i++) {
                updateAttrs(ld.getDrawable(i), scaleType);
            }
        }
    }

    @Override
    @Deprecated
    public void setBackgroundDrawable(@Nullable Drawable background) {
        mBackgroundDrawable = background;
        updateBackgroundDrawableAttrs(true);
        //noinspection deprecation
        super.setBackgroundDrawable(mBackgroundDrawable);
    }

    /**
     * @return the largest corner radius.
     */
    public float getCornerRadius() {
        return getMaxCornerRadius();
    }

    /**
     * @return the largest corner radius.
     */
    public float getMaxCornerRadius() {
        float maxRadius = 0;
        for (float r : mCornerRadii) {
            maxRadius = Math.max(r, maxRadius);
        }
        return maxRadius;
    }

    /**
     * Get the corner radius of a specified corner.
     *
     * @param corner the corner.
     * @return the radius.
     */
    public float getCornerRadius(@Corner int corner) {
        return mCornerRadii[corner];
    }

    /**
     * Set all the corner radii from a dimension resource id.
     *
     * @param resId dimension resource id of radii.
     */
    public void setCornerRadiusDimen(@DimenRes int resId) {
        float radius = getResources().getDimension(resId);
        setCornerRadius(radius, radius, radius, radius);
    }

    /**
     * Set the corner radius of a specific corner from a dimension resource id.
     *
     * @param corner the corner to set.
     * @param resId  the dimension resource id of the corner radius.
     */
    public void setCornerRadiusDimen(@Corner int corner, @DimenRes int resId) {
        setCornerRadius(corner, getResources().getDimensionPixelSize(resId));
    }

    /**
     * Set the corner radii of all corners in px.
     *
     * @param radius the radius to set.
     */
    public void setCornerRadius(float radius) {
        setCornerRadius(radius, radius, radius, radius);
    }

    /**
     * Set the corner radius of a specific corner in px.
     *
     * @param corner the corner to set.
     * @param radius the corner radius to set in px.
     */
    public void setCornerRadius(@Corner int corner, float radius) {
        if (mCornerRadii[corner] != radius) {
            mCornerRadii[corner] = radius;
            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            invalidate();
        }
    }

    /**
     * Set the corner radii of each corner individually. Currently only one unique nonzero value is
     * supported.
     *
     * @param topLeft     radius of the top left corner in px.
     * @param topRight    radius of the top right corner in px.
     * @param bottomRight radius of the bottom right corner in px.
     * @param bottomLeft  radius of the bottom left corner in px.
     */
    public void setCornerRadius(float topLeft, float topRight, float bottomLeft, float bottomRight) {
        if (mCornerRadii[Corner.TOP_LEFT] != topLeft
                || mCornerRadii[Corner.TOP_RIGHT] != topRight
                || mCornerRadii[Corner.BOTTOM_RIGHT] != bottomRight
                || mCornerRadii[Corner.BOTTOM_LEFT] != bottomLeft) {
            mCornerRadii[Corner.TOP_LEFT] = topLeft;
            mCornerRadii[Corner.TOP_RIGHT] = topRight;
            mCornerRadii[Corner.BOTTOM_LEFT] = bottomLeft;
            mCornerRadii[Corner.BOTTOM_RIGHT] = bottomRight;

            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            invalidate();
        }
    }

    public void setBorderWidth(@DimenRes int resId) {
        setBorderWidth(getResources().getDimension(resId));
    }

    public void setBorderWidth(float width) {
        if (mBorderWidth != width) {
            mBorderWidth = width;
            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            invalidate();
        }
    }

    @ColorInt
    public int getBorderColor() {
        return mBorderColors.getDefaultColor();
    }

    public void setBorderColor(@ColorInt int color) {
        setBorderColor(ColorStateList.valueOf(color));
    }

    public void setBorderColor(@Nullable ColorStateList colors) {
        if (!mBorderColors.equals(colors)) {
            mBorderColors = (colors != null) ? colors : ColorStateList.valueOf(DEFAULT_BORDER_COLOR);
            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            if (mBorderWidth > 0) {
                invalidate();
            }
        }
    }

    public void setOval(boolean oval) {
        if (mOval != oval) {
            mOval = oval;
            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            invalidate();
        }
    }

    public void setTileModeX(@NonNull Shader.TileMode tileModeX) {
        if (mTileModeX != tileModeX) {
            this.mTileModeX = tileModeX;
            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            invalidate();
        }
    }

    public void setTileModeY(@NonNull Shader.TileMode tileModeY) {
        if (mTileModeY != tileModeY) {
            mTileModeY = tileModeY;
            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            invalidate();
        }
    }

    public void mutateBackground(boolean mutate) {
        if (mMutateBackground != mutate) {
            mMutateBackground = mutate;
            updateBackgroundDrawableAttrs(true);
            invalidate();
        }
    }
}
