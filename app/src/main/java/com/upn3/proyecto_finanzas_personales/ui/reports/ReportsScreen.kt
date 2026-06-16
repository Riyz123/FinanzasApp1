package com.upn3.proyecto_finanzas_personales.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upn3.proyecto_finanzas_personales.model.Transaction
import com.upn3.proyecto_finanzas_personales.model.TransactionType
import com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reportes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text(
                                    when(reportType) {
                                        ReportType.DAILY -> "DIARIO"
                                        ReportType.WEEKLY -> "SEMANAL"
                                        ReportType.MONTHLY -> "MENSUAL"
                                        ReportType.YEARLY -> "ANUAL"
                                        ReportType.CUSTOM -> "PERSONALIZADO"
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
                        text = { Text("Por Semana") },
                        leadingIcon = { Icon(Icons.Default.DateRange, null) },
                        onClick = {
                            reportType = ReportType.WEEKLY
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
                            showDatePicker = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Por Año") },
                        leadingIcon = { Icon(Icons.Default.Event, null) },
                        onClick = {
                            reportType = ReportType.YEARLY
                            showTypeMenu = false
                            showDatePicker = true
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

            // Summary Totals
            val reportData = remember(uiState.transactions, reportType, selectedDate, customStartDate, customEndDate) {
                val (start, end) = getPeriodBounds(reportType, selectedDate, customStartDate, customEndDate)
                // Incluir transacciones normales y ajustes de saldo, pero ignorar otros eventos de sistema
                val filtered = uiState.transactions.filter { 
                    it.timestamp in start..end && 
                    (it.origin != "Sistema" || it.description == "Ajuste de Saldo") &&
                    it.origin != "Ajuste de Moneda"
                }
                val sorted = filtered.sortedByDescending { it.timestamp }
                
                var income = 0.0
                var expense = 0.0
                var transfer = 0.0
                
                filtered.forEach { 
                    when(it.type) {
                        TransactionType.INCOME -> income += it.amount
                        TransactionType.EXPENSE -> expense += it.amount
                        TransactionType.TRANSFER -> transfer += it.amount
                    }
                }
                
                Triple(sorted, Pair(income, expense), transfer)
            }

            val sortedTransactions = reportData.first
            val (totalIncome, totalExpense) = reportData.second
            val totalTransfer = reportData.third
            val currentSymbol = viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN")

            // ── Balance neto ──────────────────────────────────────────────────
            val netBalance = totalIncome - totalExpense
            val netPct = if (totalIncome > 0) (netBalance / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportSummaryCard(label = "Ingresos", amount = totalIncome, color = MaterialTheme.colorScheme.primary, symbol = currentSymbol, icon = Icons.AutoMirrored.Filled.TrendingUp, modifier = Modifier.weight(1f))
                    ReportSummaryCard(label = "Gastos", amount = totalExpense, color = MaterialTheme.colorScheme.error, symbol = currentSymbol, icon = Icons.AutoMirrored.Filled.TrendingDown, modifier = Modifier.weight(1f))
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

            // ── Transactions list ─────────────────────────────────────────────
            Text(
                "Detalle de Movimientos",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = sortedTransactions,
                    key = { it.id } // Usar ID para optimizar recomposición
                ) { transaction ->
                    TransactionReportItem(transaction, viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN"))
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
fun TransactionReportItem(transaction: Transaction, currency: String) {
    val fmt = remember { SimpleDateFormat("dd/MM · HH:mm", Locale.getDefault()) }
    val color = when(transaction.type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary
    }
    val prefix = when {
        transaction.origin == "Sistema" || transaction.description.contains("Ajuste", ignoreCase = true) -> ""
        transaction.type == TransactionType.INCOME -> "+"
        transaction.type == TransactionType.EXPENSE -> "-"
        transaction.type == TransactionType.TRANSFER -> "⇄ "
        else -> ""
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.12f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(when(transaction.type) {
                    TransactionType.INCOME -> Icons.Default.ArrowUpward
                    TransactionType.EXPENSE -> Icons.Default.ArrowDownward
                    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                }, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(transaction.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("${transaction.origin} · ${fmt.format(Date(transaction.timestamp))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$prefix$currency ${String.format("%.2f", transaction.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

enum class ReportType { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

fun getReportDateRangeText(type: ReportType, cal: Calendar, customStart: Long? = null, customEnd: Long? = null): String {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val monthF = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val yearF = SimpleDateFormat("yyyy", Locale.getDefault())
    
    return when (type) {
        ReportType.DAILY -> df.format(cal.time)
        ReportType.WEEKLY -> {
            val start = cal.clone() as Calendar
            start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
            val end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_WEEK, 6)
            "${df.format(start.time)} - ${df.format(end.time)}"
        }
        ReportType.MONTHLY -> monthF.format(cal.time).replaceFirstChar { it.uppercase() }
        ReportType.YEARLY -> "Año ${yearF.format(cal.time)}"
        ReportType.CUSTOM -> {
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
        ReportType.WEEKLY -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 6)
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
