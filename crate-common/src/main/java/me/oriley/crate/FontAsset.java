package me.oriley.crate;

@SuppressWarnings("unused")
public class FontAsset extends Asset {

    final String mFontName;

    FontAsset(String path, String name, String fontName) {
        super(path, name);
        mFontName = fontName;
    }

    public String getFontName() {
        return mFontName;
    }
}