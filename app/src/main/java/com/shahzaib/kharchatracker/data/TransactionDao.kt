package com.shahzaib.kharchatracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'sent'")
    fun getTotalSent(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'received'")
    fun getTotalReceived(): Flow<Double>

    /**
     * Totals recorded *after* [since] (epoch millis) — used for the live
     * balance, which is anchored to the moment the user set their balance.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'sent' AND date >= :since")
    fun getTotalSentSince(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'received' AND date >= :since")
    fun getTotalReceivedSince(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'sent' AND date >= :since")
    suspend fun sumSentSince(since: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'received' AND date >= :since")
    suspend fun sumReceivedSince(since: Long): Double

    /**
     * Used for duplicate protection: counts transactions with the same amount
     * and title recorded after [since] (epoch millis).
     */
    @Query(
        "SELECT COUNT(*) FROM transactions " +
            "WHERE amount = :amount AND title = :title AND date > :since"
    )
    suspend fun countRecent(amount: Double, title: String, since: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
