package com.shahzaib.kharchatracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small settings store (SharedPreferences) behind the manual-balance feature.
 *
 * The live balance shown in the app is:
 *     base balance + money received − money sent
 * for transactions recorded *after* the base was set. So the user can type
 * their real wallet/bank balance once, and every new SMS / notification /
 * manual entry adjusts it automatically.
 *
 * Also holds the low-balance alert threshold and the edge-trigger flag so
 * the "balance khatam hone wala hai" notification fires once per crossing.
 */
class BalancePrefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _balanceBase = MutableStateFlow(loadBase())
    val balanceBase: StateFlow<Pair<Double, Long>> = _balanceBase.asStateFlow()

    private val _lowThreshold = MutableStateFlow(loadThreshold())
    val lowThreshold: StateFlow<Double> = _lowThreshold.asStateFlow()

    fun getBase(): Pair<Double, Long> = _balanceBase.value
    fun getThreshold(): Double = _lowThreshold.value
    fun alertsEnabled(): Boolean = prefs.getBoolean(KEY_ALERTS, true)

    /** User typed their current real balance — it becomes the new base. */
    fun setBalance(amount: Double) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putFloat(KEY_BASE, amount.toFloat())
            .putLong(KEY_SINCE, now)
            .putBoolean(KEY_WAS_BELOW, false)
            .apply()
        _balanceBase.value = amount to now
    }

    fun setThreshold(amount: Double) {
        prefs.edit().putFloat(KEY_THRESHOLD, amount.toFloat()).apply()
        _lowThreshold.value = amount
    }

    fun wasBelowThreshold(): Boolean = prefs.getBoolean(KEY_WAS_BELOW, false)

    fun setWasBelowThreshold(below: Boolean) {
        prefs.edit().putBoolean(KEY_WAS_BELOW, below).apply()
    }

    private fun loadBase(): Pair<Double, Long> =
        prefs.getFloat(KEY_BASE, 0f).toDouble() to prefs.getLong(KEY_SINCE, 0L)

    private fun loadThreshold(): Double =
        prefs.getFloat(KEY_THRESHOLD, DEFAULT_THRESHOLD.toFloat()).toDouble()

    companion object {
        private const val PREFS_NAME = "kharcha_prefs"
        private const val KEY_BASE = "base_balance"
        private const val KEY_SINCE = "balance_since"
        private const val KEY_THRESHOLD = "low_threshold"
        private const val KEY_WAS_BELOW = "was_below"
        private const val KEY_ALERTS = "alerts_enabled"
        const val DEFAULT_THRESHOLD = 1000.0
    }
}
