package com.nmc.android.appupdate

interface InAppUpdateHelper {
    /**
     * function should be called from activity onResume
     * to check if the update is downloaded or still in progress
     */
    fun onResume()

    /**
     * function should be called from activity onDestroy
     * this will unregister the update listener attached for Flexible update
     */
    fun onDestroy()
}