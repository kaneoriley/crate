package me.oriley.crate;

@SuppressWarnings("unused")
public class VideoAsset extends Asset {

    final int mWidth;

    final int mHeight;

    VideoAsset(String path, boolean gzipped, int width, int height) {
        super(path, gzipped);
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}