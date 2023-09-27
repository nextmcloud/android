package com.nmc.android.helper

import com.owncloud.android.R
import com.owncloud.android.features.FeatureItem
import com.owncloud.android.utils.DisplayUtils

class OnBoardingUtils {

    companion object {
        private val DEFAULT_IMAGES =
            arrayOf(
                R.drawable.intro_screen_first, R.drawable.intro_screen_second, R.drawable
                    .intro_screen_third
            )
        private val TAB_PORT_IMAGES =
            arrayOf(
                R.drawable.intro_screen_first_port_tab, R.drawable.intro_screen_second_port_tab, R.drawable
                    .intro_screen_third_port_tab
            )
        private val TAB_LAND_IMAGES =
            arrayOf(
                R.drawable.intro_screen_first_land_tab, R.drawable.intro_screen_second_land_tab, R.drawable
                    .intro_screen_third_land_tab
            )

        fun getOnBoardingItems(): List<FeatureItem> {
            val onBoardingItems = mutableListOf<FeatureItem>()
            val onBoardingImages = getOnBoardingImages()
            for (i in onBoardingImages.indices) {
                val onBoardingItem = FeatureItem(onBoardingImages[i], R.string.empty, R.string.empty, false, false)
                onBoardingItems.add(onBoardingItem)
            }
            return onBoardingItems
        }

        private fun getOnBoardingImages(): Array<Int> {
            return if (com.nmc.android.utils.DisplayUtils.isTablet()) {
                if (com.nmc.android.utils.DisplayUtils.isLandscapeOrientation()) {
                    TAB_LAND_IMAGES
                } else {
                    TAB_PORT_IMAGES
                }
            } else {
                DEFAULT_IMAGES
            }
        }
    }
}