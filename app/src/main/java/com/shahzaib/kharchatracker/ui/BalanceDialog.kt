package com.shahzaib.kharchatracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahzaib.kharchatracker.ui.theme.Emerald40
import java.util.Locale

/**
 * Lets the user type their real current balance (it becomes the anchor —
 * every new transaction adjusts it automatically) and the low-balance
 * alert threshold.
 */
@Composable
fun BalanceDialog(
    currentBalance: Double,
    currentThreshold: Double,
    onDismiss: () -> Unit,
    onSave: (balance: Double, threshold: Double) -> Unit
) {
    var balanceText by remember {
        mutableStateOf(if (currentBalance == 0.0) "" else trimAmount(currentBalance))
    }
    var thresholdText by remember { mutableStateOf(trimAmount(currentThreshold)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Apna balance set karo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Apna maujooda balance likho — is ke baad har aane/jaane " +
                        "wali raqam is me khud jama-tafreeq hogi.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { input ->
                        if (input.matches(Regex("[0-9,.]*"))) balanceText = input
                    },
                    label = { Text("Current balance (Rs.)") },
                    placeholder = { Text("e.g. 25,000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { input ->
                        if (input.matches(Regex("[0-9,.]*"))) thresholdText = input
                    },
                    label = { Text("Low balance alert below (Rs.)") },
                    placeholder = { Text("e.g. 1,000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Balance is se neeche gaya to app foran khabardar karegi.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = balanceText.replace(",", "").toDoubleOrNull() ?: currentBalance
                    val t = thresholdText.replace(",", "").toDoubleOrNull() ?: currentThreshold
                    onSave(b, t)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald40),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun trimAmount(amount: Double): String =
    if (amount % 1.0 == 0.0) {
        "%,.0f".format(Locale.US, amount)
    } else {
        amount.toString()
    }
