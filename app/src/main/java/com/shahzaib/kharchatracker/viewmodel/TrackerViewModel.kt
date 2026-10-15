package com.shahzaib.kharchatracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahzaib.kharchatracker.data.AppDatabase
import com.shahzaib.kharchatracker.data.BalancePrefs
import com.shahzaib.kharchatracker.data.Transaction
import com.shahzaib.kharchatracker.notifications.BalanceAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state holder for the tracker screens. All data comes from the local
 * Room database; totals update automatically as new SMS/notifications arrive.
 *
 * The live balance is anchored to the manual balance the user sets:
 * base + received − sent for transactions recorded after it was set.
 */
class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).transactionDao()
    private val prefs = BalancePrefs(application)

    val transactions: StateFlow<List<Transaction>> =
        dao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalSent: StateFlow<Double> =
        dao.getTotalSent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val totalReceived: StateFlow<Double> =
        dao.getTotalReceived().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** Live balance = manual base + money in/out recorded after it was set. */
    val balance: StateFlow<Double> =
        prefs.balanceBase.flatMapLatest { (base, since) ->
            combine(
                dao.getTotalReceivedSince(since),
                dao.getTotalSentSince(since)
            ) { received, sent -> base + received - sent }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val lowThreshold: StateFlow<Double> = prefs.lowThreshold

    /** User typed their real current balance — it becomes the new anchor. */
    fun setManualBalance(amount: Double) {
        if (amount < 0.0) return
        prefs.setBalance(amount)
        BalanceAlert.checkAndNotify(getApplication())
    }

    fun setLowThreshold(amount: Double) {
        if (amount < 0.0) return
        prefs.setThreshold(amount)
        BalanceAlert.checkAndNotify(getApplication())
    }

    /** Adds a hand-entered transaction (cash payments, etc.). */
    fun addManual(amount: Double, type: String, title: String) {
        if (amount <= 0.0) return
        val safeType = if (type == Transaction.TYPE_RECEIVED) {
            Transaction.TYPE_RECEIVED
        } else {
            Transaction.TYPE_SENT
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(
                Transaction(
                    amount = amount,
                    type = safeType,
                    title = title.ifBlank { "Manual entry" },
                    bank = "Manual",
                    date = System.currentTimeMillis(),
                    smsBody = null
                )
            )
            BalanceAlert.checkAndNotify(getApplication())
        }
    }

    /** Clears the whole history (with confirmation in the UI). */
    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll()
        }
    }
}
