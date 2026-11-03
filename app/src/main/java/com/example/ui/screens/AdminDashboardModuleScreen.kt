package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StaffActivityLog
import com.example.data.model.StaffMember
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardModuleScreen(
    currentStaff: StaffMember,
    activities: List<StaffActivityLog>,
    staffList: List<StaffMember>,
    auditLogs: List<com.example.data.model.AdminAuditLog> = emptyList(),
    onVerifyPin: (pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPinVerified by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var selectedFilterType by remember { mutableStateOf("ALL") }

    val imperialGold = Color(0xFFD4AF37)
    val deepNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val borderNavy = Color(0xFF334155)

    androidx.activity.compose.BackHandler { onBack() }

    if (!isPinVerified) {
        // Dedicated Security Screen prompting for 5-digit Admin PIN (22026)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(deepNavy)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_pin_security_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                border = BorderStroke(1.5.dp, imperialGold)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(deepNavy)
                            .border(2.dp, imperialGold, CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.zayed_logo),
                            contentDescription = "Zayed Corporate Emblem",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Admin Security Verification",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enter your Executive Admin PIN to access Audit Trail & Activity Logs",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8) pinInput = it },
                        label = { Text("Admin PIN") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_dashboard_pin_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = imperialGold,
                            unfocusedBorderColor = borderNavy,
                            focusedLabelColor = imperialGold,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pinError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            isVerifying = true
                            pinError = null
                            onVerifyPin(
                                pinInput,
                                {
                                    isVerifying = false
                                    isPinVerified = true
                                },
                                { err ->
                                    isVerifying = false
                                    pinError = err
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_dashboard_pin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = imperialGold)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = deepNavy)
                        } else {
                            Text(
                                text = "Verify & Open Admin Module",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = deepNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pin_cancel_button")
                    ) {
                        Text(text = "Return to Main", color = Color.Gray)
                    }
                }
            }
        }
    } else {
        // Secure Admin Dashboard Module & Activity Logs Screen
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = deepNavy,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Secure Admin Dashboard Module",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Staff Activity Logs & Account Audit Trail",
                                fontSize = 11.sp,
                                color = imperialGold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("admin_dashboard_back")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = imperialGold)
                        }
                    },
                    actions = {
                        Image(
                            painter = painterResource(id = R.drawable.zayed_logo),
                            contentDescription = "Zayed Corporate Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = cardNavy)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                var selectedMainTab by remember { mutableStateOf(0) }

                TabRow(
                    selectedTabIndex = selectedMainTab,
                    containerColor = cardNavy,
                    contentColor = imperialGold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedMainTab == 0,
                        onClick = { selectedMainTab = 0 },
                        text = { Text("Operations Logs (${activities.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedMainTab == 1,
                        onClick = { selectedMainTab = 1 },
                        text = { Text("Immutable Audit Trail (${auditLogs.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                if (selectedMainTab == 0) {
                    // Filter Row
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterType == "ALL",
                        onClick = { selectedFilterType = "ALL" },
                        label = { Text("All Activities") }
                    )
                    FilterChip(
                        selected = selectedFilterType == "LOGIN",
                        onClick = { selectedFilterType = "LOGIN" },
                        label = { Text("Logins") }
                    )
                    FilterChip(
                        selected = selectedFilterType == "ACCOUNT_MODIFICATION",
                        onClick = { selectedFilterType = "ACCOUNT_MODIFICATION" },
                        label = { Text("Accounts") }
                    )
                    FilterChip(
                        selected = selectedFilterType == "ORDER_UPDATE",
                        onClick = { selectedFilterType = "ORDER_UPDATE" },
                        label = { Text("Orders") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredActivities = when (selectedFilterType) {
                    "LOGIN" -> activities.filter { it.activityType == "LOGIN" }
                    "ACCOUNT_MODIFICATION" -> activities.filter { it.activityType == "ACCOUNT_MODIFICATION" }
                    "ORDER_UPDATE" -> activities.filter { it.activityType == "ORDER_UPDATE" }
                    else -> activities
                }

                if (filteredActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No staff activity logs recorded yet.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredActivities) { log ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("activity_log_item_${log.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardNavy),
                                border = BorderStroke(1.dp, borderNavy)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(deepNavy)
                                            .border(
                                                1.dp,
                                                when (log.activityType) {
                                                    "LOGIN" -> Color(0xFF34D399)
                                                    "ACCOUNT_MODIFICATION" -> Color(0xFF60A5FA)
                                                    else -> imperialGold
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (log.activityType) {
                                                "LOGIN" -> Icons.Default.Login
                                                "ACCOUNT_MODIFICATION" -> Icons.Default.ManageAccounts
                                                else -> Icons.Default.ShoppingBag
                                            },
                                            contentDescription = null,
                                            tint = when (log.activityType) {
                                                "LOGIN" -> Color(0xFF34D399)
                                                "ACCOUNT_MODIFICATION" -> Color(0xFF60A5FA)
                                                else -> imperialGold
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = log.staffName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (log.syncStatus == "SYNCED") Color(0xFF34D399).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = log.syncStatus,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (log.syncStatus == "SYNCED") Color(0xFF34D399) else Color(0xFFF59E0B),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = log.description,
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
                                        Text(
                                            text = "$dateStr • ID: ${log.staffId}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Immutable Compliance Audit Trail (Supabase)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = cardNavy,
                            border = BorderStroke(1.dp, imperialGold.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield",
                                    tint = imperialGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Tamper-Evident Immutable Audit Log",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = imperialGold
                                    )
                                    Text(
                                        text = "Every pricing, stock, customer, permission, and cloud configuration change is signed with SHA-256 for non-repudiation.",
                                        fontSize = 10.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        if (auditLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = imperialGold, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No sensitive modifications recorded yet.", color = Color.Gray, fontSize = 14.sp)
                                    Text("Changes to prices, stock, PINs, and cloud sync will appear here.", color = Color.DarkGray, fontSize = 11.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(auditLogs) { log ->
                                    val isValid = log.isSignatureValid()
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("audit_log_item_${log.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardNavy),
                                        border = BorderStroke(1.dp, if (isValid) borderNavy else Color(0xFFEF4444))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = imperialGold.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = log.action.replace("_", " "),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = imperialGold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isValid) Color(0xFF34D399).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = if (isValid) "✓ SHA-256 Valid" else "⚠ TAMPERED",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isValid) Color(0xFF34D399) else Color(0xFFEF4444),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = "${log.entityType} [${log.fieldName}]: ${log.oldValue} ➔ ${log.newValue}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = "Reason: ${log.reason}",
                                                fontSize = 11.sp,
                                                color = Color.LightGray
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
                                            Text(
                                                text = "$dateStr • Actor: ${log.actorName} (${log.actorEmail}) • Tenant: ${log.tenantId}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
