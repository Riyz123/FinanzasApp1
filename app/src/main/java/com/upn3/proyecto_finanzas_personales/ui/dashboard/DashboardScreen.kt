package com.upn3.proyecto_finanzas_personales.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.upn3.proyecto_finanzas_personales.model.Category
import com.upn3.proyecto_finanzas_personales.model.Transaction
import com.upn3.proyecto_finanzas_personales.model.TransactionType
import com.upn3.proyecto_finanzas_personales.model.TransferContact
import com.upn3.proyecto_finanzas_personales.model.Wallet
import com.upn3.proyecto_finanzas_personales.ui.components.ConversionDetailDialog
import com.upn3.proyecto_finanzas_personales.ui.components.NumericKeyboard
import com.upn3.proyecto_finanzas_personales.ui.components.TransactionDetailDialog
import com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import java.util.*
import androidx.compose.foundation.clickable
import android.net.Uri
import coil.compose.AsyncImage
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.yalantis.ucrop.UCrop
import java.io.File
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditBalanceDialog by remember { mutableStateOf(false) }
    var newBalanceText by remember { mutableStateOf("") }
    
    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedConversion by remember { mutableStateOf<Transaction?>(null) }
    var showConversionDialog by remember { mutableStateOf(false) }
    var selectedTransactionDetail by remember { mutableStateOf<Transaction?>(null) }
    var showTransactionDetailDialog by remember { mutableStateOf(false) }
    var selectedCategoryIcon by remember { mutableStateOf("Category") }
    var showVoucherDialog by remember {mutableStateOf(false)}
    var editAmount by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf<Category?>(null) }
    var editDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showEditDatePicker by remember { mutableStateOf(false) }
    var editReceiptPath by remember { mutableStateOf<String?>(null) }

    val searchQuery = uiState.searchQuery
    val selectedTab = uiState.selectedTab
    val filteredTransactions = uiState.filteredTransactions


    var showTransferDialog by remember { mutableStateOf(false) }
    var transferAmount by remember { mutableStateOf("") }
    var selectedToWallet by remember { mutableStateOf<Wallet?>(null) }
    var transferMode by remember { mutableStateOf(0) }
    var extRecipientName by remember { mutableStateOf("") }
    var extRecipientAlias by remember { mutableStateOf("") }
    var extBank by remember { mutableStateOf("") }
    var extMotivo by remember { mutableStateOf("") }
    var extTransferDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showExtDatePicker by remember { mutableStateOf(false) }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var showEditWalletDialog by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<com.upn3.proyecto_finanzas_personales.model.Wallet?>(null) }
    var showGlobalBalanceDialog by remember { mutableStateOf(false) }

    val currencies = listOf(
        Pair("PEN", "🇵🇪 Sol Peruano"),
        Pair("USD", "🇺🇸 Dólar Estadounidense"),
        Pair("EUR", "🇪🇺 Euro"),
        Pair("GBP", "🇬🇧 Libra Esterlina"),
        Pair("JPY", "🇯🇵 Yen Japonés"),
        Pair("MXN", "🇲🇽 Peso Mexicano"),
        Pair("CLP", "🇨🇱 Peso Chileno"),
        Pair("BRL", "🇧🇷 Real Brasileño")
    )

    val currentUser = uiState.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    val editCropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            UCrop.getOutput(result.data!!)?.let { resultUri ->
                val dir = File(context.filesDir, "vouchers").also { it.mkdirs() }
                val file = File(dir, "voucher_edit_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(resultUri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                editReceiptPath = Uri.fromFile(file).toString()
            }
        }
    }
    val editImagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val dest = Uri.fromFile(File(context.cacheDir, "voucher_edit_${System.currentTimeMillis()}.jpg"))
            val intent = UCrop.of(it, dest)
                .withOptions(UCrop.Options().apply { setFreeStyleCropEnabled(true) })
                .getIntent(context)
            editCropLauncher.launch(intent)
        }
    }

    // Clear error when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearError()
        }
    }

    val onRefresh: () -> Unit = {
        scope.launch {
            isRefreshing = true
            viewModel.loadWallets()
            viewModel.loadTransactions()
            delay(1000)
            isRefreshing = false
        }
    }

    val pageCount = uiState.wallets.size + 1
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    // Sincronizar el Pager con la billetera seleccionada (ViewModel -> UI)
    LaunchedEffect(uiState.selectedWallet?.id) {
        val idx = uiState.wallets.indexOfFirst { it.id == uiState.selectedWallet?.id }
        if (idx >= 0 && idx != pagerState.currentPage) {
            pagerState.animateScrollToPage(idx)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navColors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Inicio", fontSize = 10.sp) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToReports,
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("Reportes", fontSize = 10.sp) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToTransactions,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                        }
                    },
                    label = { Text("") },
                    alwaysShowLabel = false,
                    colors = navColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showGlobalBalanceDialog = true },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, null) },
                    label = { Text("Global", fontSize = 10.sp) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = {
                        val pic = uiState.currentUser?.profilePicture
                        if (!pic.isNullOrEmpty()) {
                            AsyncImage(
                                model = pic, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(26.dp).clip(CircleShape)
                            )
                        } else { Icon(Icons.Default.Person, null) }
                    },
                    label = { Text("Perfil", fontSize = 10.sp) },
                    colors = navColors
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── 1. Greeting header ────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                dashboardGreeting(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                currentUser?.name ?: "Usuario",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { showTransferDialog = true },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                onClick = { viewModel.logout(onLogout) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // ── 2. Wallet hero cards (HorizontalPager) ────────────────────────
                item {
                    Column {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            pageSpacing = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            if (page < uiState.wallets.size) {
                                val wallet = uiState.wallets[page]
                                WalletHeroCard(
                                    wallet = wallet,
                                    currencySymbol = viewModel.getCurrencySymbol(wallet.currencyCode),
                                    onEditClick = { editingWallet = wallet; showEditWalletDialog = true },
                                    onBalanceEditClick = {
                                        newBalanceText = wallet.balance.toString()
                                        showEditBalanceDialog = true
                                        viewModel.clearError()
                                    },
                                    onSelect = { viewModel.selectWallet(wallet) },
                                    incomeTotal = uiState.chartIncome,
                                    expenseTotal = uiState.chartExpense,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Card(
                                    onClick = { showAddWalletDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(190.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(56.dp)) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Text("Nueva Billetera", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(uiState.wallets.size + 1) { index ->
                                val isCurrent = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (isCurrent) 8.dp else 5.dp)
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 3. Quick stats row ────────────────────────────────────────────
                item {
                    val sym = viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardStatCard(
                            icon = Icons.AutoMirrored.Filled.TrendingUp, label = "Ingresos",
                            amount = "$sym ${String.format("%.2f", uiState.chartIncome)}",
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            icon = Icons.AutoMirrored.Filled.TrendingDown, label = "Gastos",
                            amount = "$sym ${String.format("%.2f", uiState.chartExpense)}",
                            color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── 4. Statistics section ─────────────────────────────────────────
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        StatisticsSection(
                            transactions = uiState.convertedTransactions,
                            currencySymbol = viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN"),
                            selectedWalletId = uiState.selectedWallet?.id,
                            getCurrencySymbol = viewModel::getCurrencySymbol,
                            generalIncome = uiState.chartIncome,
                            generalExpense = uiState.chartExpense
                        )
                    }
                }

                // ── 5. Transaction header + search + tabs ─────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 8.dp)) {
                        Text("Movimientos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar...") },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(selectedTab),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            listOf("Todos", "Gastos", "Ingresos", "Transferencias", "Conversiones").forEachIndexed { index, label ->
                                Tab(selected = selectedTab == index, onClick = { viewModel.updateSelectedTab(index) }, text = { Text(label) })
                            }
                        }
                    }
                }

                // ── 6. Transaction items ──────────────────────────────────────────
                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Inbox, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                Text("Sin movimientos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { transaction ->
                        PremiumTransactionItem(
                            transaction = transaction,
                            categories = uiState.categories,
                            viewModel = viewModel,
                            onItemClick = {
                                if (transaction.origin == "Ajuste de Moneda") {
                                    selectedConversion = transaction
                                    showConversionDialog = true
                                } else {
                                    selectedCategoryIcon = uiState.categories.find { it.name == transaction.origin }?.icon ?: "Category"
                                    selectedTransactionDetail = transaction
                                    showTransactionDetailDialog = true
                                }
                            },
                            onEditClick = {
                                editingTransaction = transaction
                                editAmount = transaction.amount.toString()
                                editDescription = transaction.description
                                editCategory = uiState.categories.find { it.name == transaction.origin }
                                editReceiptPath = transaction.receiptPath
                                showEditTransactionDialog = true
                                editDate = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                                viewModel.clearError()
                            },
                            onDeleteClick = { viewModel.deleteTransaction(transaction.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }
    }

    if (showGlobalBalanceDialog) {
        var expandedCurrency by remember { mutableStateOf(false) }
        val lastUpdateText = if (uiState.lastRatesUpdate > 0) {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            "Actualizado: ${sdf.format(java.util.Date(uiState.lastRatesUpdate))}"
        } else {
            "Cargando tasas..."
        }

        AlertDialog(
            onDismissRequest = { showGlobalBalanceDialog = false },
            title = { Text("Resumen Global", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total combinado:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            lastUpdateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${viewModel.getCurrencySymbol(uiState.preferredCurrency)} ${String.format("%.2f", uiState.globalBalance)}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Box {
                        OutlinedTextField(
                            value = currencies.find { it.first == uiState.preferredCurrency }?.second ?: uiState.preferredCurrency,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Convertir a") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expandedCurrency = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency.second) },
                                    onClick = {
                                        viewModel.setPreferredCurrency(currency.first)
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    
                    Text("Desglose por billetera:", style = MaterialTheme.typography.labelLarge)
                    uiState.wallets.forEach { wallet ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(wallet.name, style = MaterialTheme.typography.bodySmall)
                            Text("${viewModel.getCurrencySymbol(wallet.currencyCode)} ${String.format("%.2f", wallet.balance)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showGlobalBalanceDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showAddWalletDialog) {
        var walletName by remember { mutableStateOf("") }
        var selectedCurrency by remember { mutableStateOf(currencies[0]) }
        var expanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showAddWalletDialog = false },
            title = { Text("Nueva Billetera") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = { walletName = it },
                        label = { Text("Nombre de la Billetera") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Box {
                        OutlinedTextField(
                            value = selectedCurrency.second,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Moneda") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency.second) },
                                    onClick = {
                                        selectedCurrency = currency
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (walletName.isNotBlank()) {
                        viewModel.createWallet(com.upn3.proyecto_finanzas_personales.model.Wallet(
                            name = walletName,
                            currencyCode = selectedCurrency.first
                        ))
                        showAddWalletDialog = false
                    }
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWalletDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showEditWalletDialog && editingWallet != null) {
        var walletName by remember { mutableStateOf(editingWallet!!.name) }
        var selectedCurrency by remember { mutableStateOf(currencies.find { it.first == editingWallet!!.currencyCode } ?: currencies[0]) }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditWalletDialog = false },
            title = { Text("Editar Billetera") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = { walletName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Box {
                        OutlinedTextField(
                            value = selectedCurrency.second,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Moneda") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency.second) },
                                    onClick = {
                                        selectedCurrency = currency
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    LaunchedEffect(selectedCurrency.first) {
                        if (editingWallet != null && selectedCurrency.first != editingWallet!!.currencyCode) {
                            viewModel.fetchExchangeRatePreview(editingWallet!!.currencyCode, selectedCurrency.first)
                        } else {
                            viewModel.clearExchangeRatePreview()
                        }
                    }

                    if (selectedCurrency.first != editingWallet!!.currencyCode) {
                        if (uiState.isExchangeLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (uiState.exchangeRatePreview != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Vista previa del cambio:", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "1 ${editingWallet!!.currencyCode} = ${String.format("%.4f", uiState.exchangeRatePreview)} ${selectedCurrency.first}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Saldo estimado: ${viewModel.getCurrencySymbol(selectedCurrency.first)} ${String.format("%.2f", editingWallet!!.balance * uiState.exchangeRatePreview!!)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteWallet(editingWallet!!.id)
                            showEditWalletDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar")
                    }
                    Button(
                        onClick = {
                            val rate = uiState.exchangeRatePreview ?: 1.0
                            val updatedWallet = editingWallet!!.copy(
                                name = walletName,
                                currencyCode = selectedCurrency.first,
                                balance = if (selectedCurrency.first != editingWallet!!.currencyCode) editingWallet!!.balance * rate else editingWallet!!.balance
                            )
                            viewModel.updateWallet(updatedWallet)
                            showEditWalletDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isExchangeLoading
                    ) {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWalletDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showTransferDialog) {
        val fromWallet = uiState.selectedWallet

        LaunchedEffect(selectedToWallet?.id, transferMode) {
            if (transferMode == 0 && fromWallet != null && selectedToWallet != null) {
                viewModel.fetchExchangeRatePreview(fromWallet.currencyCode, selectedToWallet!!.currencyCode)
            }
        }

        fun resetAndClose() {
            showTransferDialog = false
            transferAmount = ""
            selectedToWallet = null
            transferMode = 0
            extRecipientName = ""
            extRecipientAlias = ""
            extBank = ""
            extMotivo = ""
            extTransferDate = Calendar.getInstance()
            viewModel.clearError()
        }

        Dialog(
            onDismissRequest = { resetAndClose() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {

                val extDatePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = extTransferDate.let {
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCal.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                        utcCal.set(Calendar.MILLISECOND, 0)
                        utcCal.timeInMillis
                    }
                )

                if (showExtDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showExtDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                extDatePickerState.selectedDateMillis?.let { utcMillis ->
                                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
                                    val localCal = Calendar.getInstance().apply {
                                        set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    extTransferDate = localCal
                                }
                                showExtDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showExtDatePicker = false }) { Text("Cancelar") } }
                    ) {
                        DatePicker(state = extDatePickerState)
                    }
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Transferencia") },
                            navigationIcon = {
                                IconButton(onClick = { resetAndClose() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        TabRow(selectedTabIndex = transferMode) {
                            Tab(
                                selected = transferMode == 0,
                                onClick = { transferMode = 0; viewModel.clearError() },
                                text = { Text("Entre Billeteras") }
                            )
                            Tab(
                                selected = transferMode == 1,
                                onClick = { transferMode = 1; viewModel.clearError() },
                                text = { Text("A otra persona") }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (transferMode == 0) {
                                // ── Entre Billeteras ─────────────────────────────
                                Text(
                                    "Desde: ${fromWallet?.name} (${fromWallet?.currencyCode})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text("Hacia:", style = MaterialTheme.typography.labelLarge)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(
                                        uiState.wallets.filter { it.id != fromWallet?.id },
                                        key = { it.id }
                                    ) { wallet ->
                                        val isSelected = selectedToWallet?.id == wallet.id
                                        val flag = currencies.find { it.first == wallet.currencyCode }?.second?.take(2) ?: "💰"
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedToWallet = wallet
                                                if (fromWallet != null) viewModel.fetchExchangeRatePreview(fromWallet.currencyCode, wallet.currencyCode)
                                            },
                                            label = { Text("$flag ${wallet.name}") }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = transferAmount,
                                    onValueChange = {
                                        val s = it.replace(",", ".")
                                        if (s.isEmpty() || s.toDoubleOrNull() != null) {
                                            if (!s.contains(".") || s.substringAfter(".").length <= 2) {
                                                transferAmount = s; viewModel.clearError()
                                            }
                                        }
                                    },
                                    label = { Text("Monto (${viewModel.getCurrencySymbol(fromWallet?.currencyCode ?: "PEN")})") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                    ),
                                    singleLine = true
                                )
                                if (selectedToWallet != null && fromWallet != null && fromWallet.currencyCode != selectedToWallet!!.currencyCode) {
                                    if (uiState.isExchangeLoading) {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    } else {
                                        val rate = uiState.exchangeRatePreview
                                        if (rate != null) {
                                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text("Tipo de cambio actual:", style = MaterialTheme.typography.labelSmall)
                                                    Text(
                                                        "1 ${fromWallet.currencyCode} = ${String.format("%.4f", rate)} ${selectedToWallet!!.currencyCode}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    val amt = transferAmount.toDoubleOrNull() ?: 0.0
                                                    Text(
                                                        "Recibirán: ${viewModel.getCurrencySymbol(selectedToWallet!!.currencyCode)} ${String.format("%.2f", amt * rate)}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ── A otra persona ───────────────────────────────
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Text("Desde: ${fromWallet?.name} (${fromWallet?.currencyCode})", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                OutlinedTextField(
                                    value = extRecipientName,
                                    onValueChange = { extRecipientName = it; viewModel.clearError() },
                                    label = { Text("Nombre del destinatario *") },
                                    leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = extRecipientAlias,
                                    onValueChange = { extRecipientAlias = it; viewModel.clearError() },
                                    label = { Text("Alias / CBU / Cuenta *") },
                                    leadingIcon = { Icon(Icons.Default.AlternateEmail, null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = extMotivo,
                                    onValueChange = { extMotivo = it },
                                    label = { Text("Motivo (opcional)") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = transferAmount,
                                    onValueChange = {
                                        val s = it.replace(",", ".")
                                        if (s.isEmpty() || s.toDoubleOrNull() != null) {
                                            if (!s.contains(".") || s.substringAfter(".").length <= 2) {
                                                transferAmount = s; viewModel.clearError()
                                            }
                                        }
                                    },
                                    label = { Text("Monto (${viewModel.getCurrencySymbol(fromWallet?.currencyCode ?: "PEN")}) *") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                    ),
                                    singleLine = true
                                )
                                val extDateFmt = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
                                OutlinedTextField(
                                    value = extDateFmt.format(extTransferDate.time),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Fecha") },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { showExtDatePicker = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            if (uiState.errorMessage != null) {
                                Text(
                                    uiState.errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Button(
                                onClick = {
                                    if (transferMode == 0) {
                                        val amt = transferAmount.toDoubleOrNull()
                                        val from = fromWallet
                                        val to = selectedToWallet
                                        if (amt != null && from != null && to != null) {
                                            viewModel.transferMoney(from, to, amt) { resetAndClose() }
                                        }
                                    } else {
                                        val amt = transferAmount.toDoubleOrNull()
                                        val from = fromWallet
                                        when {
                                            extRecipientName.isBlank() -> viewModel.setError("El nombre del destinatario es requerido")
                                            extRecipientAlias.isBlank() -> viewModel.setError("El alias o cuenta es requerido")
                                            amt == null || amt <= 0 -> viewModel.setError("Ingresa un monto válido")
                                            from == null -> viewModel.setError("No hay billetera seleccionada")
                                            amt > from.balance -> viewModel.setError("Saldo insuficiente")
                                            else -> viewModel.addExternalTransfer(
                                                fromWallet = from,
                                                amount = amt,
                                                contact = TransferContact(
                                                    recipientName = extRecipientName.trim(),
                                                    recipientAlias = extRecipientAlias.trim(),
                                                    bank = extBank.trim(),
                                                    motivo = extMotivo.trim()
                                                ),
                                                timestamp = extTransferDate.timeInMillis,
                                                onSuccess = { resetAndClose() }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("CONFIRMAR TRANSFERENCIA", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showEditBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showEditBalanceDialog = false },
            title = { 
                Text(
                    "GESTIÓN DE BÓVEDA", 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Ingrese el nuevo saldo deseado y elija cómo aplicarlo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newBalanceText,
                        onValueChange = { 
                            val sanitized = it.replace(",", ".")
                            if (sanitized.isEmpty() || sanitized.toDoubleOrNull() != null) {
                                if (!sanitized.contains(".") || sanitized.substringAfter(".").length <= 2) {
                                    newBalanceText = sanitized
                                    viewModel.clearError()
                                }
                            }
                        },
                        label = { Text("Nuevo Saldo (${viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN")})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (uiState.errorMessage != null) {
                        Text(
                            uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val balance = newBalanceText.toDoubleOrNull()
                                if (balance != null && balance >= 0) {
                                    viewModel.adjustBalance(balance)
                                    showEditBalanceDialog = false
                                } else {
                                    viewModel.setError("Ingrese un monto válido")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("AJUSTAR SALDO")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                val balance = newBalanceText.toDoubleOrNull()
                                if (balance != null && balance >= 0) {
                                    viewModel.resetTransactions(balance)
                                    showEditBalanceDialog = false
                                } else {
                                    viewModel.setError("Ingrese un monto válido")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("REINICIAR BÓVEDA (BORRAR HISTORIAL)")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEditBalanceDialog = false }) {
                    Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showConversionDialog && selectedConversion != null) {
        ConversionDetailDialog(
            transaction = selectedConversion!!,
            wallet = uiState.wallets.find { it.id == selectedConversion!!.walletId },
            userEmail = uiState.currentUser?.email ?: "",
            onDismiss = {
                showConversionDialog = false
                selectedConversion = null
            }
        )
    }

    if (showTransactionDetailDialog && selectedTransactionDetail != null) {
        val tx = selectedTransactionDetail!!
        TransactionDetailDialog(
            transaction = tx,
            categoryIconName = selectedCategoryIcon,
            wallet = uiState.wallets.find { it.id == tx.walletId },
            userEmail = uiState.currentUser?.email ?: "",
            currencySymbol = viewModel.getCurrencySymbol(tx.currencyCode),
            onDismiss = {
                showTransactionDetailDialog = false
                selectedTransactionDetail = null
            },
            onViewVoucher = if (!tx.receiptPath.isNullOrBlank()) {
                { showVoucherDialog = true }
            } else null,
            auditLogs = uiState.auditLogs,
            isLoadingAuditLogs = uiState.isLoadingAuditLogs,
            onLoadAuditLogs = { viewModel.loadAuditLogs(tx.id) }
        )
    }

    if (showVoucherDialog && selectedTransactionDetail?.receiptPath != null
    ) {
        Dialog(
            onDismissRequest = {
                showVoucherDialog = false
            }
        ) {

            Surface(
                shape = RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    AsyncImage(
                        model = Uri.parse(
                            selectedTransactionDetail!!.receiptPath
                        ),
                        contentDescription = "Voucher",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showVoucherDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    if (showEditTransactionDialog && editingTransaction != null) {
        val filteredCategories = uiState.categories.filter { it.type == editingTransaction!!.type }
        
        Dialog(
            onDismissRequest = { showEditTransactionDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                val editDatePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = editDate.let {
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCal.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                        utcCal.set(Calendar.MILLISECOND, 0)
                        utcCal.timeInMillis
                    }
                )

                if (showEditDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showEditDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                editDatePickerState.selectedDateMillis?.let { utcMillis ->
                                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                        timeInMillis = utcMillis
                                    }
                                    val localCal = Calendar.getInstance().apply {
                                        set(
                                            utcCal.get(Calendar.YEAR),
                                            utcCal.get(Calendar.MONTH),
                                            utcCal.get(Calendar.DAY_OF_MONTH),
                                            0, 0, 0
                                        )
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    editDate = localCal
                                }
                                showEditDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditDatePicker = false }) { Text("Cancelar") }
                        }
                    ) {
                        DatePicker(state = editDatePickerState)
                    }
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("EDITAR TRANSACCIÓN") },
                            navigationIcon = {
                                IconButton(onClick = { showEditTransactionDialog = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 16.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Visualización del Monto
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("MONTO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp))
                                    val color = when(editingTransaction!!.type) {
                                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                        TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary
                                    }
                                    val symbol = viewModel.getCurrencySymbol(
                                        editingTransaction!!.currencyCode
                                    )

                                    Text(
                                        text = if (editAmount.isEmpty()) "$symbol 0.00" else "$symbol $editAmount",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = color
                                    )
                                }
                            }

                            // Descripción
                            OutlinedTextField(
                                value = editDescription,
                                onValueChange = { editDescription = it },
                                label = { Text("Descripción") },
                                placeholder = { Text("¿En qué se usó el dinero?") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )

                            // Comprobante
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                if (editReceiptPath != null) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = Uri.parse(editReceiptPath),
                                            contentDescription = "Comprobante",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                "Comprobante adjunto",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { editImagePickerLauncher.launch("image/*") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(32.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Cambiar", style = MaterialTheme.typography.labelSmall)
                                                }
                                                OutlinedButton(
                                                    onClick = { editReceiptPath = null },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(32.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                                ) {
                                                    Text("Eliminar", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    TextButton(
                                        onClick = { editImagePickerLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Adjuntar comprobante")
                                    }
                                }
                            }

                            // Fecha
                            val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
                            OutlinedTextField(
                                value = dateFormatter.format(editDate.time),
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Fecha") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().clickable { showEditDatePicker = true },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Listado de Categorías
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(filteredCategories, key = { it.id }) { category ->
                                        val isSelected = editCategory?.name == category.name
                                        Surface(
                                            onClick = { editCategory = category },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                                Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                    item {
                                        Surface(
                                            onClick = { 
                                                showEditTransactionDialog = false
                                                onNavigateToCategories()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Text("NUEVA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Panel Fijo
                        Column(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp, top = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = 2.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            NumericKeyboard(
                                onKeyPress = { key ->
                                    if (key == "." && editAmount.contains(".")) return@NumericKeyboard
                                    if (editAmount.contains(".") && editAmount.substringAfter(".").length >= 2) return@NumericKeyboard
                                    if (editAmount.length < 10) {
                                        editAmount += key
                                        viewModel.clearError()
                                    }
                                },
                                onDelete = { if (editAmount.isNotEmpty()) editAmount = editAmount.dropLast(1) },
                                onClear = { editAmount = "" }
                            )

                            Box(
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.errorMessage != null) {
                                    Text(
                                        uiState.errorMessage!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val amt = editAmount.toDoubleOrNull() ?: 0.0
                                    when {
                                        amt <= 0 -> viewModel.setError("El monto debe ser mayor a 0")
                                        editDescription.isBlank() -> viewModel.setError("La descripción es requerida")
                                        editCategory == null -> viewModel.setError("Debe seleccionar una categoría")
                                        else -> {
                                            val updated = editingTransaction!!.copy(
                                                amount = amt,
                                                description = editDescription,
                                                origin = editCategory!!.name,
                                                timestamp = editDate.timeInMillis,
                                                receiptPath = editReceiptPath
                                            )
                                            viewModel.updateTransaction(updated) {
                                                showEditTransactionDialog = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("ACTUALIZAR TRANSACCIÓN", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class ChartType { PIE, BAR }

data class ChartData(
    val label: String,
    val value: Float,
    val color: Color,
    val currencyCode: String
)

private val ChartColors = listOf(
    Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFD54F),
    Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFF4DB6AC),
    Color(0xFFFF8A65), Color(0xFF90A4AE), Color(0xFF7986CB),
    Color(0xFFF06292), Color(0xFF4DD0E1), Color(0xFFAED581)
)

@Composable
fun StatisticsSection(    transactions: List<Transaction>,
                          currencySymbol: String,
                          selectedWalletId: String?,
                          getCurrencySymbol: (String) -> String,
                          generalIncome: Double,
                          generalExpense: Double) {
    var viewMode by remember { mutableStateOf(0) } // 0: Gastos, 1: Ingresos, 2: General
    var chartType by remember { mutableStateOf(ChartType.PIE) }
    var selectedItem by remember { mutableStateOf<ChartData?>(null) }

    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val selectedWalletCurrency =
        transactions.firstOrNull()?.currencyCode ?: "PEN"

    val chartData = remember(transactions, viewMode, incomeColor, expenseColor, currencySymbol, selectedWalletId) {
        // Filtrar transacciones de la billetera actual.
        // Incluimos "Ajuste de Saldo" para que el gráfico cuadre con el dinero real, 
        // pero excluimos avisos técnicos como "Ajuste de Moneda" que no mueven dinero real.
        val filteredByWallet = transactions.filter { 
            it.walletId == selectedWalletId && 
            (it.origin != "Sistema" || it.description == "Ajuste de Saldo") &&
            it.origin != "Ajuste de Moneda"
        }


        when (viewMode) {
            0 -> { // Gastos
                filteredByWallet.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.origin.ifBlank { "Sin Categoría" } }
                    .mapValues { it.value.sumOf { t -> t.amount }.toFloat() }
                    .toList()
                    .sortedByDescending { it.second }
                    .mapIndexed { index, pair ->

                        val tx = filteredByWallet.firstOrNull {
                            it.origin == pair.first &&
                                    it.type == TransactionType.EXPENSE
                        }

                        ChartData(
                            label = pair.first,
                            value = pair.second,
                            color = ChartColors[index % ChartColors.size],
                            currencyCode = tx?.currencyCode ?: "PEN"
                        )
                    }
            }
            1 -> { // Ingresos
                filteredByWallet.filter { it.type == TransactionType.INCOME }
                    .groupBy { it.origin.ifBlank { "Sin Categoría" } }
                    .mapValues { it.value.sumOf { t -> t.amount }.toFloat() }
                    .toList()
                    .sortedByDescending { it.second }
                    .mapIndexed { index, pair ->

                        val tx = filteredByWallet.firstOrNull {
                            it.origin == pair.first &&
                                    it.type == TransactionType.INCOME
                        }

                        ChartData(
                            label = pair.first,
                            value = pair.second,
                            color = ChartColors[index % ChartColors.size],
                            currencyCode = tx?.currencyCode ?: "PEN"
                        )
                    }
            }
            else -> { // General (Ambos)

                listOf(
                    ChartData(
                        "Ingresos",
                        generalIncome.toFloat(),
                        incomeColor,
                        selectedWalletCurrency
                    ),
                    ChartData(
                        "Gastos",
                        generalExpense.toFloat(),
                        expenseColor,
                        selectedWalletCurrency
                    )
                ).filter { it.value > 0 }
            }
        }
    }

    val totalValue = chartData.sumOf { it.value.toDouble() }.toFloat()

    // Reset selection when data changes
    LaunchedEffect(viewMode) { selectedItem = null }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when(viewMode) {
                        0 -> "Análisis de Gastos"
                        1 -> "Análisis de Ingresos"
                        else -> "Balance General"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))) {
                    IconButton(onClick = { chartType = ChartType.PIE }) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = "Circular",
                            tint = if (chartType == ChartType.PIE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { chartType = ChartType.BAR }) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "Barras",
                            tint = if (chartType == ChartType.BAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewMode == 0,
                    onClick = { viewMode = 0 },
                    label = { Text("Gastos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.error
                    )
                )
                FilterChip(
                    selected = viewMode == 1,
                    onClick = { viewMode = 1 },
                    label = { Text("Ingresos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = viewMode == 2,
                    onClick = { viewMode = 2 },
                    label = { Text("General") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chartData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay transacciones para analizar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1.2f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (chartType == ChartType.PIE) {
                            PieChart(chartData, selectedItem, modifier = Modifier.size(140.dp))
                        } else {
                            BarChart(chartData, selectedItem, { selectedItem = it }, modifier = Modifier.fillMaxSize().padding(8.dp))
                        }
                        
                        if (chartType == ChartType.PIE) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedItem != null) selectedItem!!.label else "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "$currencySymbol ${String.format("%.2f", if (selectedItem != null) selectedItem!!.value else totalValue)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        chartData.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedItem = if (selectedItem == item) null else item }
                                    .background(
                                        if (selectedItem == item) item.color.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(item.color, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selectedItem == item) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (selectedItem == item) {
                                        Text(
                                            "${getCurrencySymbol(item.currencyCode)} ${String.format("%.2f", item.value)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.primary
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

// ── Premium helpers ────────────────────────────────────────────────────────────

private fun dashboardGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Buenos días,"
        hour < 18 -> "Buenas tardes,"
        else -> "Buenas noches,"
    }
}

@Composable
private fun WalletHeroCard(
    wallet: com.upn3.proyecto_finanzas_personales.model.Wallet,
    currencySymbol: String,
    onEditClick: () -> Unit,
    onBalanceEditClick: () -> Unit,
    onSelect: () -> Unit,
    incomeTotal: Double,
    expenseTotal: Double,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(wallet.color)
    val darkColor = remember(wallet.color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(wallet.color.toInt(), hsv)
        hsv[2] = (hsv[2] * 0.55f).coerceIn(0f, 1f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    Card(
        onClick = onSelect,
        modifier = modifier.height(190.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(baseColor, darkColor)))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(wallet.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                            Text(wallet.currencyCode, style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Row {
                        IconButton(onClick = onBalanceEditClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Settings, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Column {
                    Text("Saldo disponible", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        "$currencySymbol ${String.format("%.2f", wallet.balance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.ArrowUpward, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
                        Text("$currencySymbol ${String.format("%.2f", incomeTotal)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.ArrowDownward, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
                        Text("$currencySymbol ${String.format("%.2f", expenseTotal)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(icon: ImageVector, label: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
                Text(amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
private fun PremiumTransactionItem(
    transaction: Transaction,
    categories: List<com.upn3.proyecto_finanzas_personales.model.Category>,
    viewModel: com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val amountColor = when {
        transaction.type == TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        transaction.type == TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    val prefix = when {
        transaction.origin == "Sistema" || transaction.description.contains("Ajuste", ignoreCase = true) -> ""
        transaction.type == TransactionType.INCOME -> "+"
        transaction.type == TransactionType.EXPENSE -> "-"
        transaction.type == TransactionType.TRANSFER -> "⇄ "
        else -> ""
    }
    val symbol = viewModel.getCurrencySymbol(transaction.currencyCode)

    val dateStr = remember(transaction.timestamp) {
        val now = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp))
        when {
            txCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> "Hoy · $time"
            txCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 && txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> "Ayer · $time"
            else -> java.text.SimpleDateFormat("dd/MM/yyyy · HH:mm", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp))
        }
    }

    Surface(
        onClick = onItemClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).background(amountColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (transaction.type) {
                        TransactionType.INCOME -> Icons.Default.ArrowUpward
                        TransactionType.EXPENSE -> Icons.Default.ArrowDownward
                        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                    },
                    null, tint = amountColor, modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(transaction.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(transaction.origin, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (!transaction.receiptPath.isNullOrBlank()) {
                        Icon(Icons.Default.Receipt, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                    if (transaction.latitude != null) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                    }
                }
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$prefix$symbol${String.format("%.2f", transaction.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = amountColor)
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                            onClick = { showMenu = false; onEditClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDeleteClick() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(data: List<ChartData>, selectedItem: ChartData?, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        data.forEach { item ->
            val sweepAngle = if (total > 0) (item.value / total) * 360f else 0f
            val isSelected = item == selectedItem
            
            drawArc(
                color = item.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = if (isSelected) 35.dp.toPx() else 25.dp.toPx())
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun BarChart(data: List<ChartData>, selectedItem: ChartData?, onItemSelected: (ChartData) -> Unit, modifier: Modifier = Modifier) {
    val maxValue = data.maxOfOrNull { it.value } ?: 1f
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.take(5).forEach { item ->
            val isSelected = item == selectedItem
            // Calculamos la altura relativa (mínimo 10% para que se vea algo si el valor es muy bajo)
            val barHeightFraction = if (maxValue > 0) (item.value / maxValue).coerceIn(0.1f, 1f) else 0.1f
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onItemSelected(item) }
            ) {
                // Contenedor de la barra para dar espacio y centrar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f) // Barra un poco más delgada para mejor estética
                            .fillMaxHeight(barHeightFraction)
                            .background(
                                if (isSelected) item.color else item.color.copy(alpha = 0.4f),
                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

