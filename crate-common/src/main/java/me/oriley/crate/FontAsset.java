package me.oriley.crate;

@SuppressWarnings("unused")
public class FontAsset extends Asset {

    final String mFontName;

    FontAsset(String path, boolean gzipped, String fontName) {
        super(path, gzipped);
        mFontName = fontName;
    }

    public String getFontName() {
        return mFontName;
    }
}