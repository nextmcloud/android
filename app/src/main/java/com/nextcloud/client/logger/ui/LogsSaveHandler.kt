/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger.ui

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.app.NotificationCompat
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Cancellable
import com.nextcloud.client.core.Clock
import com.nextcloud.client.logger.LogEntry
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileExportUtils
import java.io.File
import java.io.FileWriter
import java.security.SecureRandom
import java.util.TimeZone

// NMC-4888 task
class LogsSaveHandler(private val context: Context, private val clock: Clock, private val runner: AsyncRunner) {

    private companion object {
        private const val LOGS_MIME_TYPE = "text/plain"
        private const val LOGS_DATE_FORMAT = "yyyyMMdd_HHmmssZ"
        private val notificationId = SecureRandom().nextInt()
    }

    private class Task(
        private val logs: List<LogEntry>,
        private val file: File,
        private val tz: TimeZone
    ) : Function0<File> {

        override fun invoke(): File {
            file.parentFile?.mkdirs()
            val fo = FileWriter(file, false)
            logs.forEach {
                fo.write(it.toString(tz))
                fo.write("\n")
            }
            fo.close()
            return file
        }
    }

    private var task: Cancellable? = null

    fun save(logs: List<LogEntry>) {
        if (task == null) {
            val timestamp = DisplayUtils.getDateByPattern(System.currentTimeMillis(), context, LOGS_DATE_FORMAT)
            val logFileName = "logs_${context.resources.getString(R.string.app_name)}_${timestamp}.txt"
            val outFile = File(context.cacheDir, logFileName)
            task = runner.postQuickTask(Task(logs, outFile, clock.tz), onResult = {
                task = null
                export(it)
            })
        }
    }

    fun stop() {
        if (task != null) {
            task?.cancel()
            task = null
        }
    }

    private fun export(file: File) {
        task = null
        try {
            FileExportUtils().exportFile(
                file.name,
                LOGS_MIME_TYPE,
                context.contentResolver,
                null,
                file
            )
            showSuccessNotification()
        } catch (e: IllegalStateException) {
            Log_OC.e("LogsSaveHandler", "Error saving logs to file", e)
            showErrorNotification()
        }
    }

    private fun showErrorNotification() {
        showNotification(false, context.resources.getString(R.string.logs_export_failed))
    }

    private fun showSuccessNotification() {
        showNotification(true, context.resources.getString(R.string.logs_export_success))
    }

    private fun showNotification(isSuccess: Boolean, message: String) {
        val notificationBuilder = NotificationCompat.Builder(
            context,
            NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
        )
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(message)
            .setAutoCancel(true)

        // NMC Customization
        notificationBuilder.color = context.resources.getColor(R.color.primary, null)

        if (isSuccess) {
            val actionIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
            val actionPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                actionIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                NotificationCompat.Action(
                    null,
                    context.getString(R.string.locate_folder),
                    actionPendingIntent
                )
            )
        }

        val notificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
