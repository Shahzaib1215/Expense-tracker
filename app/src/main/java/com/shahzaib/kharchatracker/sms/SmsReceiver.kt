package com.shahzaib.kharchatracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.shahzaib.kharchatracker.data.AppDatabase
import com.shahzaib.kharchatracker.data.Transaction
import com.shahzaib.kharchatracker.notifications.BalanceAlert
import com.shahzaib.kharchatracker.parser.TransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives incoming SMS, runs them through [TransactionParser] and stores
 * recognised bank transactions in the local database.
 *
 * Duplicate protection: skips a message if the same amount + title was
 * already recorded in the last 5 minutes (multi-part SMS can arrive twice).
 */
class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val bundle = intent.extras ?: return
        @Suppress("UNCHECKED_CAST")
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")
        val appContext = context.applicationContext

        scope.launch {
            val dao = AppDatabase.getInstance(appContext).transactionDao()
            val now = System.currentTimeMillis()
            var insertedAny = false

            for (pdu in pdus) {
                val bytes = pdu as? ByteArray ?: continue
                val message = SmsMessage.createFromPdu(bytes, format)
                val body = message.messageBody ?: continue
                val sender = message.displayOriginatingAddress ?: ""

                val parsed = TransactionParser.parse(body, sender) ?: continue

                // Skip duplicates from re-delivered / multi-part messages.
                val recentCutoff = now - 5 * 60 * 1000L
                if (dao.countRecent(parsed.amount, parsed.title, recentCutoff) > 0) continue

                dao.insert(
                    Transaction(
                        amount = parsed.amount,
                        type = parsed.type,
                        title = parsed.title,
                        bank = parsed.bank,
                        date = now,
                        smsBody = body
                    )
                )
                insertedAny = true
            }

            if (insertedAny) {
                BalanceAlert.checkAndNotify(appContext)
            }
        }
    }
}
