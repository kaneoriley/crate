package me.oriley.cratesample.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import butterknife.Bind;
import butterknife.ButterKnife;
import me.oriley.cratesample.R;

public class CrateSvgView extends CardView {

    @Bind(R.id.image_view)
    ImageView mImageView;

    @Bind(R.id.tag_view)
    TagView mTagView;


    public CrateSvgView(@NonNull Context context) {
        this(context, null);
    }

    public CrateSvgView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CrateSvgView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.crate_svg_view, this);
        ButterKnife.bind(this);
    }


    @NonNull
    public ViewPropertyAnimator animateBitmap() {
        return mImageView.animate();
    }

    public void setBitmap(@Nullable Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
    }

    public void setBitmapAlpha(float alpha) {
        mImageView.setAlpha(alpha);
    }

    public void setBitmapRotation(float rotation) {
        mImageView.setRotation(rotation);
    }

    public void setBitmapScale(float scaleX, float scaleY) {
        mImageView.setScaleX(scaleX);
        mImageView.setScaleY(scaleY);
    }

    public void setTagColor(@ColorInt int color) {
        mTagView.setTagColor(color);
    }
}