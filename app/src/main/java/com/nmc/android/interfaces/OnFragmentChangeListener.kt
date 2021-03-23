package com.nmc.android.interfaces

import androidx.fragment.app.Fragment

interface OnFragmentChangeListener {
    fun onReplaceFragment(fragment: Fragment, tag: String, addToBackStack: Boolean)
}
