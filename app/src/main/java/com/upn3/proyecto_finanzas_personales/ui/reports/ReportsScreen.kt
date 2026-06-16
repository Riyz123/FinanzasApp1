package com.upn3.proyecto_finanzas_personales.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.upn3.proyecto_finanzas_personales.model.Transaction
import com.upn3.proyecto_finanzas_personales.model.TransactionType
import com.upn3.proyecto_finanzas_personales.ui.components.ConversionDetailDialog
import com.upn3.proyecto_finanzas_personales.ui.components.TransactionDetailDialog
import com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var reportType by remember { mutableStateOf(ReportType.DAILY) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    // null = billetera actualmente seleccionada; "all" = todas las billeteras
    var reportWalletFilter by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTransactionDetail by remember { mutableStateOf<Transaction?>(null) }
    var showTransactionDetailDialog by remember { mutableStateOf(false) }
    var selectedConversion by remember { mutableStateOf<Transaction?>(null) }
    var showConversionDialog by remember { mutableStateOf(false) }
    var selectedCategoryIcon by remember { mutableStateOf("Category") }

    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.let {
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCal.set(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            utcCal.set(Calendar.MILLISECOND, 0)
            utcCal.timeInMillis
        }
    )

    val dateRangePickerState = rememberDateRangePickerState()

    // ── Report data (hoisted so launchers can capture it) ──────────────────────
    val isAllWallets = reportWalletFilter == "all"
    val reportData = remember(uiState.transactions, uiState.convertedTransactions, uiState.allWalletTransactions, reportType, selectedDate, customStartDate, customEndDate, reportWalletFilter) {
        val (start, end) = getPeriodBounds(reportType, selectedDate, customStartDate, customEndDate)

        val rawSource = when {
            isAllWallets -> uiState.allWalletTransactions
            reportWalletFilter != null -> uiState.allWalletTransactions.filter { it.walletId == reportWalletFilter }
            else -> uiState.transactions
        }
        val displayList = rawSource.filter {
            it.timestamp in start..end &&
            (it.origin != "Sistema" || it.description.contains("Ajuste", ignoreCase = true))
        }.sortedByDescending { it.timestamp }

        val sourceForTotals = when {
            isAllWallets || reportWalletFilter != null -> rawSource
            else -> uiState.convertedTransactions.ifEmpty { uiState.transactions }
        }
        var income = 0.0; var expense = 0.0; var transfer = 0.0
        sourceForTotals.filter { tx ->
            tx.timestamp in start..end &&
            (tx.origin != "Sistema" || tx.description.contains("Ajuste", ignoreCase = true))
        }.forEach { tx ->
            when (tx.type) {
                TransactionType.INCOME   -> income += tx.amount
                TransactionType.EXPENSE  -> expense += tx.amount
                TransactionType.TRANSFER -> {
                    val desc = tx.description.lowercase()
                    if (desc.startsWith("de ") || desc.startsWith("transferencia de ")) income += tx.amount
                    else expense += tx.amount
                }
            }
        }
        Triple(displayList, Pair(income, expense), transfer)
    }
    val sortedTransactions = reportData.first
    val (totalIncome, totalExpense) = reportData.second
    val totalTransfer = reportData.third
    val reportWalletName = when {
        isAllWallets -> "Todas las billeteras"
        reportWalletFilter != null -> uiState.wallets.find { it.id == reportWalletFilter }?.name ?: uiState.selectedWallet?.name ?: ""
        else -> uiState.selectedWallet?.name ?: ""
    }
    val currentSymbol = if (isAllWallets) "" else viewModel.getCurrencySymbol(
        (if (reportWalletFilter != null) uiState.wallets.find { it.id == reportWalletFilter } else uiState.selectedWallet)?.currencyCode ?: "PEN"
    )
    val reportTitle = when(reportType) {
        ReportType.DAILY   -> "Reporte Diario"
        ReportType.MONTHLY -> "Reporte Mensual"
        ReportType.YEARLY  -> "Reporte Anual"
        ReportType.CUSTOM  -> "Reporte Personalizado"
    }
    val reportDateText = getReportDateRangeText(reportType, selectedDate, customStartDate, customEndDate)
    val exportTitle = "$reportTitle${if (reportWalletName.isNotBlank()) " — $reportWalletName" else ""}"
    val exportFileName = remember(reportType, selectedDate) {
        "reporte_finanzas_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"
    }

    // Capture mutable state for launchers (closures re-evaluated each recomposition via rememberUpdatedState)
    val latestSortedTx by rememberUpdatedState(sortedTransactions)
    val latestIncome by rememberUpdatedState(totalIncome)
    val latestExpense by rememberUpdatedState(totalExpense)
    val latestTransfer by rememberUpdatedState(totalTransfer)
    val latestSymbol by rememberUpdatedState(currentSymbol)
    val latestTitle by rememberUpdatedState(exportTitle)
    val latestDateText by rememberUpdatedState(reportDateText)

    // ── Export launchers ───────────────────────────────────────────────────────
    val createPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.exportReportToPdf(uri, latestTitle, latestDateText, latestSortedTx,
            latestIncome, latestExpense, latestTransfer, latestSymbol, context)
    }
    val createCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.exportReportToCsv(uri, latestTitle, latestDateText, latestSortedTx,
            latestIncome, latestExpense, latestTransfer, latestSymbol, context)
    }

    LaunchedEffect(uiState.errorMessage) {
        when (uiState.errorMessage) {
            "REPORT_PDF_OK" -> {
                snackbarHostState.showSnackbar("PDF exportado correctamente")
                viewModel.clearError()
            }
            "REPORT_CSV_OK" -> {
                snackbarHostState.showSnackbar("CSV exportado — ábrelo con Excel o Google Sheets")
                viewModel.clearError()
            }
            else -> if (uiState.errorMessage?.startsWith("Error al generar") == true) {
                snackbarHostState.showSnackbar(uiState.errorMessage!!)
                viewModel.clearError()
            }
        }
    }

    // Resetear scroll al inicio al cambiar billetera o periodo (sin animación para evitar flicker)
    LaunchedEffect(reportWalletFilter, reportType) {
        listState.scrollToItem(0)
    }

    // ── Export format dialog ───────────────────────────────────────────────────
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = { Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Exportar Reporte", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Selecciona el formato para: $reportDateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        onClick = { showExportDialog = false; createPdfLauncher.launch("$exportFileName.pdf") },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(0.15f), modifier = Modifier.size(40.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                                }
                            }
                            Column {
                                Text("PDF", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Documento formateado, listo para imprimir o compartir",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Surface(
                        onClick = { showExportDialog = false; createCsvLauncher.launch("$exportFileName.csv") },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(0.15f), modifier = Modifier.size(40.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.GridOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Column {
                                Text("Excel / CSV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Tabla editable, compatible con Excel y Google Sheets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val startUtc = dateRangePickerState.selectedStartDateMillis
                    val endUtc = dateRangePickerState.selectedEndDateMillis ?: startUtc
                    
                    if (startUtc != null) {
                        // Convertir UTC a Local para el inicio (00:00:00)
                        val startCalUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startUtc }
                        val startCalLocal = Calendar.getInstance().apply {
                            set(startCalUtc.get(Calendar.YEAR), startCalUtc.get(Calendar.MONTH), startCalUtc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        
                        // Convertir UTC a Local para el fin (23:59:59)
                        val endCalUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = endUtc!! }
                        val endCalLocal = Calendar.getInstance().apply {
                            set(endCalUtc.get(Calendar.YEAR), endCalUtc.get(Calendar.MONTH), endCalUtc.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        
                        customStartDate = startCalLocal.timeInMillis
                        customEndDate = endCalLocal.timeInMillis
                    }
                    showDateRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Cancelar") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Seleccionar rango", modifier = Modifier.padding(16.dp)) },
                headline = { Text("Periodo de reporte", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth().height(500.dp)
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
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
                        selectedDate = localCal
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Month picker dialog ────────────────────────────────────────────────────
    if (showMonthPicker) {
        var pickerYear by remember { mutableStateOf(selectedDate.get(Calendar.YEAR)) }
        val monthNames = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")
        val monthRows = listOf(0..2, 3..5, 6..8, 9..11)
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("Seleccionar mes", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { pickerYear-- }) {
                            Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("$pickerYear", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { pickerYear++ }) {
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    monthRows.forEach { range ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            range.forEach { monthIdx ->
                                val isSelected = pickerYear == selectedDate.get(Calendar.YEAR) &&
                                        monthIdx == selectedDate.get(Calendar.MONTH)
                                Surface(
                                    onClick = {
                                        selectedDate = Calendar.getInstance().also { c ->
                                            c.set(Calendar.YEAR, pickerYear)
                                            c.set(Calendar.MONTH, monthIdx)
                                            c.set(Calendar.DAY_OF_MONTH, 1)
                                        }
                                        showMonthPicker = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            monthNames[monthIdx],
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMonthPicker = false }) { Text("Cancelar") } }
        )
    }

    // ── Year picker dialog ─────────────────────────────────────────────────────
    if (showYearPicker) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear + 1 downTo currentYear - 10).toList()
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            title = { Text("Seleccionar año", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    years.forEach { year ->
                        val isSelected = selectedDate.get(Calendar.YEAR) == year
                        Surface(
                            onClick = {
                                selectedDate = Calendar.getInstance().also { c ->
                                    c.set(Calendar.YEAR, year)
                                    c.set(Calendar.MONTH, Calendar.JANUARY)
                                    c.set(Calendar.DAY_OF_MONTH, 1)
                                }
                                showYearPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "$year",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                if (year == currentYear) {
                                    Text(
                                        "(año actual)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showYearPicker = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reportes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar reporte")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Date Selector ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ◀ Previous period
                IconButton(
                    onClick = {
                        selectedDate = (selectedDate.clone() as Calendar).also { c ->
                            when (reportType) {
                                ReportType.DAILY   -> c.add(Calendar.DAY_OF_MONTH, -1)
                                ReportType.MONTHLY -> c.add(Calendar.MONTH, -1)
                                ReportType.YEARLY  -> c.add(Calendar.YEAR, -1)
                                ReportType.CUSTOM  -> {}
                            }
                        }
                    },
                    enabled = reportType != ReportType.CUSTOM
                ) {
                    Icon(
                        Icons.Default.ChevronLeft, null,
                        tint = if (reportType != ReportType.CUSTOM) MaterialTheme.colorScheme.primary
                               else Color.Transparent
                    )
                }

                // Type pill + dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { showTypeMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text(
                                        when (reportType) {
                                            ReportType.DAILY   -> "DIARIO"
                                            ReportType.MONTHLY -> "MENSUAL"
                                            ReportType.YEARLY  -> "ANUAL"
                                            ReportType.CUSTOM  -> "PERSONALIZADO"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        getReportDateRangeText(reportType, selectedDate, customStartDate, customEndDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Icon(Icons.Default.UnfoldMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Por Día") },
                            leadingIcon = { Icon(Icons.Default.Today, null) },
                            onClick = {
                                reportType = ReportType.DAILY
                                showTypeMenu = false
                                showDatePicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Por Mes") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            onClick = {
                                reportType = ReportType.MONTHLY
                                showTypeMenu = false
                                showMonthPicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Por Año") },
                            leadingIcon = { Icon(Icons.Default.Event, null) },
                            onClick = {
                                reportType = ReportType.YEARLY
                                showTypeMenu = false
                                showYearPicker = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Rango Personalizado") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) },
                            onClick = {
                                reportType = ReportType.CUSTOM
                                showTypeMenu = false
                                showDateRangePicker = true
                            }
                        )
                    }
                }

                // ▶ Next period
                IconButton(
                    onClick = {
                        selectedDate = (selectedDate.clone() as Calendar).also { c ->
                            when (reportType) {
                                ReportType.DAILY   -> c.add(Calendar.DAY_OF_MONTH, 1)
                                ReportType.MONTHLY -> c.add(Calendar.MONTH, 1)
                                ReportType.YEARLY  -> c.add(Calendar.YEAR, 1)
                                ReportType.CUSTOM  -> {}
                            }
                        }
                    },
                    enabled = reportType != ReportType.CUSTOM
                ) {
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = if (reportType != ReportType.CUSTOM) MaterialTheme.colorScheme.primary
                               else Color.Transparent
                    )
                }
            }

            // ── Selector de billetera ─────────────────────────────────────────
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = reportWalletFilter == "all",
                        onClick = { reportWalletFilter = "all" },
                        label = { Text("Todas las billeteras") },
                        leadingIcon = {
                            if (reportWalletFilter == "all") Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                items(uiState.wallets) { w ->
                    val isSelected = reportWalletFilter == null && w.id == uiState.selectedWallet?.id ||
                                     reportWalletFilter == w.id
                    FilterChip(
                        selected = isSelected,
                        // Sólo filtro local — sin selectWallet() para no disparar isLoading
                        onClick = {
                            reportWalletFilter = if (w.id == uiState.selectedWallet?.id) null else w.id
                        },
                        label = { Text(w.name) }
                    )
                }
            }

            // ── Tarjetas resumen con crossfade al cambiar billetera ───────────
            // La key combina el filtro + los totales para animar cuando cambian los datos
            AnimatedContent(
                targetState = Triple(reportWalletFilter, totalIncome, totalExpense),
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(100))
                },
                label = "report-summary"
            ) { (_, animIncome, animExpense) ->
                val netBalance = animIncome - animExpense
                val netPct = if (animIncome > 0) (netBalance / animIncome).toFloat().coerceIn(0f, 1f) else 0f
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportSummaryCard(label = "Ingresos", amount = animIncome, color = MaterialTheme.colorScheme.primary, symbol = currentSymbol, icon = Icons.AutoMirrored.Filled.TrendingUp, modifier = Modifier.weight(1f))
                        ReportSummaryCard(label = "Gastos", amount = animExpense, color = MaterialTheme.colorScheme.error, symbol = currentSymbol, icon = Icons.AutoMirrored.Filled.TrendingDown, modifier = Modifier.weight(1f))
                    }
                    if (totalTransfer > 0) {
                        Spacer(Modifier.height(8.dp))
                        ReportSummaryCard(label = "Transferencias", amount = totalTransfer, color = MaterialTheme.colorScheme.secondary, symbol = currentSymbol, icon = Icons.Default.SwapHoriz, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Balance neto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${if (netBalance >= 0) "+" else ""}$currentSymbol ${String.format("%.2f", netBalance)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            LinearProgressIndicator(
                                progress = { netPct },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // ── Transactions list ─────────────────────────────────────────────
            Text(
                "Detalle de Movimientos",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = sortedTransactions,
                    key = { it.id } // Usar ID para optimizar recomposición
                ) { transaction ->
                    val isConversion = transaction.origin == "Ajuste de Moneda"
                    TransactionReportItem(
                        transaction = transaction,
                        currency = viewModel.getCurrencySymbol(transaction.currencyCode),
                        onClick = {
                            if (isConversion) {
                                selectedConversion = transaction
                                showConversionDialog = true
                            } else {
                                selectedCategoryIcon = uiState.categories.find { it.name == transaction.origin }?.icon ?: "Category"
                                selectedTransactionDetail = transaction
                                showTransactionDetailDialog = true
                            }
                        }
                    )
                }
                
                if (sortedTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay transacciones en este periodo", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
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
            onViewVoucher = null,
            auditLogs = uiState.auditLogs,
            isLoadingAuditLogs = uiState.isLoadingAuditLogs,
            onLoadAuditLogs = { viewModel.loadAuditLogs(tx.id) }
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
}

@Composable
fun ReportTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) }
    )
}

@Composable
fun ReportSummaryCard(label: String, amount: Double, color: Color, symbol: String, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.BarChart, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
                Text("$symbol ${String.format("%.2f", amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
fun TransactionReportItem(transaction: Transaction, currency: String, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd/MM · HH:mm", Locale.getDefault()) }
    val isConversion = transaction.origin == "Ajuste de Moneda"
    val color = when {
        isConversion -> androidx.compose.ui.graphics.Color(0xFF7C3AED)
        transaction.type == TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        transaction.type == TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    val prefix = when {
        isConversion -> ""
        transaction.origin == "Sistema" || transaction.description.contains("Ajuste", ignoreCase = true) -> ""
        transaction.type == TransactionType.INCOME -> "+"
        transaction.type == TransactionType.EXPENSE -> "-"
        transaction.type == TransactionType.TRANSFER -> "⇄ "
        else -> ""
    }
    val icon = when {
        isConversion -> Icons.Default.SwapHoriz
        transaction.type == TransactionType.INCOME -> Icons.Default.ArrowUpward
        transaction.type == TransactionType.EXPENSE -> Icons.Default.ArrowDownward
        else -> Icons.Default.SwapHoriz
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.12f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(transaction.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("${transaction.origin} · ${fmt.format(Date(transaction.timestamp))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$prefix$currency ${String.format("%.2f", transaction.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

enum class ReportType { DAILY, MONTHLY, YEARLY, CUSTOM }

fun getReportDateRangeText(type: ReportType, cal: Calendar, customStart: Long? = null, customEnd: Long? = null): String {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val monthF = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val yearF = SimpleDateFormat("yyyy", Locale.getDefault())
    
    return when (type) {
        ReportType.DAILY   -> df.format(cal.time)
        ReportType.MONTHLY -> monthF.format(cal.time).replaceFirstChar { it.uppercase() }
        ReportType.YEARLY  -> "Año ${yearF.format(cal.time)}"
        ReportType.CUSTOM  -> {
            if (customStart != null && customEnd != null) {
                "${df.format(Date(customStart))} - ${df.format(Date(customEnd))}"
            } else {
                "Seleccionar rango"
            }
        }
    }
}

fun getPeriodBounds(
    type: ReportType, 
    selectedCal: Calendar, 
    customStart: Long?, 
    customEnd: Long?
): Pair<Long, Long> {
    val cal = selectedCal.clone() as Calendar
    cal.set(Calendar.MILLISECOND, 0)
    
    return when (type) {
        ReportType.DAILY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            Pair(start, cal.timeInMillis)
        }
        ReportType.MONTHLY -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            Pair(start, cal.timeInMillis)
        }
        ReportType.YEARLY -> {
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            Pair(start, cal.timeInMillis)
        }
        ReportType.CUSTOM -> {
            Pair(customStart ?: 0L, customEnd ?: Long.MAX_VALUE)
        }
    }
}
