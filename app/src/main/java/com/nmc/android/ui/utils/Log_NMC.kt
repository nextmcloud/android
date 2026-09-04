package com.nmc.android.ui.utils

import com.owncloud.android.BuildConfig
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * NMC log interpreter class
 * this class will be used whenever we have to reduce the logs writing
 * this will avoid printing logs in release builds
 * todo: can be extended later for more functions
 */
object Log_NMC {
    @JvmStatic
    fun v(tag: String?, msg: String?) {
        if (BuildConfig.DEBUG) Log_OC.v(tag, msg)
    }

    @JvmStatic
    fun d(tag: String?, msg: String?) {
        if (BuildConfig.DEBUG) Log_OC.d(tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String, e: Exception) {
        if (BuildConfig.DEBUG) Log_OC.d(tag, msg, e)
    }

    @JvmStatic
    fun i(tag: String?, msg: String?) {
        if (BuildConfig.DEBUG) Log_OC.i(tag, msg)
    }

    @JvmStatic
    fun e(tag: String?, msg: String?) {
        if (BuildConfig.DEBUG) Log_OC.e(tag, msg)
    }

    @JvmStatic
    fun w(tag: String?, msg: String?) {
        if (BuildConfig.DEBUG) Log_OC.w(tag, msg)
    }
}
