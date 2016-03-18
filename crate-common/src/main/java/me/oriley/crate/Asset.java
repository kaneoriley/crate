package me.oriley.crate;

@SuppressWarnings("unused")
public class Asset {

    final boolean mGzipped;

    final String mPath;

    Asset(String path, boolean gzipped) {
        mPath = path;
        mGzipped = gzipped;
    }

    public boolean isGzipped() {
        return mGzipped;
    }

    public String getPath() {
        return mPath;
    }

    public String getName() {
        String[] path = mPath.split("/");
        return path[path.length - 1];
    }
}
