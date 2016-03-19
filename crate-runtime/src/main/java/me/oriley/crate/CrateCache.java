package me.oriley.crate;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.*;
import java.util.Map.Entry;

public final class CrateCache<K, V> {

    private static final String TAG = CrateCache.class.getSimpleName();

    private static final int INITIAL_MAP_SIZE = 10;
    private static final float MAP_LOAD_FACTOR = 0.75f;

    @NonNull
    private final Map<K, V> mCache;

    private final int mMaxSize;

    public CrateCache(int maxSize, boolean lruCache) {
        mMaxSize = maxSize;
        mCache = Collections.synchronizedMap(new LinkedHashMap<K, V>(INITIAL_MAP_SIZE, MAP_LOAD_FACTOR, lruCache));
    }

    @Nullable
    public V get(@NonNull K id) {
        return mCache.get(id);
    }

    public void put(@NonNull K id, @Nullable V value) {
        if (value == null) {
            return;
        }

        int currentSize = mCache.size();
        if (currentSize >= mMaxSize) {
            Iterator<Entry<K, V>> iterator = mCache.entrySet().iterator();
            while (currentSize > mMaxSize && iterator.hasNext()) {
                iterator.remove();
                currentSize--;
            }
        }
        mCache.put(id, value);
    }

    @Nullable
    public V remove(@NonNull K id) {
        return mCache.remove(id);
    }

    public Collection<V> values() {
        return mCache.values();
    }

    public boolean containsKey(@NonNull K id) {
        return mCache.containsKey(id);
    }

    public int maxSize() {
        return mMaxSize;
    }

    public int size() {
        return mCache.size();
    }

    public void clear() {
        mCache.clear();
    }
}