/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nmc.android.utils.DisplayUtils.isShowDividerForList
import com.nmc.android.utils.DisplayUtils.isTablet
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.OCFileListAdapter
import com.owncloud.android.ui.decoration.MediaGridItemDecoration
import com.owncloud.android.ui.decoration.SimpleListItemDividerDecoration
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder

class FileListLayoutManager(private val fragment: OCFileListFragment, private val preferences: AppPreferences) {

    /* ---- NMC Customization region start ---- */
    companion object {
        private const val MAX_COLUMN_SIZE_LANDSCAPE = 5

        // this variable will help us to provide number of span count for grid view
        // the width for single item is approx to 180
        private const val GRID_ITEM_DEFAULT_WIDTH: Int = 180

        private const val DEFAULT_FALLBACK_SPAN_COUNT: Int = 4
    }

    var simpleListItemDividerDecoration: SimpleListItemDividerDecoration =
        SimpleListItemDividerDecoration(fragment.context, R.drawable.item_divider, true)
    var mediaGridItemDecoration: MediaGridItemDecoration? = null

    init {
        fragment.context?.let {
            val spacing: Int = it.resources.getDimensionPixelSize(R.dimen.media_grid_spacing)
            mediaGridItemDecoration = MediaGridItemDecoration(spacing)
        }
    }

    /* ---- NMC Customization region end ---- */
    fun sortFiles(sortOrder: FileSortOrder?) {
        fragment.mSortButton?.setText(DisplayUtils.getSortOrderStringId(sortOrder))
        sortOrder?.let { fragment.mAdapter.setSortOrder(fragment.mFile, it) }
    }

    /**
     * Determines whether a folder should be displayed in grid or list view.
     *
     *
     * The preference is checked for the given folder. If the folder itself does not have a preference set,
     * it will fall back to its parent folder recursively until a preference is found (root folder is always set).
     * Additionally, if a search event is active and is of type `SHARED_FILTER`, grid view is disabled.
     *
     * @param folder The folder to check, or `null` to refer to the root folder.
     * @return `true` if the folder should be displayed in grid mode, `false` if list mode is preferred.
     */
    fun isGridViewPreferred(folder: OCFile?): Boolean {
        return if (fragment.searchEvent != null) {
            (fragment.searchEvent.toSearchType() != SearchType.SHARED_FILTER) &&
                OCFileListFragment.FOLDER_LAYOUT_GRID == preferences.getFolderLayout(folder)
        } else {
            OCFileListFragment.FOLDER_LAYOUT_GRID == preferences.getFolderLayout(folder)
        }
    }

    fun setLayoutViewMode() {
        val isGrid = isGridViewPreferred(fragment.mFile)

        if (isGrid) {
            switchToGridView()
        } else {
            switchToListView()
        }

        fragment.setLayoutSwitchButton(isGrid)
    }

    fun setListAsPreferred() {
        preferences.setFolderLayout(fragment.mFile, OCFileListFragment.FOLDER_LAYOUT_LIST)
        switchToListView()
    }

    fun switchToListView() {
        if (fragment.isGridEnabled) {
            switchLayoutManager(false)
        }
        // NMC customization
        addRemoveRecyclerViewItemDecorator()
    }

    fun setGridAsPreferred() {
        preferences.setFolderLayout(fragment.mFile, OCFileListFragment.FOLDER_LAYOUT_GRID)
        switchToGridView()
    }

