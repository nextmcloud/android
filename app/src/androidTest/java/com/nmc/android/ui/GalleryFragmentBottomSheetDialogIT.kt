package com.nmc.android.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetActions
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Random

class GalleryFragmentBottomSheetDialogIT : AbstractIT() {

    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    lateinit var activity: TestActivity
    val random = Random()

    @Before
    fun before() {
        activity = testActivityRule.launchActivity(null)

        createImage(10000001, true, 700, 300)
        createImage(10000002, true, 500, 300)

        createImage(10000007, true, 300, 400)

        showGalleryWithBottomSheet()
    }

    @Test
    fun validateUIElements() {

        onView(withId(R.id.btn_hide_images)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tickMarkShowImages)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.btn_hide_images)).check(matches(withText("Show images")))

        onView(withId(R.id.btn_hide_videos)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tickMarkShowVideos)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.btn_hide_videos)).check(matches(withText("Show videos")))

        onView(withId(R.id.btn_select_media_folder)).check(matches(isCompletelyDisplayed()))
    }

    private fun showGalleryWithBottomSheet(): GalleryFragmentBottomSheetDialog {
        val sut = GalleryFragment()
        activity.addFragment(sut)

        shortSleep()

        val sheet = GalleryFragmentBottomSheetDialog(object : GalleryFragmentBottomSheetActions {
            override fun updateMediaContent(mediaState: GalleryFragmentBottomSheetDialog.MediaState) {
            }

            override fun selectMediaFolder() {
            }
        })

        sheet.show(activity.supportFragmentManager, "bottom_sheet")
        return sheet
    }

    private fun createImage(id: Int, createPreview: Boolean = true, width: Int? = null, height: Int? = null) {
        val defaultSize = ThumbnailsCacheManager.getThumbnailDimension().toFloat()
        val file = OCFile("/$id.png").apply {
            fileId = id.toLong()
            remoteId = "$id"
            mimeType = "image/png"
            isPreviewAvailable = true
            modificationTimestamp = (1658475504 + id.toLong()) * 1000
            imageDimension = ImageDimension(width?.toFloat() ?: defaultSize, height?.toFloat() ?: defaultSize)
            storageManager.saveFile(this)
        }

        if (!createPreview) {
            return
        }

        // create dummy thumbnail
        var w: Int
        var h: Int
        if (width == null || height == null) {
            if (random.nextBoolean()) {
                // portrait
                w = (random.nextInt(3) + 2) * 100 // 200-400
                h = (random.nextInt(5) + 4) * 100 // 400-800
            } else {
                // landscape
                w = (random.nextInt(5) + 4) * 100 // 400-800
                h = (random.nextInt(3) + 2) * 100 // 200-400
            }
        } else {
            w = width
            h = height
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, Paint().apply { color = Color.BLACK })
        }
        ThumbnailsCacheManager.addBitmapToCache(PREFIX_RESIZED_IMAGE + file.remoteId, bitmap)

        assertNotNull(ThumbnailsCacheManager.getBitmapFromDiskCache(PREFIX_RESIZED_IMAGE + file.remoteId))

        Log_OC.d("Gallery_thumbnail", "created $id with ${bitmap.width} x ${bitmap.height}")
    }

    @After
    override fun after() {
        ThumbnailsCacheManager.clearCache()
        super.after()
    }
}
