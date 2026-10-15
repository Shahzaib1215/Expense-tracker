package com.shahzaib.kharchatracker.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.shahzaib.kharchatracker.data.AppDatabase
import com.shahzaib.kharchatracker.data.Transaction
import com.shahzaib.kharchatracker.parser.TransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for transaction alerts posted as notifications by banking apps,
 * parses them with [TransactionParser] and stores them locally.
 *
 * The user must enable notification access for this app once, in system
 * Settings > Notifications > Notification access. The dashboard guides them.
 */
class TxnNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName !in BANK_PACKAGES) return
        if (notification.isOngoing) return

        val extras = notification.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString().orEmpty()
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(separator = " ") { it.toString() }.orEmpty()
        val combined = listOf(title, text, bigText, subText, infoText, textLines)
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
        if (combined.isBlank()) return

        val parsed = TransactionParser.parse(combined, notification.packageName) ?: return

        scope.launch {
            val dao = AppDatabase.getInstance(applicationContext).transactionDao()
            val now = System.currentTimeMillis()

            // Skip duplicates: same amount + title within the last 60 seconds
            // (apps often update the same notification, which re-triggers this).
            val recentCutoff = now - 60 * 1000L
            if (dao.countRecent(parsed.amount, parsed.title, recentCutoff) > 0) return@launch

            dao.insert(
                Transaction(
                    amount = parsed.amount,
                    type = parsed.type,
                    title = parsed.title,
                    bank = parsed.bank,
                    date = now,
                    smsBody = combined
                )
            )

            BalanceAlert.checkAndNotify(applicationContext)
        }
    }

    companion object {
        /** Known Pakistani banking / wallet app packages to watch. */
        val BANK_PACKAGES: Set<String> = setOf(
            "com.meezanbank.meezanmobileapp", // Meezan Bank
            "pk.com.telenor.phoenix", // Easypaisa
            "com.techlogix.mobilinkcustomer", // JazzCash
            "com.ubl.ubldigital", // UBL Digital
            "com.hbl.hblmobilebanking", // HBL Mobile
            "com.mcb.mcbmobile", // MCB Mobile
            "com.alfalah.mobile", // Bank Alfalah Alfa
            "com.faysalbank.mobile", // Faysal Bank
            "com.mashreq.mashreqmobile", // Mashreq
            "com.nayapay.portal", // NayaPay
            "com.sadapay", // SadaPay
            "com.abacus.android", // myABL (Allied Bank)
            "com.askaribank.mobile", // Askari Bank
            "com.bankislami.mobile", // BankIslami
            "com.mcb.live", // MCB Live
            "com.jsbank.mobile" // JS Bank
        )
    }
}

/**
 * Returns true if the user has granted this app notification-listener access.
 */
fun isNotificationListenerEnabled(context: Context): Boolean {
    val component = ComponentName(context, TxnNotificationListener::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return enabled.split(":").any { it.equals(component.flattenToString(), ignoreCase = true) }
}
