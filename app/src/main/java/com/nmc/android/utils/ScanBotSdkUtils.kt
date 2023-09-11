package com.nmc.android.utils

import android.app.Activity
import android.graphics.Bitmap
import com.owncloud.android.lib.common.utils.Log_OC
import io.scanbot.sdk.ScanbotSDK
import kotlin.math.roundToInt

object ScanBotSdkUtils {
    private val TAG = ScanBotSdkUtils::class.java.simpleName

    //license key will be valid for application id: com.t_systems.android.webdav & com.t_systems.android.webdav.beta
    //License validity till 25th March 2024
    const val LICENSE_KEY = "GzN70Lmov04uA+JL3wHMlhg+x+sy5Q" +
        "CLm9W96N5/8skCHz15FqQ877gGhi46" +
        "PLtmjDq+jIzfHoavmlF/q982ZazqB7" +
        "mKZY/6K2RsS4Tq5REDmPBPNlGZUDia" +
        "/7glnwvPOmamfA4DNwWPTSNi8Eh4xR" +
        "vIDnloBZsxCK+2tu2toahVXCt5Lvlc" +
        "C5BJ2CTcpdjlyr/8vkOy+Ao7ap7wmi" +
        "+xwMjnTl8H5wkcIjoAFzeo4v2IIoNG" +
        "4MZsmDXP7w93GbS6X7JGGYf9JA1gbC" +
        "TLngPkl4hvRGs+d6xcpIideMFhM4po" +
        "UqLxiGiLucNyZYpWWWn/5iUDJ/EmMb" +
        "hK8p9RHzCdmw==\nU2NhbmJvdFNESw" +
        "pjb20udF9zeXN0ZW1zLmFuZHJvaWQu" +
        "d2ViZGF2fGNvbS50X3N5c3RlbXMuYW" +
        "5kcm9pZC53ZWJkYXYuYmV0YQoxNzE0" +
        "MDg5NTk5CjExNTU2NzgKMg==\n"

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