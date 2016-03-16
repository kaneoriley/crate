package me.oriley.crate;

import android.graphics.drawable.PictureDrawable;
import android.support.annotation.Nullable;

import java.io.InputStream;

public class CrateSvgParserDummy implements CrateSvg.Parser {

    @Override
    @Nullable
    public PictureDrawable parseSvg(@Nullable InputStream is) {
        return null;
    }
}
