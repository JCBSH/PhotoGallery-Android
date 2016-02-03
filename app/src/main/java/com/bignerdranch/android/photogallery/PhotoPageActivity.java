package com.bignerdranch.android.photogallery;

import android.support.v4.app.Fragment;

/**
 * Created by JCBSH on 3/02/2016.
 */
public class PhotoPageActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return new PhotoPageFragment();
    }
}