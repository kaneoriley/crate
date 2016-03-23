package me.oriley.cratesample.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import me.oriley.cratesample.R;

public class CrateFontView extends CardView {

    @Bind(R.id.text_view)
    TextView mTextView;

    @Bind(R.id.tag_view)
    TagView mTagView;


    public CrateFontView(@NonNull Context context) {
        this(context, null);
    }

    public CrateFontView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CrateFontView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.crate_font_view, this);
        ButterKnife.bind(this);
    }


    @NonNull
    public ViewPropertyAnimator animateText() {
        return mTextView.animate();
    }

    public void setText(@Nullable String text) {
        mTextView.setText(text);
    }

    public void setTextAlpha(float alpha) {
        mTextView.setAlpha(alpha);
    }

    public void setTextRotation(float rotationX, float rotationY) {
        mTextView.setRotationX(rotationX);
        mTextView.setRotationY(rotationY);
    }

    public void setTextScale(float scaleX, float scaleY) {
        mTextView.setScaleX(scaleX);
        mTextView.setScaleY(scaleY);
    }

    public void setTextTranslation(float translationX, float translationY) {
        mTextView.setTranslationX(translationX);
        mTextView.setTranslationY(translationY);
    }

    public void setTextTypeface(@Nullable Typeface typeface) {
        mTextView.setTypeface(typeface);
    }

    public void setTagColor(@ColorInt int color) {
        mTagView.setTagColor(color);
    }
}