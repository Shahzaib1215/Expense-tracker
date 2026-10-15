package com.shahzaib.kharchatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single money movement detected from SMS, a banking app notification,
 * or entered manually by the user.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Amount in PKR, always stored as a positive number. */
    val amount: Double,

    /** "sent" (money went out) or "received" (money came in). */
    val type: String,

    /** Short human-readable description of the transaction. */
    val title: String,

    /** Where it came from: "Meezan", "Easypaisa", "Manual", "SMS", ... */
    val bank: String,

    /** Epoch milliseconds when the transaction happened / was recorded. */
    val date: Long,

    /** Original SMS/notification text, if this came from one. Null for manual entries. */
    val smsBody: String? = null
) {
    companion object {
        const val TYPE_SENT = "sent"
        const val TYPE_RECEIVED = "received"
    }
}
