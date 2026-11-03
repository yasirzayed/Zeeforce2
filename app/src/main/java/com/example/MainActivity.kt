package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserRole
import com.example.data.repository.OrderBookingRepository
import com.example.ui.OrderBookingViewModel
import com.example.ui.screens.*
import com.example.ui.theme.ZayedSalesForceTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = OrderBookingRepository.getInstance(applicationContext)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }

            ZayedSalesForceTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SalesForceAppContent(
                        repository = repository,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { isDarkMode = it }
                    )
                }
            }
        }
    }
}

@Composable
fun SalesForceAppContent(
    repository: OrderBookingRepository,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val activeStaff by repository.activeStaff.collectAsState()
    val currentUserRole = activeStaff.role
    val currentUserEmail = activeStaff.email

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "admin_dashboard",
                    onClick = { navController.navigate("admin_dashboard") },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Admin") },
                    modifier = Modifier.testTag("nav_item_admin")
                )

                NavigationBarItem(
                    selected = currentRoute == "customer_account",
                    onClick = { navController.navigate("customer_account") },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Customer") },
                    label = { Text("Customer") },
                    modifier = Modifier.testTag("nav_item_customer")
                )

                NavigationBarItem(
                    selected = currentRoute == "commission_slips",
                    onClick = { navController.navigate("commission_slips") },
                    icon = { Icon(imageVector = Icons.Default.Receipt, contentDescription = "Commissions") },
                    label = { Text("Commissions") },
                    modifier = Modifier.testTag("nav_item_commissions")
                )

                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "admin_dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("admin_dashboard") {
                AdminDashboardScreen(
                    repository = repository,
                    onNavigateToCommissions = { navController.navigate("commission_slips") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToStaff = { navController.navigate("manage_staff") },
                    onNavigateToLogs = { navController.navigate("admin_logs") },
                    onNavigateToAnalytics = { navController.navigate("admin_analytics") },
                    onNavigateToInventory = { navController.navigate("inventory_catalog") },
                    onNavigateToCustomers = { navController.navigate("customer_directory") },
                    onNavigateToDispatch = { navController.navigate("admin_dispatch") },
                    onNavigateToSuppliers = { navController.navigate("supplier_performance") }
                )
            }

            composable("manage_staff") {
                ManageStaffScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("admin_logs") {
                val viewModel: OrderBookingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val activities by viewModel.allActivities.collectAsState()
                val staff by viewModel.allStaff.collectAsState()
                val activeStaff by viewModel.activeStaff.collectAsState()
                val auditLogs by viewModel.allAuditLogs.collectAsState()
                
                AdminDashboardModuleScreen(
                    currentStaff = activeStaff,
                    activities = activities,
                    staffList = staff,
                    auditLogs = auditLogs,
                    onVerifyPin = { pin, onSuccess, onError ->
                        viewModel.verifyAdminPinForDashboard(pin, onSuccess, onError)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("admin_analytics") {
                AdminSalesAnalyticsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("inventory_catalog") {
                InventoryCatalogScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("customer_directory") {
                CustomerDirectoryScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("admin_dispatch") {
                AdminDispatchScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("supplier_performance") {
                SupplierPerformanceScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("customer_account") {
                CustomerAccountScreen(
                    repository = repository,
                    customerEmail = currentUserEmail
                )
            }

            composable("commission_slips") {
                CommissionSlipsScreen(
                    repository = repository,
                    salesRepId = activeStaff.id,
                    salesRepName = activeStaff.name
                )
            }

            composable("settings") {
                AppSettingsScreen(
                    repository = repository,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }
    }
}
