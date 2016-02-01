package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by JCBSH on 1/02/2016.
 */
public class PhotoGalleryFragment extends Fragment{

    private static final String TAG = "PhotoGalleryFragment";

    private GridView mGridView;
    private View mProgressContainer;
    private ArrayList<GalleryItem> mItems;
    private ThumbnailDownloader<ImageView> mThumbnailThread;
    private static final int PRELOAD_SIZE = 25;

    private int mCurrentPage = 1;
    //IMPORTANT: used to stop onScroll from fetching pages multiple times
    private int mFetchedPage = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mItems = new ArrayList<GalleryItem>();
        new FetchItemsTask().execute(mCurrentPage);
        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler(), getContext());
        mThumbnailThread.setListener(new ThumbnailDownloader.Liistener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail, String url) {
                if (isVisible()) {
                    imageView.setImageBitmap(thumbnail);
                }
            }

        });

        mThumbnailThread.start();
        mThumbnailThread.getLooper();



        Log.i(TAG, "Background thread started");
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount > 0 && mCurrentPage == mFetchedPage) {

                    new FetchItemsTask().execute(mCurrentPage);
                    mCurrentPage++;

                }
                Log.d(TAG, "Scrolled: totalItemCount: " + totalItemCount + " firstVisibleItem: "
                        + firstVisibleItem + " visibleItemCount: " + visibleItemCount);

                if (firstVisibleItem >= PRELOAD_SIZE) {
                    for (int j = firstVisibleItem, i = 0; i < PRELOAD_SIZE; j--, i++) {
                        mThumbnailThread.queuePreloadThumbnail(mItems.get(j).getUrl());
                    }
                }
                int lastVisibleItem = (firstVisibleItem + visibleItemCount);
                if (totalItemCount - lastVisibleItem >= PRELOAD_SIZE) {
                    for (int j = lastVisibleItem, i = 0; i < PRELOAD_SIZE; j++, i++) {
                        mThumbnailThread.queuePreloadThumbnail(mItems.get(j).getUrl());
                    }
                }

            }
        });
        mProgressContainer = v.findViewById(R.id.progressContainer);
        mProgressContainer.setVisibility(View.INVISIBLE);


        setupAdapter();
        return v;
    }

    private void setupAdapter() {
        if (getActivity() == null || mGridView == null) return;
        if (mGridView.getAdapter() == null) {
            mGridView.setAdapter(new GalleryItemAdapter(mItems));
        } else {
            ((GalleryItemAdapter) mGridView.getAdapter()).notifyDataSetChanged();
        }

    }



    private class FetchItemsTask extends AsyncTask<Integer,Void,ArrayList<GalleryItem>> {
        @Override
        protected ArrayList<GalleryItem> doInBackground(Integer... params) {
            publishProgress();
            return new FlickrFetchr().fetchItemsByPage(params[0]);
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            mItems.addAll(items);
            //Log.d(TAG, "total item count: " + mItems.size() + " current page: " + mCurrentPage);

            setupAdapter();
            mFetchedPage++;
            mProgressContainer.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            mProgressContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

        public GalleryItemAdapter(ArrayList<GalleryItem> items) {
            super(getActivity(), 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.gallery_item, parent, false);
            }
            ImageView imageView = (ImageView)convertView
                    .findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(R.drawable.brian_up_close);

            TextView textView = (TextView) convertView.findViewById(R.id.gallery_item_textView);
            textView.setText(String.valueOf(position));
            if (getItem(position).getUrl() == null) {
                return convertView;
            }

            //Log.d("!!!!!", position + ": " + getItem(position).getUrl());

            MyBitMapCache cache = MyBitMapCache.get(getActivity());
            if (cache.getBitmapFromMemCache(getItem(position).getUrl()) != null) {
                if(isVisible()) {
                    imageView.setImageBitmap(cache.getBitmapFromMemCache(getItem(position).getUrl()));
                    //Log.d("!!!!!", position + ": " + getItem(position).getUrl());
                    mThumbnailThread.removeFromQueue(imageView);
                }
            } else {
                mThumbnailThread.queueThumbnail(imageView,getItem(position).getUrl());
            }

            //imageView.setImageResource(R.drawable.brian_up_close);
            return convertView;
        }
    }
}