    fun switchToGridView() {
        if (!fragment.isGridEnabled) {
            switchLayoutManager(true)
        }
        // NMC customization
        addRemoveRecyclerViewItemDecorator()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun switchLayoutManager(grid: Boolean) {
        val recyclerView: RecyclerView? = fragment.recyclerView
        val adapter: OCFileListAdapter? = fragment.adapter
        val context: Context? = fragment.context

        if (context == null || adapter == null || recyclerView == null) {
            Log_OC.e(OCFileListFragment.TAG, "cannot switch layout, arguments are null")
            return
        }

        var position = 0

        if (recyclerView.layoutManager is LinearLayoutManager) {
            val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
            position = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        }

        val layoutManager: RecyclerView.LayoutManager?
        if (grid) {
            layoutManager = GridLayoutManager(context, fragment.columnsCount)
            val gridLayoutManager = layoutManager
            gridLayoutManager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position == fragment.adapter.itemCount - 1 ||
                        position == 0 && fragment.adapter.shouldShowHeader()
                    ) {
                        gridLayoutManager.spanCount
                    } else {
                        1
                    }
                }
            }
        } else {
            layoutManager = LinearLayoutManager(context)
        }

        recyclerView.setLayoutManager(layoutManager)
        // NMC customization
        updateSpanCount(context.resources.configuration);
        recyclerView.scrollToPosition(position)
        adapter.setGridView(grid)
        recyclerView.setAdapter(adapter)
        adapter.notifyDataSetChanged()
    }

    /* ---- NMC Customization region start ---- */
    private fun addRemoveRecyclerViewItemDecorator() {
        val recyclerView: RecyclerView? = fragment.recyclerView
        val context: Context? = fragment.context

        if (context == null || recyclerView == null) {
            Log_OC.e(OCFileListFragment.TAG, "cannot add/remove decorator, arguments are null")
            return
        }
        if (recyclerView.layoutManager is GridLayoutManager) {
            removeItemDecorator()
            if (recyclerView.itemDecorationCount == 0) {
                mediaGridItemDecoration?.let {
                    recyclerView.addItemDecoration(it)
                }
                val padding: Int = context.resources.getDimensionPixelSize(R.dimen.grid_recyclerview_padding)
                recyclerView.setPadding(padding, padding, padding, padding)
            }
        } else {
            removeItemDecorator()
            if (recyclerView.itemDecorationCount == 0 && isShowDividerForList()) {
                recyclerView.addItemDecoration(simpleListItemDividerDecoration)
                recyclerView.setPadding(0, 0, 0, 0)
            }
        }
    }

    /**
     * method to remove the item decorator
     */
    private fun removeItemDecorator() {
        val recyclerView: RecyclerView? = fragment.recyclerView

        if (recyclerView == null) {
            Log_OC.e(OCFileListFragment.TAG, "cannot remove decorator, arguments are null")
            return
        }
        while (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }
    }

    /**
     * method will calculate the number of spans required for grid item and will update the span accordingly
     *
     */
    private fun calculateAndUpdateSpanCount() {
        val recyclerView: RecyclerView? = fragment.recyclerView
        val context: Context? = fragment.context

        if (context == null || recyclerView == null) {
            Log_OC.e(OCFileListFragment.TAG, "cannot calculate and update span count, arguments are null")
            return
        }
        // NMC-4667 fix
        // use display metrics to calculate the span count
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        var newSpanCount = (screenWidthDp / GRID_ITEM_DEFAULT_WIDTH) as Int
        val layoutManager: RecyclerView.LayoutManager? = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {
            if (newSpanCount < 1) {
                newSpanCount = DEFAULT_FALLBACK_SPAN_COUNT
            }
            layoutManager.setSpanCount(newSpanCount)
            layoutManager.requestLayout()
        }
    }

    /**
     * method will update the span count on basis of device orientation for the file listing
     *
     * @param newConfig current configuration
     */
    fun updateSpanCount(newConfig: Configuration) {
        val recyclerView: RecyclerView? = fragment.recyclerView
        val adapter: OCFileListAdapter? = fragment.adapter
        val context: Context? = fragment.context

        if (context == null || adapter == null || recyclerView == null) {
            Log_OC.e(OCFileListFragment.TAG, "cannot update span count, arguments are null")
            return
        }

        //this should only run when current view is not media gallery
        var maxColumnSize = AppPreferencesImpl.DEFAULT_GRID_COLUMN.toInt()
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //add the divider item decorator when orientation is landscape and device is not tablet
            //because we don't have to add divider again as it is already added
            if (!isTablet()) {
                addRemoveRecyclerViewItemDecorator()
            }
            maxColumnSize = MAX_COLUMN_SIZE_LANDSCAPE
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //remove the divider item decorator when orientation is portrait and when device is not tablet
            //because we have to show divider in both landscape and portrait mode
            if (!isTablet()) {
                removeItemDecorator()
            }
            maxColumnSize = AppPreferencesImpl.DEFAULT_GRID_COLUMN.toInt()
        }

        if (fragment.isGridEnabled) {
            //for tablet calculate size on the basis of screen width
            if (isTablet()) {
                calculateAndUpdateSpanCount()
            } else {
                //and for phones directly show the hardcoded column size
                if (recyclerView.layoutManager is GridLayoutManager) {
                    (recyclerView.layoutManager as GridLayoutManager).setSpanCount(maxColumnSize)
                }
            }
        }
    }

    /* ---- NMC Customization region end ---- */
}
