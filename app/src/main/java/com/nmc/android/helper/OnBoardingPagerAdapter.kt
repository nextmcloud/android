package com.nmc.android.helper

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.owncloud.android.databinding.OnboardingElementBinding
import com.owncloud.android.features.FeatureItem
import com.owncloud.android.utils.DisplayUtils

class OnBoardingPagerAdapter(val context: Context, val items: List<FeatureItem>) :
    PagerAdapter() {

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val binding = OnboardingElementBinding.inflate(LayoutInflater.from(context), container, false)

        //val fontColor = ResourcesCompat.getColor(requireContext().resources, R.color.login_text_color, null)
        //binding.ivOnboarding.setImageDrawable(ThemeDrawableUtils.tintDrawable(onBoardingItem?.image ?: 0, fontColor))
        //binding.tvOnboarding.setText(onBoardingItem?.content ?: 0)

        //due to cropping of image in landscape mode we are using fix xy for landscape and
        //center crop for other
        if (com.nmc.android.utils.DisplayUtils.isLandscapeOrientation()) {
            binding.ivOnboarding.scaleType = ImageView.ScaleType.FIT_XY
        } else {
            binding.ivOnboarding.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        Glide.with(context)
            .load(items[position].image)
            .into(binding.ivOnboarding)

        container.addView(binding.root, 0)

        return binding.root
    }
}