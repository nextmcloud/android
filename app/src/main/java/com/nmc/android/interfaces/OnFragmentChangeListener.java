package com.nmc.android.interfaces;

import androidx.fragment.app.Fragment;

public interface OnFragmentChangeListener {
    void onReplaceFragment(Fragment fragment, String tag, boolean addToBackStack);
}