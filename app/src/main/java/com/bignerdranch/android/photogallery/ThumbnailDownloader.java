package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by JCBSH on 1/02/2016.
 */
public class ThumbnailDownloader<Token> extends HandlerThread{
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_PRELOAD = 1;
    private Context mContext;
    private Handler mHandler;

    private Map<Token, String> requestMap =
            Collections.synchronizedMap(new HashMap<Token, String>());
    private ArrayList<String> mRequestPreloadQueue = new ArrayList<String>();

    private Handler mResponseHandler;
    private Liistener<Token> mListener;



    public interface Liistener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail, String url);
    }
    public void setListener(Liistener<Token> listener) {
        mListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    public ThumbnailDownloader(Handler responseHandler, Context context) {
        super(TAG);
        mResponseHandler = responseHandler;
        mContext = context;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    Token token = (Token)msg.obj;
                    Log.i(TAG, "Got a request for url: " + requestMap.get(token));
                    handleRequest(token);
                }
                if(msg.what == MESSAGE_PRELOAD) {
                    handlePreloadRequest((String) msg.obj);
                }
            }
        };
    }

    public void queueThumbnail(Token token, String url) {
        mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(MESSAGE_DOWNLOAD, token));
        requestMap.put(token, url);
        Log.i(TAG, "Got an URL: " + url);
    }

    public void queuePreloadThumbnail(String url) {

        if (!mRequestPreloadQueue.contains(url)) {
            mRequestPreloadQueue.add(url);
            mHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
        }
    }


    public void removeFromQueue(Token imageView) {
        requestMap.remove(imageView);
    }

    private void handleRequest(final Token token) {
        try {
            final String url = requestMap.get(token);
            if (url == null)
                return;
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            MyBitMapCache.get(mContext).addBitmapToMemoryCache(url, bitmap);
            Log.i(TAG, "Bitmap created");
            mResponseHandler.post(new Runnable() {
                public void run() {
                    if (requestMap.get(token) != url)
                        return;
                    requestMap.remove(token);
                    mListener.onThumbnailDownloaded(token, bitmap, url);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    private void handlePreloadRequest(final String url) {
        try {
            MyBitMapCache cache = MyBitMapCache.get(mContext);
            if (url == null || !mRequestPreloadQueue.contains(url) || cache.getBitmapFromMemCache(url) != null)
                return;
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

            Log.i(TAG, "Bitmap created fro preload");
            mRequestPreloadQueue.remove(url);
            cache.addBitmapToMemoryCache(url, bitmap);
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void clearQueue() {
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        mHandler.removeMessages(MESSAGE_PRELOAD);
        requestMap.clear();
    }

    @Override
    public boolean quit() {
        return super.quit();
    }
}
