package com.shahzaib.kharchatracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shahzaib.kharchatracker.R
import com.shahzaib.kharchatracker.data.AppDatabase
import com.shahzaib.kharchatracker.data.BalancePrefs
import com.shahzaib.kharchatracker.ui.formatRs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires a system notification the moment the tracked balance drops below
 * the user's low-balance threshold. Edge-triggered: it notifies once per
 * crossing (not on every transaction while already low), and re-arms when
 * the balance climbs back above the threshold.
 */
object BalanceAlert {

    private const val CHANNEL_ID = "low_balance_alerts"
    private const val NOTIFICATION_ID = 1001

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun checkAndNotify(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val prefs = BalancePrefs(appContext)
            if (!prefs.alertsEnabled()) return@launch
            val threshold = prefs.getThreshold()
            if (threshold <= 0.0) return@launch

            val (base, since) = prefs.getBase()
            val dao = AppDatabase.getInstance(appContext).transactionDao()
            val balance = base + dao.sumReceivedSince(since) - dao.sumSentSince(since)

            val wasBelow = prefs.wasBelowThreshold()
            when {
                balance < threshold && !wasBelow -> {
                    prefs.setWasBelowThreshold(true)
                    showNotification(appContext, balance, threshold)
                }
                balance >= threshold && wasBelow -> {
                    prefs.setWasBelowThreshold(false)
                }
            }
        }
    }

    private fun showNotification(context: Context, balance: Double, threshold: Double) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Balance alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Balance low hai!")
            .setContentText(
                "Sirf ${formatRs(balance)} reh gaya hai " +
                    "(alert ${formatRs(threshold)} par set hai)."
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            // On Android 13+ this needs POST_NOTIFICATIONS, which the app
            // requests on launch. If denied, the dashboard banner still warns.
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
