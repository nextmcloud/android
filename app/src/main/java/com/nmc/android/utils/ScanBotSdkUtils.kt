package com.nmc.android.utils

import android.app.Activity
import android.graphics.Bitmap
import com.owncloud.android.lib.common.utils.Log_OC
import io.scanbot.sdk.ScanbotSDK
import kotlin.math.roundToInt

object ScanBotSdkUtils {
    private val TAG = ScanBotSdkUtils::class.java.simpleName

    //license key will be valid for application id: com.t_systems.android.webdav & com.t_systems.android.webdav.beta
    //License validity until end of 2026
    const val LICENSE_KEY = "M7j1BCE/NVedJyWcstLjZvNQdeluHx" +
        "XkMNsHYeuQ8o4MKhPITd/xJDsc9xfY" +
        "JRPSCA5UpXbzVObI5MMeoFiUWMPCR6" +
        "yoOe1Ghj1UjVIVS6lLW/Unipe+Pozm" +
        "8TFO+l0Q0TAuWXXqwGZJt4dHy1t9t9" +
        "QUy4i1q90VuVs1I0k4C3ScZNr2R+aT" +
        "z4Hht5J5Svu4RwVPqcOiEuoAMYj8+a" +
        "bvidW0CQK3+12ryaV64qzLrFtcHAb7" +
        "Wx3aqZH7WXT/F4uZTYpaau6lzU+xIY" +
        "YxtC8SS+6+nb2l6V2hIqmpEJwS1z0p" +
        "uUbO7D7O5Gm3aSaOk+8xqX2mNuk4dX" +
        "EyTSR36bFuVA==\nU2NhbmJvdFNESw" +
        "pjb20udF9zeXN0ZW1zLmFuZHJvaWQu" +
        "d2ViZGF2fGNvbS50X3N5c3RlbXMuYW" +
        "5kcm9pZC53ZWJkYXYuYmV0YQoxODAz" +
        "ODU5MTk5CjExNTU2NzgKMg==\n"

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