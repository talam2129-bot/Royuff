package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SavedBiller
import com.example.data.TransactionHistory
import java.text.SimpleDateFormat
import java.util.*

enum class ActiveSheet {
    NONE, SEND_MONEY, BANK_TRANSFER, MOBILE_RECHARGE, UTILITY_BILL, LOAD_MONEY, PROFILE_SETTINGS
}

@Composable
fun DashboardScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val billers by viewModel.savedBillers.collectAsStateWithLifecycle()
    val paymentStatus by viewModel.paymentStatus.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }
    var selectedUtilityType by remember { mutableStateOf("ELECTRICITY") } // Or "WATER", "INTERNET"
    
    // Balance eyeball visibility
    var balanceVisible by remember { mutableStateOf(true) }

    if (!isUnlocked) {
        NeoLockScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NeoBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    "HOME" -> {
                        HomeContent(
                            userProfile = userProfile,
                            transactions = transactions,
                            billers = billers,
                            balanceVisible = balanceVisible,
                            onBalanceToggle = { balanceVisible = !balanceVisible },
                            onActionClicked = { activeSheet = it }
                        )
                    }
                    "SCAN_PAY" -> {
                        ScanPayContent(
                            viewModel = viewModel,
                            paymentStatus = paymentStatus,
                            onResetStatus = { viewModel.resetPaymentStatus() }
                        )
                    }
                    "BUDGET" -> {
                        BudgetContent(viewModel = viewModel)
                    }
                    "HISTORY" -> {
                        HistoryContent(
                            transactions = transactions
                        )
                    }
                    "OFFERS" -> {
                        OffersContent()
                    }
                }

                // Centralized Modal Sheets implementation
                if (activeSheet != ActiveSheet.NONE) {
                    NeoSheetDialog(
                        activeSheet = activeSheet,
                        utilityType = selectedUtilityType,
                        viewModel = viewModel,
                        paymentStatus = paymentStatus,
                        onClose = {
                            activeSheet = ActiveSheet.NONE
                            viewModel.resetPaymentStatus()
                        },
                        onSelectUtilityType = { selectedUtilityType = it }
                    )
                }
            }
        }
    }
}

@Composable
fun NeoBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == "HOME",
            onClick = { onTabSelected("HOME") },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_home_tab")
        )
        NavigationBarItem(
            selected = currentTab == "SCAN_PAY",
            onClick = { onTabSelected("SCAN_PAY") },
            icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan & Pay") },
            label = { Text("Scan Qr", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_scan_tab")
        )
        NavigationBarItem(
            selected = currentTab == "BUDGET",
            onClick = { onTabSelected("BUDGET") },
            icon = { Icon(Icons.Filled.PieChart, contentDescription = "Budget Dashboard") },
            label = { Text("Budget", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_budget_tab")
        )
        NavigationBarItem(
            selected = currentTab == "HISTORY",
            onClick = { onTabSelected("HISTORY") },
            icon = { Icon(Icons.Filled.History, contentDescription = "History") },
            label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_history_tab")
        )
        NavigationBarItem(
            selected = currentTab == "OFFERS",
            onClick = { onTabSelected("OFFERS") },
            icon = { Icon(Icons.Filled.LocalActivity, contentDescription = "Offers") },
            label = { Text("Offers", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_offers_tab")
        )
    }
}

@Composable
fun HomeContent(
    userProfile: com.example.data.UserProfile?,
    transactions: List<TransactionHistory>,
    billers: List<SavedBiller>,
    balanceVisible: Boolean,
    onBalanceToggle: () -> Unit,
    onActionClicked: (ActiveSheet) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. Customized Nepal Design Header
        item {
            HeaderSection(userProfile = userProfile, onProfileClick = { onActionClicked(ActiveSheet.PROFILE_SETTINGS) })
        }

        // 2. Neon-Glaze Balance Card Component
        item {
            BalanceCard(
                userProfile = userProfile,
                balanceVisible = balanceVisible,
                onBalanceToggle = onBalanceToggle,
                onActionClicked = onActionClicked
            )
        }

        // 3. Dynamic Quick Payments Services Grid
        item {
            QuickServicesGrid(onActionClicked = onActionClicked)
        }

        // 4. Promo Slider Header
        item {
            SectionTitle(title = "FEATURED CASHBACK OFFERS")
            NepalOffersSlider()
        }

        // 5. Recent Transaction Feed
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT TRANSACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found", color = Color.Gray)
                }
            }
        } else {
            items(transactions.take(5)) { tx ->
                TransactionRow(tx = tx)
            }
        }
    }
}

