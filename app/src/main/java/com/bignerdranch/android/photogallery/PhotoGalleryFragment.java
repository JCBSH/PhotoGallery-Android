package com.bignerdranch.android.photogallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    public static final String LIFE_TAG = "life_PhotoGalleryFragment";
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
    public void onAttach(Activity activity) {
        Log.d(LIFE_TAG, "onAttach() ");
        super.onAttach(activity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(LIFE_TAG, "onCreate() ");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mItems = new ArrayList<GalleryItem>();
        updateItems();
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
        Log.d(LIFE_TAG, "onCreateView() ");
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount > 0 && mCurrentPage == mFetchedPage) {

                    updateItems();
                    mCurrentPage++;

                }
//                Log.d(TAG, "Scrolled: totalItemCount: " + totalItemCount + " firstVisibleItem: "
//                        + firstVisibleItem + " visibleItemCount: " + visibleItemCount);

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


    @Override
    @TargetApi(11)
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // pull out the SearchView
            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

            // get the data from our searchable.xml as a SearchableInfo
            SearchManager searchManager = (SearchManager)getActivity()
                    .getSystemService(Context.SEARCH_SERVICE);
            ComponentName name = getActivity().getComponentName();
            SearchableInfo searchInfo = searchManager.getSearchableInfo(name);

            searchView.setSearchableInfo(searchInfo);
//
       }
        //searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//                @Override
//                public boolean onQueryTextSubmit(String s) {
//                    PreferenceManager.getDefaultSharedPreferences(getActivity())
//                            .edit()
//                            .putString(FlickrFetchr.PREF_SEARCH_QUERY, s)
//                            .commit();
//                    return true;
//                }
//
//                @Override
//                public boolean onQueryTextChange(String s) {
//                    return true;
//                }
//            });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_search:
                getActivity().onSearchRequested();
                return true;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                        .commit();
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

            Activity activity = getActivity();
            if (activity == null)
                return new ArrayList<GalleryItem>();
            String query = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
            if (query != null) {
                return new FlickrFetchr().search(query);
            } else {
                return new FlickrFetchr().fetchItemsByPage(params[0]);
            }
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
    public void onStart() {
        Log.d(LIFE_TAG, "onStart() ");
        super.onStart();
        //showPhoto();
    }

    @Override
    public void onResume() {
        Log.d(LIFE_TAG, "onResume() ");
        //showPhoto();
        super.onResume();

    }

    @Override
    public void onPause() {
        Log.d(LIFE_TAG, "onPause() ");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(LIFE_TAG, "onStop() ");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(LIFE_TAG, "onDestroyView() ");
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    public void onDestroy() {
        Log.d(LIFE_TAG, "onDestroy() ");
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onDetach() {
        Log.d(LIFE_TAG, "onDetach() ");
        super.onDetach();
    }

    public void updateItems() {
        Log.d(TAG, "Updating question text for question #",
                new Exception());
        new FetchItemsTask().execute(mCurrentPage);
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
