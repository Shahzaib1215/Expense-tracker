package com.shahzaib.kharchatracker.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shahzaib.kharchatracker.data.Transaction
import com.shahzaib.kharchatracker.notifications.isNotificationListenerEnabled
import com.shahzaib.kharchatracker.ui.theme.Emerald40
import com.shahzaib.kharchatracker.ui.theme.EmeraldDeep
import com.shahzaib.kharchatracker.ui.theme.ExpenseRed
import com.shahzaib.kharchatracker.ui.theme.ExpenseRedBg
import com.shahzaib.kharchatracker.ui.theme.Gold40
import com.shahzaib.kharchatracker.ui.theme.IncomeGreen
import com.shahzaib.kharchatracker.ui.theme.IncomeGreenBg
import com.shahzaib.kharchatracker.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TrackerViewModel,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val totalSent by viewModel.totalSent.collectAsStateWithLifecycle()
    val totalReceived by viewModel.totalReceived.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var listenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var showSmsInfo by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    val lowThreshold by viewModel.lowThreshold.collectAsStateWithLifecycle()

    // Re-check notification access whenever the screen comes back to the front.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add", fontWeight = FontWeight.Bold) },
                containerColor = Emerald40,
                contentColor = Color.White
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppHeader(onClearClick = { showDeleteConfirm = true })
            }

            item {
                BalanceHeroCard(
                    balance = balance,
                    received = totalReceived,
                    sent = totalSent,
                    onClick = { showBalanceDialog = true }
                )
            }

            // Low-balance warning — tap to fix the balance / threshold.
            if (lowThreshold > 0.0 && balance < lowThreshold) {
                item {
                    LowBalanceBanner(
                        balance = balance,
                        threshold = lowThreshold,
                        onClick = { showBalanceDialog = true }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Aamdani",
                        amount = totalReceived,
                        icon = Icons.Filled.KeyboardArrowUp,
                        tint = IncomeGreen,
                        tintBg = IncomeGreenBg,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Kharcha",
                        amount = totalSent,
                        icon = Icons.Filled.KeyboardArrowDown,
                        tint = ExpenseRed,
                        tintBg = ExpenseRedBg,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!listenerEnabled) {
                item {
                    AutoDetectNudgeCard(
                        onEnableClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Recent Transactions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { showSmsInfo = true }) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Why SMS access?")
                    }
                }
            }

            if (transactions.isEmpty()) {
                item { EmptyState() }
            } else {
                items(transactions, key = { it.id }) { txn ->
                    TransactionCard(txn)
                }
            }
        }
    }

    if (showBalanceDialog) {
        BalanceDialog(
            currentBalance = balance,
            currentThreshold = lowThreshold,
            onDismiss = { showBalanceDialog = false },
            onSave = { newBalance, newThreshold ->
                viewModel.setManualBalance(newBalance)
                viewModel.setLowThreshold(newThreshold)
                showBalanceDialog = false
            }
        )
    }

    if (showSmsInfo) {
        AlertDialog(
            onDismissRequest = { showSmsInfo = false },
            confirmButton = {
                TextButton(onClick = { showSmsInfo = false }) { Text("Got it") }
            },
            title = { Text("Why does the app read SMS?") },
            text = {
                Text(
                    "Pakistani banks send a text message for almost every " +
                        "transaction. Kharcha Tracker reads only those messages, " +
                        "extracts the amount and whether money was sent or " +
                        "received, and saves it on your phone. Nothing is ever " +
                        "uploaded anywhere — everything stays on this device."
                )
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAll()
                        showDeleteConfirm = false
                    }
                ) { Text("Delete all", color = ExpenseRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            title = { Text("Clear history?") },
            text = { Text("This will permanently delete all recorded transactions.") }
        )
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun AppHeader(onClearClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Kharcha Tracker",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Apne kharchay par nazar rakho",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClearClick) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Clear all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hero balance card — emerald gradient
// ---------------------------------------------------------------------------

@Composable
private fun BalanceHeroCard(
    balance: Double,
    received: Double,
    sent: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(EmeraldDeep, Emerald40)
                    )
                )
        ) {
            // Decorative translucent circles for a premium feel.
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .offset(x = 110.dp, y = (-55).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .align(Alignment.TopEnd)
            )
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .offset(x = (-35).dp, y = 70.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .align(Alignment.CenterStart)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Total Balance",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        "Tap to set",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    formatRs(balance),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroMiniStat(
                        label = "Money In",
                        amount = received,
                        icon = Icons.Filled.KeyboardArrowUp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                    HeroMiniStat(
                        label = "Money Out",
                        amount = sent,
                        icon = Icons.Filled.KeyboardArrowDown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMiniStat(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                formatRs(amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Small stat cards
// ---------------------------------------------------------------------------

@Composable
private fun StatCard(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    tintBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tintBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatRs(amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Low-balance warning banner
// ---------------------------------------------------------------------------

@Composable
private fun LowBalanceBanner(
    balance: Double,
    threshold: Double,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ExpenseRedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ExpenseRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Balance khatam hone wala hai!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ExpenseRed
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Sirf ${formatRs(balance)} reh gaya hai " +
                        "(alert ${formatRs(threshold)} par set hai). " +
                        "Tap karke balance update karo.",
                    fontSize = 12.sp,
                    color = ExpenseRed.copy(alpha = 0.85f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Auto-detect nudge
// ---------------------------------------------------------------------------

@Composable
private fun AutoDetectNudgeCard(onEnableClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Gold40.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = Gold40)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Auto-detect is off",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Enable notification access so banking alerts are recorded automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
        OutlinedButton(
            onClick = onEnableClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text("Enable notification access")
        }
    }
}

// ---------------------------------------------------------------------------
// Transaction rows
// ---------------------------------------------------------------------------

@Composable
private fun TransactionCard(txn: Transaction) {
    val isSent = txn.type == Transaction.TYPE_SENT
    val tint = if (isSent) ExpenseRed else IncomeGreen
    val tintBg = if (isSent) ExpenseRedBg else IncomeGreenBg

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tintBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSent) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    contentDescription = txn.type,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    txn.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${txn.bank}  •  ${formatDate(txn.date)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (isSent) "- " else "+ ") + formatRs(txn.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = tint,
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No transactions yet",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Bank SMS and app notifications will appear here automatically — or tap + to add one manually.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------

fun formatRs(amount: Double): String {
    val number = if (amount % 1.0 == 0.0) {
        "%,.0f".format(Locale.US, amount)
    } else {
        "%,.2f".format(Locale.US, amount)
    }
    return "Rs. $number"
}

private fun formatDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
