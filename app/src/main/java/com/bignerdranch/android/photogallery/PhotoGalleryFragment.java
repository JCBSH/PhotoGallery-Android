package com.bignerdranch.android.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import java.util.ArrayList;

/**
 * Created by JCBSH on 1/02/2016.
 */
public class PhotoGalleryFragment extends Fragment{

    private static final String TAG = "PhotoGalleryFragment";

    private GridView mGridView;
    private View mProgressContainer;
    private ArrayList<GalleryItem> mItems;

    private int mNextPage = 1;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mItems = new ArrayList<GalleryItem>();
        new FetchItemsTask().execute(mNextPage);
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
                if(firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount > 0) {
                    new FetchItemsTask().execute(mNextPage);
                }
                Log.d(TAG, "Scrolled: totalItemCount: " + totalItemCount + " firstVisibleItem: "
                        + firstVisibleItem + " visibleItemCount: " + visibleItemCount);


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
            mNextPage++;
            setupAdapter();
            mProgressContainer.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            mProgressContainer.setVisibility(View.VISIBLE);
        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

        public GalleryItemAdapter(ArrayList<GalleryItem> items) {
            super(getActivity(), android.R.layout.simple_gallery_item, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }
    }
}
