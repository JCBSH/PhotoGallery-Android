package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by JCBSH on 1/02/2016.
 */
public class MyBitMapCache {
    private static MyBitMapCache sMyBitMapCache;
    private Context mContext;
    private LruCache<String, Bitmap> mPhotoCache;
    private MyBitMapCache (Context context) {
        mContext = context;
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 4;

        mPhotoCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
            }
        };

    }

    public static MyBitMapCache get(Context context) {
        if (sMyBitMapCache == null) {
            sMyBitMapCache = new MyBitMapCache(context.getApplicationContext());
        }
        return sMyBitMapCache;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mPhotoCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mPhotoCache.get(key);
    }
}
