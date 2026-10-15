package com.shahzaib.kharchatracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shahzaib.kharchatracker.ui.AddTransactionScreen
import com.shahzaib.kharchatracker.ui.DashboardScreen
import com.shahzaib.kharchatracker.ui.theme.KharchaTrackerTheme
import com.shahzaib.kharchatracker.viewmodel.TrackerViewModel

/**
 * Entry point. Requests SMS permissions on launch, then hosts the
 * dashboard and add-transaction screens.
 *
 * Notification-listener access cannot be requested like a normal permission;
 * the dashboard shows a nudge card that opens the system settings screen.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: TrackerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Nothing to do here — the dashboard works with whatever was granted,
        // and the SMS rationale dialog explains why the permission matters.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()

        setContent {
            KharchaTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onAddClick = { navController.navigate("add") }
                            )
                        }
                        composable("add") {
                            AddTransactionScreen(
                                onSave = { amount, type, title ->
                                    viewModel.addManual(amount, type, title)
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.RECEIVE_SMS)
            needed.add(Manifest.permission.READ_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
