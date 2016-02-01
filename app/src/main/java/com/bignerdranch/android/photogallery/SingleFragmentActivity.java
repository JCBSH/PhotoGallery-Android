package com.bignerdranch.android.photogallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

/**
 * Created by JCBSH on 19/01/2016.
 */
public abstract class SingleFragmentActivity extends ActionBarActivity {


    public static final String LIFE_TAG = "life_ListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LIFE_TAG, "onCreate() ");
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment =  createFragment();

            //Log.d(LIFE_TAG, "before transaction ");
            fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();
            //Log.d(LIFE_TAG, "after transaction ");
        }

    }

    protected int getLayoutResId() {
        return R.layout.activity_fragment;
    }

    @Override
    protected void onStart() {
        Log.d(LIFE_TAG, "onStart() ");
        super.onStart();


        Log.d(LIFE_TAG, "end of onStart() ");
    }

    @Override
    protected void onResume() {
        Log.d(LIFE_TAG, "onResume() ");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(LIFE_TAG, "onPause() ");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(LIFE_TAG, "onStop() ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(LIFE_TAG, "onDestroy() ");
        super.onDestroy();
    }


    protected abstract Fragment createFragment();


}
