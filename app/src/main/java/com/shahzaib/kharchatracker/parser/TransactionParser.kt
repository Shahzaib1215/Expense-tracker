package com.shahzaib.kharchatracker.parser

import com.shahzaib.kharchatracker.data.Transaction

/**
 * Result of parsing a bank SMS or banking-app notification.
 */
data class ParsedTransaction(
    val amount: Double,
    /** Transaction.TYPE_SENT or Transaction.TYPE_RECEIVED */
    val type: String,
    val title: String,
    val bank: String
)

/**
 * Parses Pakistani bank transaction alerts.
 *
 * Handles amounts like "Rs. 5,000", "Rs.5000", "PKR 12,500.00"
 * (commas, decimals, any casing) and classifies direction from keywords:
 * debit/debited/sent/transferred/paid/withdrawn -> sent,
 * credit/credited/received/deposited -> received.
 *
 * Returns null when no amount can be found or the direction is unclear.
 */
object TransactionParser {

    // "Rs. 5,000" | "Rs.5000" | "PKR 12,500.00" | "rs 250"
    private val amountRegex = Regex(
        """(?:Rs\.?|PKR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val sentKeywords = listOf(
        "debited", "debit", "sent", "transferred", "transfer",
        "paid", "payment", "withdrawn", "withdrawal", "purchase", "spent"
    )

    private val receivedKeywords = listOf(
        "credited", "credit", "received", "deposited", "deposit", "refund"
    )

    // keyword (lowercase) -> display name. Checked against "sender + body".
    private val bankKeywords = listOf(
        "meezan" to "Meezan",
        "ubl" to "UBL",
        "hbl" to "HBL",
        "mcb" to "MCB",
        "allied" to "Allied Bank",
        "alfalah" to "Bank Alfalah",
        "faysal" to "Faysal Bank",
        "mashreq" to "Mashreq",
        "easypaisa" to "Easypaisa",
        "telenor" to "Easypaisa",
        "jazzcash" to "JazzCash",
        "mobilink" to "JazzCash",
        "nayapay" to "NayaPay",
        "naya pay" to "NayaPay",
        "sadapay" to "SadaPay",
        "sada pay" to "SadaPay"
    )

    fun parse(smsBody: String, sender: String): ParsedTransaction? {
        val body = smsBody.trim()
        if (body.isEmpty()) return null

        val amount = extractAmount(body) ?: return null
        val type = detectType(body) ?: return null
        val bank = detectBank(sender, body)
        val title = makeTitle(body, bank)

        return ParsedTransaction(
            amount = amount,
            type = type,
            title = title,
            bank = bank
        )
    }

    /**
     * Finds the first Rs./PKR amount in the text. Handles thousands separators
     * ("12,500"), decimals ("500.00") and missing spaces ("Rs.5000").
     */
    fun extractAmount(text: String): Double? {
        val match = amountRegex.find(text) ?: return null
        val cleaned = match.groupValues[1].replace(",", "")
        val amount = cleaned.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null
        return amount
    }

    /**
     * Classifies the transaction direction. When both sent and received
     * keywords appear, the one mentioned first wins.
     */
    fun detectType(text: String): String? {
        val lower = text.lowercase()

        val firstSent = sentKeywords
            .mapNotNull { keyword -> lower.indexOf(keyword).takeIf { it >= 0 } }
            .minOrNull()
        val firstReceived = receivedKeywords
            .mapNotNull { keyword -> lower.indexOf(keyword).takeIf { it >= 0 } }
            .minOrNull()

        return when {
            firstSent == null && firstReceived == null -> null
            firstReceived == null -> Transaction.TYPE_SENT
            firstSent == null -> Transaction.TYPE_RECEIVED
            firstSent <= firstReceived -> Transaction.TYPE_SENT
            else -> Transaction.TYPE_RECEIVED
        }
    }

    fun detectBank(sender: String, body: String): String {
        val haystack = "$sender $body".lowercase()
        for ((keyword, bank) in bankKeywords) {
            if (haystack.contains(keyword)) return bank
        }
        return "SMS"
    }

    private fun makeTitle(body: String, bank: String): String {
        val firstLine = body.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
            .replace(Regex("\\s+"), " ")
        if (firstLine.isEmpty()) return "$bank transaction"
        return if (firstLine.length <= 70) firstLine else firstLine.take(67) + "..."
    }
}
