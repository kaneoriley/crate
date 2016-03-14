package me.oriley.crate;

@SuppressWarnings("unused")
public class Asset {

    final String mPath;

    final String mName;

    Asset(String path, String name) {
        mPath = path;
        mName = name;
    }

    public String getPath() {
        return mPath;
    }

    public String getName() {
        return mName;
    }
}
