package com.nmc.android.utils

import android.app.Activity
import android.graphics.Bitmap
import com.owncloud.android.lib.common.utils.Log_OC
import io.scanbot.sdk.ScanbotSDK
import kotlin.math.roundToInt

object ScanBotSdkUtils {
    private val TAG = ScanBotSdkUtils::class.java.simpleName

    //license key will be valid for application id: com.t_systems.android.webdav & com.t_systems.android.webdav.beta
    //License validity till 25th March 2025
    const val LICENSE_KEY = "Z+GkfzcfWnNtCoGp4OsH9EJg4OuN5v" +
        "BDhPFzHkhecpQaOS4s/r3qRPvKtgpG" +
        "Q89KqfbvPC9Bwx/rPE7GYMmh+YnFIV" +
        "wMD3HcGr4X0ETbH8JdsVP7njFJ5+yi" +
        "xqlS3aSBh3GWtKT+umoTAzXbqF0ZS/" +
        "EGXg0AhwWpQ7Fp+fyNMLwJTxt9/6Ya" +
        "MZ2C0+MVwZyauKjeglILGZrcfenFR+" +
        "a1LjBexcBigcqpMqsd6pDIBwtdp8RY" +
        "spCuYgyQ6Vfb+DYbPts6ynFxXR1bsq" +
        "TRcWBfkVMXIyCSNqgGStHCOZlVvqKo" +
        "anolbemQEGz9lDtigeQN/4txtKX0L9" +
        "2PLfqq6rOh/w==\nU2NhbmJvdFNESw" +
        "pjb20udF9zeXN0ZW1zLmFuZHJvaWQu" +
        "d2ViZGF2fGNvbS50X3N5c3RlbXMuYW" +
        "5kcm9pZC53ZWJkYXYuYmV0YQoxNzQ1" +
        "NjI1NTk5CjExNTU2NzgKMg==\n"

    @JvmStatic
    fun isScanBotLicenseValid(activity: Activity): Boolean {
        // Check the license status:
        val licenseInfo = ScanbotSDK(activity).licenseInfo
        Log_OC.d(TAG, "License status: ${licenseInfo.status}")
        Log_OC.d(TAG, "License isValid: ${licenseInfo.isValid}")

        // Making your call into ScanbotSDK API is safe now.
        // e.g. start barcode scanner
        return licenseInfo.isValid
    }

    @JvmStatic
    fun resizeForPreview(bitmap: Bitmap): Bitmap {
        val maxW = 1000f
        val maxH = 1000f
        val oldWidth = bitmap.width.toFloat()
        val oldHeight = bitmap.height.toFloat()
        val scaleFactor = if (oldWidth > oldHeight) maxW / oldWidth else maxH / oldHeight
        val scaledWidth = (oldWidth * scaleFactor).roundToInt()
        val scaledHeight = (oldHeight * scaleFactor).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)
    }
}