/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.text

import android.content.Context
import android.text.format.DateUtils
import com.nextcloud.client.account.User
import com.nextcloud.utils.HumanReadableFormatter
import com.nextcloud.utils.date.DateFormatPattern
import com.owncloud.android.MainApp
import com.owncloud.android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

object DisplayTextFormatter {

    private const val DATE_TIME_SEPARATOR = ","
    private const val TIME_SEPARATOR = ':'
    private const val DATE_TIME_PARTS_SIZE = 2
    private const val ACCOUNT_HOST_SEPARATOR = '@'

    @JvmStatic
    fun formatAccountName(user: User): String {
        val host = user.accountName.substringAfterLast(ACCOUNT_HOST_SEPARATOR)
        val displayName = user.toOwnCloudAccount().displayName
        return "$displayName$ACCOUNT_HOST_SEPARATOR${LinkFormatter.toUnicodeDomain(host)}"
    }

    @JvmStatic
    @JvmOverloads
    fun formatRelativeTimestamp(context: Context, timestamp: Long, showFuture: Boolean = false): CharSequence =
        formatRelativeDateTime(context, timestamp, DateUtils.SECOND_IN_MILLIS, showFuture = showFuture)

    @JvmStatic
    @JvmOverloads
    fun formatRelativeDateTime(
        context: Context,
        time: Long,
        minResolution: Long,
        transitionResolution: Long = DateUtils.WEEK_IN_MILLIS,
        flags: Int = 0,
        showFuture: Boolean = false
    ): CharSequence {
        val elapsed = System.currentTimeMillis() - time
        return when {
            !showFuture && elapsed < 0 -> HumanReadableFormatter.formatDateTime(time)

            minResolution == DateUtils.MINUTE_IN_MILLIS && elapsed in 1 until DateUtils.MINUTE_IN_MILLIS ->
                context.getString(R.string.file_list_seconds_ago)

            else -> DateUtils.getRelativeDateTimeString(context, time, minResolution, transitionResolution, flags)
                .toString()
                .withoutTimeOfDay()
        }
    }

    @JvmStatic
    @JvmOverloads
    fun formatDate(timestamp: Long, pattern: DateFormatPattern, context: Context = MainApp.getAppContext()): String {
        val locale = context.resources.configuration.locales[0]
        return SimpleDateFormat(pattern.pattern, locale).format(Date(timestamp))
    }

    private fun String.withoutTimeOfDay(): String {
        val parts = split(DATE_TIME_SEPARATOR)
        if (parts.size != DATE_TIME_PARTS_SIZE || parts.any(String::isBlank)) {
            return this
        }

        val (first, second) = parts
        val firstHasTime = TIME_SEPARATOR in first
        val secondHasTime = TIME_SEPARATOR in second

        return when {
            secondHasTime && !firstHasTime -> first
            firstHasTime && !secondHasTime -> second
            else -> this
        }
    }

    // NMC function
    fun formatRelativeDateTime(context: Context, time: Long): String {
        var convTime: String?

        val nowTime = Date()

        val dateDiff = nowTime.time - time

        val second: Long = TimeUnit.MILLISECONDS.toSeconds(dateDiff)
        val minute: Long = TimeUnit.MILLISECONDS.toMinutes(dateDiff)
        val hour: Long = TimeUnit.MILLISECONDS.toHours(dateDiff)
        val day: Long = TimeUnit.MILLISECONDS.toDays(dateDiff)

        if (second == 0L) {
            convTime = context.resources.getString(R.string.just_now)
        } else if (second < 60) {
            convTime =
                context.resources.getQuantityString(R.plurals.seconds_ago, second.toInt(), second.toInt())
        } else if (minute < 60) {
            convTime =
                context.resources.getQuantityString(R.plurals.minutes_ago, minute.toInt(), minute.toInt())
        } else if (hour < 24) {
            convTime = context.resources.getQuantityString(R.plurals.hours_ago, hour.toInt(), hour.toInt())
        } else if (day >= 7) {
            if (day > 360) {
                val year = (day / 360)
                convTime =
                    context.resources.getQuantityString(R.plurals.years_ago, year.toInt(), year.toInt())
            } else if (day > 30) {
                val month = (day / 30)
                convTime =
                    context.resources.getQuantityString(R.plurals.months_ago, month.toInt(), month.toInt())
            } else {
                val week = (day / 7)
                convTime =
                    context.resources.getQuantityString(R.plurals.weeks_ago, week.toInt(), week.toInt())
            }
        } else {
            convTime = context.resources.getQuantityString(R.plurals.days_ago, day.toInt(), day.toInt())
        }

        return convTime
    }
}
