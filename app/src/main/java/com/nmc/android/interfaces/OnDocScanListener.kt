package com.nmc.android.interfaces

import android.graphics.Bitmap

interface OnDocScanListener {
    fun addScannedDoc(file: Bitmap?)

    fun getScannedDocs(): List<Bitmap>

    fun removedScannedDoc(file: Bitmap?, index: Int): Boolean

    // isFilterApplied will tell whether the filter is applied to the image or not
    fun replaceScannedDoc(index: Int, newFile: Bitmap?, isFilterApplied: Boolean): Bitmap?

    fun replaceFilterIndex(index: Int, filterIndex: Int)
}