@Composable
fun HeaderSection(userProfile: com.example.data.UserProfile?, onProfileClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 28.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Editorial rounded monogram container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    letterSpacing = (-1).sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "NeoPay",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f)
            )

            // Dynamic safe notification & profile triggers
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Alerts",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onProfileClick() }
                        .testTag("action_profile_settings"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceCard(
    userProfile: com.example.data.UserProfile?,
    balanceVisible: Boolean,
    onBalanceToggle: () -> Unit,
    onActionClicked: (ActiveSheet) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Upper balance tracking text
        Text(
            text = "AVAILABLE BALANCE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Large Editorial Typographic Balance Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBalanceToggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NPR",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (balanceVisible) String.format("%,.2f", userProfile?.balanceNpr ?: 0.0) else "••••••••",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("wallet_balance_text")
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = if (balanceVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = "Toggle Balance",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Premium Editorial "+ Add Money" button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onActionClicked(ActiveSheet.LOAD_MONEY) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD3E4FF),
                    contentColor = Color(0xFF001C38)
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.testTag("load_funds_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Money",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Reward Points inline label
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "350 NPR PTS",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        
        Text(
            text = "Wallet A/C: ${userProfile?.accountNumber ?: "NP-NEOPAY-382910"}",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

        Spacer(modifier = Modifier.height(14.dp))

        // Transfer options formatted as high-contrast button rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WalletActionButton(
                icon = Icons.Filled.ArrowUpward,
                label = "Send",
                tag = "send_funds_btn",
                onClick = { onActionClicked(ActiveSheet.SEND_MONEY) }
            )
            WalletActionButton(
                icon = Icons.Filled.AccountBalance,
                label = "To Bank",
                tag = "bank_transfer_btn",
                onClick = { onActionClicked(ActiveSheet.BANK_TRANSFER) }
            )
        }
    }
}

@Composable
fun WalletActionButton(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class ServiceItem(
    val title: String,
    val icon: ImageVector,
    val sheet: ActiveSheet,
    val type: String = "ELECTRICITY"
)

@Composable
fun QuickServicesGrid(onActionClicked: (ActiveSheet) -> Unit) {
    val items = listOf(
        ServiceItem("Recharge", Icons.Filled.PhoneAndroid, ActiveSheet.MOBILE_RECHARGE),
        ServiceItem("Electricity", Icons.Filled.ElectricBolt, ActiveSheet.UTILITY_BILL, "ELECTRICITY"),
        ServiceItem("Water/KUKL", Icons.Filled.WaterDrop, ActiveSheet.UTILITY_BILL, "WATER"),
        ServiceItem("Broadband", Icons.Filled.Wifi, ActiveSheet.UTILITY_BILL, "INTERNET"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = "UTILITY SERVICES & PAYMENTS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEachIndexed { index, item ->
                // First item is highlighted asymmetrical blue block, others are light variants as per Editorial theme
                val isHighlighted = index == 0
                val containerColor = if (isHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val iconColor = if (isHighlighted) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
                val textColor = MaterialTheme.colorScheme.onBackground

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onActionClicked(item.sheet) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.title,
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun NepalOffersSlider() {
    val offers = listOf(
        "Ncell / NTC Recharge: Flat 3% INSTANT CASHBACK" to "Valid on all topups above Rs. 100",
        "NEA Bill Payment: Rs. 20 Rewards + 0 Service Fee" to "Safe electricity clearance from smart meter",
        "Refer NeoPay to Friends" to "Earn Rs. 100 per verification inside Nepal",
        "Broadband Vianet Recharge Promo" to "Up to 10% Cash Back on active payments"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .height(90.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(offers) { offer ->
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Percent,
                            contentDescription = "Promo",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = offer.first,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = offer.second,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun TransactionRow(tx: TransactionHistory) {
    val isReceive = tx.type == "RECEIVE"
    val isTopup = tx.type == "RECHARGE"
    val dateString = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Avatar corresponding to payment type
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when (tx.type) {
                            "RECEIVE" -> Color(0xFFE8F8F5)
                            "SEND" -> Color(0xFFFDEBD0)
                            "RECHARGE" -> Color(0xFFEBF5FB)
                            else -> Color(0xFFF2F4F4)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tx.type) {
                        "RECEIVE" -> Icons.Filled.ArrowDownward
                        "SEND" -> Icons.Filled.ArrowUpward
                        "RECHARGE" -> Icons.Filled.PhoneAndroid
                        else -> Icons.Filled.ElectricBolt
                    },
                    contentDescription = tx.type,
                    tint = when (tx.type) {
                        "RECEIVE" -> Color(0xFF1ABC9C)
                        "SEND" -> Color(0xFFD35400)
                        "RECHARGE" -> Color(0xFF2980B9)
                        else -> Color(0xFF7F8C8D)
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tx.subtitle,
                    fontSize = 11.sp,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateString,
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isReceive) "+" else "-"} Rs. ${tx.amountNpr}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isReceive) Color(0xFF1ABC9C) else MaterialTheme.colorScheme.onBackground
                )
                if (tx.cashbackAmount > 0) {
                    Text(
                        text = "Cashback: Rs. ${tx.cashbackAmount}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- SCAN & PAY PANEL ---
@Composable
fun ScanPayContent(
    viewModel: WalletViewModel,
    paymentStatus: PaymentStatus,
    onResetStatus: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Select QR, 2: Enter Amount, 3: Success/Status
    var qrPayload by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }

    // Dummy Nepalese shops we can scan
    val mockQrs = listOf(
        "Bhatbhateni Supermarket Kathmandu" to "NPR-BB-MERC-9892",
        "Basantapur Tea Stall" to "NPR-CH-MERC-3211",
        "Patan Durbar Taxi #988" to "NPR-TX-MERC-8839"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fonepay & NeoPay QR Scanner",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Divider(color = Color.LightGray.copy(alpha = 0.3f))

        Spacer(modifier = Modifier.height(16.dp))

        if (step == 1) {
            Text(
                text = "Simulate Scanning Nepali merchant codes below:",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Visually nice QR scanner box model
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .drawBehind {
                        // QR style corner borders
                        val size = this.size
                        val stroke = 6f
                        val len = 40f
                        // Top Left
                        drawLine(Color(0xFF16A085), Offset(0f, 0f), Offset(len, 0f), stroke)
                        drawLine(Color(0xFF16A085), Offset(0f, 0f), Offset(0f, len), stroke)
                        // Top Right
                        drawLine(Color(0xFF16A085), Offset(size.width, 0f), Offset(size.width - len, 0f), stroke)
                        drawLine(Color(0xFF16A085), Offset(size.width, 0f), Offset(size.width, len), stroke)
                        // Bottom Left
                        drawLine(Color(0xFF16A085), Offset(0f, size.height), Offset(len, size.height), stroke)
                        drawLine(Color(0xFF16A085), Offset(0f, size.height), Offset(0f, size.height - len), stroke)
                        // Bottom Right
                        drawLine(Color(0xFF16A085), Offset(size.width, size.height), Offset(size.width - len, size.height), stroke)
                        drawLine(Color(0xFF16A085), Offset(size.width, size.height), Offset(size.width, size.height - len), stroke)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = "Simulated Camera Viewfinder",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Scan options list
            Text("Select a merchant scan code to begin:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(12.dp))

            for (qr in mockQrs) {
                Button(
                    onClick = {
                        qrPayload = qr.first
                        step = 2
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(qr.first, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Scan Code", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        } else if (step == 2) {
            Text(
                text = "Pay to: $qrPayload",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = payAmount,
                onValueChange = { payAmount = it },
                label = { Text("Amount (NPR)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("Payment Note / Remarks") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = enteredPin,
                onValueChange = { enteredPin = it },
                label = { Text("NeoPay 4-Digit Wallet PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    step = 3
                    viewModel.executeSendMoney(
                        phone = "Merchant ($qrPayload)",
                        amountStr = payAmount,
                        note = noteInput,
                        pin = enteredPin
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify & Complete Payment")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { step = 1 }) {
                Text("Cancel Scan")
            }
        } else {
            // Processing/Success rendering
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (paymentStatus) {
                    is PaymentStatus.Processing -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    is PaymentStatus.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF1ABC9C),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NPR Payment Successful!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A085)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(paymentStatus.message, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                step = 1
                                payAmount = ""
                                noteInput = ""
                                enteredPin = ""
                                onResetStatus()
                            }) {
                                Text("Scan Another Code")
                            }
                        }
                    }
                    is PaymentStatus.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Transaction Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(paymentStatus.message, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { step = 2; onResetStatus() }) {
                                Text("Retry Details")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// --- HISTORY FEED FULL PANEL ---
@Composable
fun HistoryContent(transactions: List<TransactionHistory>) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "SEND", "RECEIVE", "UTILITY"

    val filteredList = transactions.filter {
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterType) {
            "ALL" -> true
            "SEND" -> it.type == "SEND"
            "RECEIVE" -> it.type == "RECEIVE"
            "BILL_UTILITY" -> it.type == "BILL_UTILITY"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Wallet Statement / History",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search transactions...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Type Filter pill row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("ALL" to "All", "SEND" to "Paid Out", "RECEIVE" to "Inflow", "BILL_UTILITY" to "Bills")
            for (f in filters) {
                FilterChip(
                    selected = filterType == f.first,
                    onClick = { filterType = f.first },
                    label = { Text(f.second, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No ledger lines matched your filters", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredList) { tx ->
                    TransactionRow(tx = tx)
                }
            }
        }
    }
}

// --- PROMOTIONS & OFFERS FULL CARD ---
@Composable
fun OffersContent() {
    val campaigns = listOf(
        OfferPromoItem(
            title = "Namaste Top-Up Double Bonus",
            desc = "Recharge any Nepal Telecom (Namaste) number with Rs. 200 or more, get instantly 3% balance cashback plus double NPR Reward points!",
            code = "NTCDOUBLE",
            utility = "NTC Topup",
            validity = "Valid till Ashadh 15"
        ),
        OfferPromoItem(
            title = "Kathmandu NEA Clearance Promo",
            desc = "Clear your NEA electricity outstanding with NeoPay Wallet. Enjoy Rs. 50 flat cashback on bills above Rs. 1500 using the code below.",
            code = "NEACLEAR50",
            utility = "NEA Electricity",
            validity = "Valid till Shrawan 1"
        ),
        OfferPromoItem(
            title = "Water / Khanepani Flat Offer",
            desc = "Clear your drinking water bills safely online within Lalitpur, Kathmandu or Pokhara branch counters. Get 2% instant rewards back.",
            code = "WATERONLINE",
            utility = "Khanepani Water",
            validity = "Valid for this month"
        ),
        OfferPromoItem(
            title = "WorldLink Broadband Cashback",
            desc = "Recharge client broadband accounts. Double up high speed internet speeds and secure flat Rs. 150 instant rebate.",
            code = "WLINKFIBER",
            utility = "Internet Bills",
            validity = "Valid till Ashadh 30"
        )
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Active Reward Campaigns & Promos",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )

        Divider(color = Color.LightGray.copy(alpha = 0.3f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(campaigns) { camp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = camp.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = camp.desc,
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Code: ${camp.code}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = camp.validity,
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

data class OfferPromoItem(
    val title: String,
    val desc: String,
    val code: String,
    val utility: String,
    val validity: String
)

// --- CENTRALIZED DIALOG MODAL LAYOUT ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoSheetDialog(
    activeSheet: ActiveSheet,
    utilityType: String,
    viewModel: WalletViewModel,
    paymentStatus: PaymentStatus,
    onClose: () -> Unit,
    onSelectUtilityType: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { onClose() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (activeSheet) {
                            ActiveSheet.SEND_MONEY -> "NeoPay W2W Transfer"
                            ActiveSheet.BANK_TRANSFER -> "To Nepali Bank Account"
                            ActiveSheet.MOBILE_RECHARGE -> "Ncell/NTC Mobile Recharge"
                            ActiveSheet.UTILITY_BILL -> "Pay Utility Bill"
                            ActiveSheet.LOAD_MONEY -> "Add Funds with Link Account"
                            ActiveSheet.PROFILE_SETTINGS -> "Security & Biometric Settings"
                            else -> ""
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { onClose() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (paymentStatus == PaymentStatus.Idle) {
                    when (activeSheet) {
                        ActiveSheet.SEND_MONEY -> SendMoneyForm(viewModel, onClose)
                        ActiveSheet.BANK_TRANSFER -> BankTransferForm(viewModel, onClose)
                        ActiveSheet.MOBILE_RECHARGE -> MobileRechargeForm(viewModel, onClose)
                        ActiveSheet.UTILITY_BILL -> UtilityBillForm(viewModel, utilityType, onSelectUtilityType, onClose)
                        ActiveSheet.LOAD_MONEY -> LoadMoneyForm(viewModel, onClose)
                        ActiveSheet.PROFILE_SETTINGS -> ProfileSettingsForm(viewModel, onClose)
                        else -> {}
                    }
                } else {
                    PaymentStatusLayout(paymentStatus, onClose)
                }
            }
        }
    }
}

// --- SUB-FORMS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyForm(viewModel: WalletViewModel, onClose: () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBiometricEnabled = userProfile?.isFingerprintEnabled ?: true

    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Other") }
    var showVerificationScanner by remember { mutableStateOf(false) }

    if (showVerificationScanner) {
        FingerprintVerificationPrompt(
            onSuccess = {
                showVerificationScanner = false
                pin = userProfile?.securePin ?: "1234"
                viewModel.executeSendMoney(phone, amount, note, pin, category = selectedCategory)
            },
            onCancel = {
                showVerificationScanner = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Recipient Phone Number (98xxxxxxxx)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().testTag("send_phone_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (NPR Rupees)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("send_amount_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Add Personal Note / Purpose") },
            modifier = Modifier.fillMaxWidth().testTag("send_note_input")
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text("Transaction Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            items(listOf("Food", "Transport", "Entertainment", "Utilities", "Recharge", "Other")) { cat ->
                val isSel = selectedCategory == cat
                FilterChip(
                    selected = isSel,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Enter 4-Digit Wallet PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                if (isBiometricEnabled) {
                    IconButton(
                        onClick = { showVerificationScanner = true },
                        modifier = Modifier.testTag("send_fingerprint_quick")
                    ) {
                        Icon(
                            Icons.Filled.Fingerprint,
                            contentDescription = "Quick Biometric Approval",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("send_pin_input")
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.executeSendMoney(phone, amount, note, pin, category = selectedCategory)
            },
            modifier = Modifier.fillMaxWidth().testTag("submit_send_btn")
        ) {
            Text("Confirm Transfer")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankTransferForm(viewModel: WalletViewModel, onClose: () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBiometricEnabled = userProfile?.isFingerprintEnabled ?: true

    val banks = listOf("Nabil Bank Limited", "Global IME Bank", "NIC ASIA Bank", "Everest Bank Limited", "Kumari Bank")
    var expanded by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf(banks[0]) }
    var accountNum by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Other") }
    var showVerificationScanner by remember { mutableStateOf(false) }

    if (showVerificationScanner) {
        FingerprintVerificationPrompt(
            onSuccess = {
                showVerificationScanner = false
                pin = userProfile?.securePin ?: "1234"
                viewModel.executeBankTransfer(selectedBank, accountNum, accountName, amount, note, pin, category = selectedCategory)
            },
            onCancel = {
                showVerificationScanner = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().testTag("select_bank_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bank: $selectedBank", fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (b in banks) {
                    DropdownMenuItem(
                        text = { Text(b) },
                        onClick = {
                            selectedBank = b
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = accountNum,
            onValueChange = { accountNum = it },
            label = { Text("Account Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = accountName,
            onValueChange = { accountName = it },
            label = { Text("Recipient Full Account Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (NPR)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Transfer Remark") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text("Transaction Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            items(listOf("Food", "Transport", "Entertainment", "Utilities", "Recharge", "Other")) { cat ->
                val isSel = selectedCategory == cat
                FilterChip(
                    selected = isSel,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Wallet Security PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                if (isBiometricEnabled) {
                    IconButton(
                        onClick = { showVerificationScanner = true },
                        modifier = Modifier.testTag("bank_fingerprint_quick")
                    ) {
                        Icon(
                            Icons.Filled.Fingerprint,
                            contentDescription = "Quick Biometric Approval",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.executeBankTransfer(selectedBank, accountNum, accountName, amount, note, pin, category = selectedCategory)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Bank Remittance")
        }
    }
}

@Composable
fun MobileRechargeForm(viewModel: WalletViewModel, onClose: () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBiometricEnabled = userProfile?.isFingerprintEnabled ?: true

    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showVerificationScanner by remember { mutableStateOf(false) }

    val resolvedOp = viewModel.resolveMobileOperator(phone)

    if (showVerificationScanner) {
        FingerprintVerificationPrompt(
            onSuccess = {
                showVerificationScanner = false
                pin = userProfile?.securePin ?: "1234"
                viewModel.executeMobileRecharge(phone, resolvedOp, amount, pin)
            },
            onCancel = {
                showVerificationScanner = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Recharge Number (98xxxxxxxx)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        
        if (phone.length >= 3) {
            Text(
                text = "Detected Vendor: $resolvedOp",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (NPR: Rs. 10 - Rs. 1000)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Security Wallet PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                if (isBiometricEnabled) {
                    IconButton(
                        onClick = { showVerificationScanner = true },
                        modifier = Modifier.testTag("recharge_fingerprint_quick")
                    ) {
                        Icon(
                            Icons.Filled.Fingerprint,
                            contentDescription = "Quick Biometric Approval",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.executeMobileRecharge(phone, resolvedOp, amount, pin)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Top-Up Recharge")
        }
    }
}

@Composable
fun UtilityBillForm(
    viewModel: WalletViewModel,
    utilityType: String,
    onSelectUtilityType: (String) -> Unit,
    onClose: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBiometricEnabled = userProfile?.isFingerprintEnabled ?: true

    val providers = when (utilityType) {
        "ELECTRICITY" -> listOf("NEA - Nepal Electricity Authority")
        "WATER" -> listOf("KUKL - Kathmandu Upatyaka Khanepani", "Lalitpur Khanepani Board")
        "INTERNET" -> listOf("WorldLink Broadband", "Vianet Fiber", "Subisu CableNet")
        else -> listOf("Nepal Telecom Billing")
    }

    var expandedType by remember { mutableStateOf(false) }
    var expandedProv by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf(providers[0]) }
    var accountId by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showVerificationScanner by remember { mutableStateOf(false) }

    if (showVerificationScanner) {
        FingerprintVerificationPrompt(
            onSuccess = {
                showVerificationScanner = false
                pin = userProfile?.securePin ?: "1234"
                viewModel.executeUtilityBill(selectedProvider, utilityType, accountId, billAmount, pin)
            },
            onCancel = {
                showVerificationScanner = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Utility Sector Type Choice
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expandedType = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Service Sector: $utilityType", fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                DropdownMenuItem(text = { Text("Electricity (NEA)") }, onClick = { onSelectUtilityType("ELECTRICITY"); expandedType = false })
                DropdownMenuItem(text = { Text("Khanepani Water") }, onClick = { onSelectUtilityType("WATER"); expandedType = false })
                DropdownMenuItem(text = { Text("Broadband Internet") }, onClick = { onSelectUtilityType("INTERNET"); expandedType = false })
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Provider select
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expandedProv = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Biller: ${selectedProvider.take(24)}...")
            }
            DropdownMenu(expanded = expandedProv, onDismissRequest = { expandedProv = false }) {
                for (p in providers) {
                    DropdownMenuItem(text = { Text(p) }, onClick = { selectedProvider = p; expandedProv = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = accountId,
            onValueChange = { accountId = it },
            label = { Text("Consumer ID / Account CID") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = billAmount,
            onValueChange = { billAmount = it },
            label = { Text("Bill Amount (NPR)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Secret 4-Digit Wallet PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                if (isBiometricEnabled) {
                    IconButton(
                        onClick = { showVerificationScanner = true },
                        modifier = Modifier.testTag("utility_fingerprint_quick")
                    ) {
                        Icon(
                            Icons.Filled.Fingerprint,
                            contentDescription = "Quick Biometric Approval",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.executeUtilityBill(selectedProvider, utilityType, accountId, billAmount, pin)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Process Utility Bill")
        }
    }
}

@Composable
fun LoadMoneyForm(viewModel: WalletViewModel, onClose: () -> Unit) {
    val banks = listOf("Global IME Linked A/C", "Nabil Bank Express", "ConnectIPS Gateway", "NeoPay Agent Counters")
    var expanded by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf(banks[0]) }
    var amount by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load Source: $selectedBank")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (b in banks) {
                    DropdownMenuItem(text = { Text(b) }, onClick = { selectedBank = b; expanded = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Enter Load Amount (NPR)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("load_amount_input")
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.executeLoadMoney(selectedBank, amount)
            },
            modifier = Modifier.fillMaxWidth().testTag("submit_load_btn")
        ) {
            Text("Authorize Load Transfer")
        }
    }
}

// --- SECURE PAYMENT TRANSACTION LEDGER OR STATE VISUALIZER ---
@Composable
fun PaymentStatusLayout(
    status: PaymentStatus,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (status) {
            is PaymentStatus.Processing -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verifying with NeoPay Trust Gateways...",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            is PaymentStatus.Success -> {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Tx Successful",
                    tint = Color(0xFF1ABC9C),
                    modifier = Modifier.size(70.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PAYMENT SUCCESSFUL!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A085)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status.message,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                if (status.cashbackEarned > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🎁 Cash Back Credited: NPR Rs. ${String.format("%.2f", status.cashbackEarned)}!",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onClose() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Return to Dashboard")
                }
            }
            is PaymentStatus.Error -> {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = "Tx Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(70.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Payment Failed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status.message,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onClose() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dismiss & Go Back")
                }
            }
            else -> {}
        }
    }
}

@Composable
fun BudgetDonutChart(
    categorySpending: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = categorySpending.values.sum()
    val colors = listOf(
        Color(0xFF005FB0), // Food
        Color(0xFFF39C12), // Transport
        Color(0xFFBA1A1A), // Entertainment
        Color(0xFF0F604B), // Utilities
        Color(0xFF9ECAFF), // Recharge
        Color(0xFF74777F)  // Other
    )

    Box(
        modifier = modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        if (total > 0.0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                categorySpending.entries.forEachIndexed { index, entry ->
                    val sweepAngle = (entry.value / total * 360.0).toFloat()
                    val color = colors.getOrElse(index) { Color.LightGray }
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 24f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TOTAL SPENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Rs. ${String.format("%.0f", total)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // Placeholder empty donut arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 20f)
                )
            }
            Text(
                text = "No spending",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun BudgetAlertsSection(alerts: List<String>) {
    if (alerts.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.NotificationImportant,
                        contentDescription = "Alerts",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BUDGET ALERTS & NOTIFICATIONS (${alerts.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                alerts.forEach { alert ->
                    Text(
                        text = alert,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetContent(
    viewModel: WalletViewModel
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val alerts by viewModel.budgetAlerts.collectAsStateWithLifecycle()

    val categoriesList = listOf("Food", "Transport", "Entertainment", "Utilities", "Recharge", "Other")

    // State for setting budget limit
    var selectedCategory by remember { mutableStateOf("Food") }
    var limitInput by remember { mutableStateOf("") }
    var limitError by remember { mutableStateOf<String?>(null) }

    // Compute spent map
    val spentMap = remember(transactions) {
        categoriesList.associateWith { cat ->
            transactions.filter { 
                it.category.equals(cat, ignoreCase = true) && 
                it.type != "RECEIVE" 
            }.sumOf { it.amountNpr }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("budget_lazy_column"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. Editorial Aesthetic Header
        item {
            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                Text(
                    text = "PERSONAL BUDGETING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SPENDING CONTROL",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 2. Notifications & Alerts
        item {
            BudgetAlertsSection(alerts = alerts)
        }

        // 3. Spending Pattern Visualizer Dashboard
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SPENDING DISTRIBUTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BudgetDonutChart(categorySpending = spentMap, modifier = Modifier.weight(1f))

                        // Legend list side-by-side
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1.2f).padding(start = 12.dp)
                        ) {
                            val colors = listOf(
                                Color(0xFF005FB0), // Food
                                Color(0xFFF39C12), // Transport
                                Color(0xFFBA1A1A), // Entertainment
                                Color(0xFF0F604B), // Utilities
                                Color(0xFF9ECAFF), // Recharge
                                Color(0xFF74777F)  // Other
                            )
                            spentMap.entries.forEachIndexed { idx, entry ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(colors.getOrElse(idx) { Color.LightGray }, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${entry.key}: Rs. ${String.format("%.0f", entry.value)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Configuration Form: Set Spending Limit
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SET SPENDING LIMIT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Category chips
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoriesList) { cat ->
                            val isSel = selectedCategory == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = {
                            limitInput = it
                            limitError = null
                        },
                        label = { Text("Limit Amount (NPR)") },
                        leadingIcon = { Text("Rs.", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                    )

                    if (limitError != null) {
                        Text(
                            text = limitError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val amount = limitInput.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                limitError = "Please enter a valid spent limit in NPR"
                            } else {
                                viewModel.setBudgetLimit(selectedCategory, amount)
                                limitInput = ""
                                limitError = null
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_budget_btn")
                    ) {
                        Text("Add / Update Limit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Active Limits list
        item {
            Text(
                text = "ACTIVE CATEGORY LIMITS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        if (budgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No spending limits set. Set a spent limit above to activate alerts.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = Color.LightGray
                        )
                    }
                }
            }
        } else {
            items(budgets) { budget ->
                val spent = spentMap[budget.category] ?: 0.0
                val limit = budget.limitNpr
                val pct = if (limit > 0.0) spent / limit else 0.0
                
                // Color mapping for threshold states
                val barColor = when {
                    pct >= 1.0 -> MaterialTheme.colorScheme.error
                    pct >= 0.8 -> Color(0xFFF39C12) // Warning Orange
                    else -> MaterialTheme.colorScheme.primary
                }

                val categoryIcon = when (budget.category) {
                    "Food" -> Icons.Filled.Restaurant
                    "Transport" -> Icons.Filled.DirectionsCar
                    "Entertainment" -> Icons.Filled.Movie
                    "Utilities" -> Icons.Filled.Lightbulb
                    "Recharge" -> Icons.Filled.Smartphone
                    else -> Icons.Filled.Category
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("budget_item_card_${budget.category.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (pct >= 1.0) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color.Transparent
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(barColor.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        categoryIcon,
                                        contentDescription = budget.category,
                                        tint = barColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = budget.category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Spent Rs. ${String.format("%,.0f", spent)} of Rs. ${String.format("%,.0f", limit)}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${String.format("%.0f", pct * 100)}%",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = barColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.deleteBudgetLimit(budget.category) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete Limit",
                                        tint = Color.Gray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = pct.coerceIn(0.0, 1.0).toFloat(),
                            color = barColor,
                            trackColor = barColor.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSettingsForm(viewModel: WalletViewModel, onClose: () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isFingerprintEnabled = userProfile?.isFingerprintEnabled ?: true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_settings_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Profile Monogram Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userProfile?.name?.take(1)?.uppercase() ?: "N",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = userProfile?.name ?: "Nepal NeoPay User",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userProfile?.phoneNumber ?: "+977 98XXXXXXXX",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CardMembership,
                            contentDescription = "KYC Verified Badge",
                            tint = Color(0xFF0F604B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "KYC SEED VERIFIED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = Color(0xFF0F604B)
                        )
                    }
                }
            }
        }

        // Account Metadata Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("NeoPay Account", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(userProfile?.accountNumber ?: "NP-NEOPAY-382910", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Secure Unlock PIN", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text("•••• (Default: 1234)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Text(
            text = "SECURITY & BIOMETRIC AUTH",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Biometric Settings Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Fingerprint,
                                contentDescription = "Fingerprint Option",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Fingerprint Lock on Startup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Locks app and prompts biometric check",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = isFingerprintEnabled,
                        onCheckedChange = { viewModel.setFingerprintEnabled(it) },
                        modifier = Modifier.testTag("toggle_fingerprint_lock")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions: Lock App Now button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onClose()
                    viewModel.lockApp()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_lock_now"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "Lock App", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock App Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { onClose() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FingerprintVerificationPrompt(
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var isScanning by remember { mutableStateOf(true) }
    var resultMessage by remember { mutableStateOf("Touch biometric sensor") }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1110)
        isScanning = false
        resultMessage = "Biometric Match Confirmed!"
        kotlinx.coroutines.delay(400)
        onSuccess()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { onCancel() }
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .testTag("biometric_sheet"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = "Secure NeoPay",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )

                Text(
                    text = "Verify Identity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Confirm biometric credential list to authenticate with secure fingerprint scanner",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pulsating Scanner Circle
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            if (isScanning) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color(0xFF0F604B).copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = "Sensor Touch point",
                        tint = if (isScanning) MaterialTheme.colorScheme.primary else Color(0xFF0F604B),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("scan_fingerprint_touch")
                    )
                }

                Text(
                    text = resultMessage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isScanning) MaterialTheme.colorScheme.primary else Color(0xFF0F604B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { onCancel() },
                    modifier = Modifier.fillMaxWidth().testTag("cancel_biometric_btn")
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun NeoLockScreen(
    viewModel: WalletViewModel
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isFingerprintEnabled = userProfile?.isFingerprintEnabled ?: true
    val securePin = userProfile?.securePin ?: "1234"

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBiometricsPrompt by remember { mutableStateOf(false) }

    // Auto show biometrics if enabled
    LaunchedEffect(isFingerprintEnabled) {
        if (isFingerprintEnabled) {
            showBiometricsPrompt = true
        }
    }

    if (showBiometricsPrompt) {
        FingerprintVerificationPrompt(
            onSuccess = {
                showBiometricsPrompt = false
                viewModel.unlockApp()
                enteredPin = ""
                errorMessage = null
            },
            onCancel = {
                showBiometricsPrompt = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("neo_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Top Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Wallet lock emblem",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NEOPAY NEPAL",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "SECURE WALLET AUTHENTICATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color.Gray
                )
            }

            // PIN dot Indicators & Error message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { idx ->
                        val isFilled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    2.dp,
                                    if (isFilled) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = errorMessage ?: "Enter your 4-digit PIN to access wallet",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // PIN Keypad with integrated Fingerprint icon
            Column(
                modifier = Modifier
                    .height(290.dp)
                    .width(280.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("F", "0", "C") // F = Biometric Fingerprint, C = Clear
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { key ->
                            val isSpecial = key == "F" || key == "C"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.4f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when {
                                            key == "F" && !isFingerprintEnabled -> Color.Transparent
                                            key == "F" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            isSpecial -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable {
                                        if (key == "C") {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                            errorMessage = null
                                        } else if (key == "F") {
                                            if (isFingerprintEnabled) {
                                                showBiometricsPrompt = true
                                            }
                                        } else {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                errorMessage = null
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == securePin) {
                                                        viewModel.unlockApp()
                                                    } else {
                                                        errorMessage = "Incorrect PIN. Please try again."
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("keypad_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "F" -> {
                                        if (isFingerprintEnabled) {
                                            Icon(
                                                Icons.Filled.Fingerprint,
                                                contentDescription = "Sensor Login Quick Tap",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    "C" -> {
                                        Icon(
                                            Icons.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = key,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
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

