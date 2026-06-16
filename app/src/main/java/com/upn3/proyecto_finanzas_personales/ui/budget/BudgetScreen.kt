package com.upn3.proyecto_finanzas_personales.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upn3.proyecto_finanzas_personales.model.*
import com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddBudget by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddFixed by remember { mutableStateOf(false) }
    var depositGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    LaunchedEffect(Unit) { viewModel.loadPlanningData() }

    val tabs = listOf(
        Triple("Presupuestos", Icons.Default.PieChart, 0),
        Triple("Metas", Icons.Default.Flag, 1),
        Triple("Gastos Fijos", Icons.Default.Repeat, 2)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Presupuestos & Metas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showAddBudget = true
                        1 -> showAddGoal = true
                        2 -> showAddFixed = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEach { (label, icon, idx) ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, fontSize = 12.sp) },
                        icon = { Icon(icon, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            if (uiState.isLoadingBudgets) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> BudgetsTab(
                        budgets = uiState.budgets.filter { it.walletId == uiState.selectedWallet?.id },
                        transactions = uiState.transactions,
                        symbol = viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN"),
                        onDelete = { viewModel.deleteBudget(it) }
                    )
                    1 -> GoalsTab(
                        goals = uiState.savingsGoals,
                        symbol = viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN"),
                        onDeposit = { depositGoal = it },
                        onDelete = { viewModel.deleteSavingsGoal(it) }
                    )
                    2 -> FixedTab(
                        expenses = uiState.fixedExpenses.filter { it.walletId == uiState.selectedWallet?.id },
                        symbol = viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN"),
                        onToggle = { id, active -> viewModel.toggleFixedExpense(id, active) },
                        onDelete = { viewModel.deleteFixedExpense(it) }
                    )
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showAddBudget) {
        AddBudgetDialog(
            categories = uiState.categories.filter { it.type == TransactionType.EXPENSE },
            walletId = uiState.selectedWallet?.id ?: "",
            currencyCode = uiState.selectedWallet?.currencyCode ?: "PEN",
            onAdd = { budget -> viewModel.addBudget(budget); showAddBudget = false },
            onDismiss = { showAddBudget = false }
        )
    }

    if (showAddGoal) {
        AddGoalDialog(
            currencyCode = uiState.selectedWallet?.currencyCode ?: "PEN",
            onAdd = { goal -> viewModel.addSavingsGoal(goal); showAddGoal = false },
            onDismiss = { showAddGoal = false }
        )
    }

    if (showAddFixed) {
        AddFixedDialog(
            categories = uiState.categories.filter { it.type == TransactionType.EXPENSE },
            walletId = uiState.selectedWallet?.id ?: "",
            currencyCode = uiState.selectedWallet?.currencyCode ?: "PEN",
            onAdd = { expense -> viewModel.addFixedExpense(expense); showAddFixed = false },
            onDismiss = { showAddFixed = false }
        )
    }

    depositGoal?.let { goal ->
        DepositDialog(
            goal = goal,
            symbol = viewModel.getCurrencySymbol(goal.currencyCode),
            onDeposit = { amount -> viewModel.addDepositToGoal(goal.id, amount); depositGoal = null },
            onDismiss = { depositGoal = null }
        )
    }
}

// ── Tab 0: Presupuestos ────────────────────────────────────────────────────────

@Composable
private fun BudgetsTab(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    symbol: String,
    onDelete: (String) -> Unit
) {
    if (budgets.isEmpty()) {
        PlanningEmptyState(
            icon = Icons.Default.PieChart,
            message = "Sin presupuestos activos",
            subtitle = "Toca + para crear un límite de gasto por categoría"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(budgets, key = { it.id }) { budget ->
                val spent = calcBudgetSpent(budget, transactions)
                BudgetCard(budget, spent, symbol, onDelete)
            }
        }
    }
}

@Composable
private fun BudgetCard(budget: Budget, spent: Double, symbol: String, onDelete: (String) -> Unit) {
    val progress = (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f)
    val overBudget = spent > budget.limitAmount
    val progressColor = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.8f -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }
    val periodLabel = if (budget.period == "MONTHLY") "MENSUAL" else "SEMANAL"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = progressColor.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Category, null, tint = progressColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column {
                        Text(budget.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(periodLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (overBudget) Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    IconButton(onClick = { onDelete(budget.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "$symbol ${String.format("%.2f", spent)} gastado",
                    style = MaterialTheme.typography.labelSmall,
                    color = progressColor
                )
                Text(
                    "límite $symbol ${String.format("%.2f", budget.limitAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            Text(
                "${(progress * 100).toInt()}% utilizado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calcBudgetSpent(budget: Budget, transactions: List<Transaction>): Double {
    val start = when (budget.period) {
        "MONTHLY" -> Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        else -> Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    return transactions.filter {
        it.type == TransactionType.EXPENSE &&
        it.origin.equals(budget.categoryName, ignoreCase = true) &&
        it.timestamp >= start
    }.sumOf { it.amount }
}

// ── Tab 1: Metas de Ahorro ─────────────────────────────────────────────────────

@Composable
private fun GoalsTab(
    goals: List<SavingsGoal>,
    symbol: String,
    onDeposit: (SavingsGoal) -> Unit,
    onDelete: (String) -> Unit
) {
    if (goals.isEmpty()) {
        PlanningEmptyState(
            icon = Icons.Default.Flag,
            message = "Sin metas de ahorro",
            subtitle = "Toca + para crear una meta con progreso de ahorro"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(goals, key = { it.id }) { goal ->
                GoalCard(goal, symbol, onDeposit, onDelete)
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: SavingsGoal,
    symbol: String,
    onDeposit: (SavingsGoal) -> Unit,
    onDelete: (String) -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val completed = goal.currentAmount >= goal.targetAmount
    val goalColor = try { Color(android.graphics.Color.parseColor(goal.color)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = goalColor.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                if (completed) Icons.Default.CheckCircle else Icons.Default.Flag,
                                null, tint = goalColor, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(goal.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (goal.deadline > 0) {
                            Text(
                                "Vence: ${dateFormat.format(Date(goal.deadline))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = { onDelete(goal.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "$symbol ${String.format("%.2f", goal.currentAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = goalColor
                )
                Text(
                    "de $symbol ${String.format("%.2f", goal.targetAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = goalColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${(progress * 100).toInt()}% completado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!completed) {
                    TextButton(onClick = { onDeposit(goal) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Depositar", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                        Text("¡Completada!", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Tab 2: Gastos Fijos ────────────────────────────────────────────────────────

@Composable
private fun FixedTab(
    expenses: List<FixedExpense>,
    symbol: String,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    if (expenses.isEmpty()) {
        PlanningEmptyState(
            icon = Icons.Default.Repeat,
            message = "Sin gastos fijos registrados",
            subtitle = "Toca + para registrar gastos recurrentes mensuales"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(expenses, key = { it.id }) { expense ->
                FixedCard(expense, symbol, onToggle, onDelete)
            }
        }
    }
}

@Composable
private fun FixedCard(
    expense: FixedExpense,
    symbol: String,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    val color = if (expense.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Repeat, null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(expense.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${expense.categoryName}  •  día ${expense.dayOfMonth} de cada mes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$symbol ${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Switch(
                checked = expense.isActive,
                onCheckedChange = { onToggle(expense.id, it) },
                modifier = Modifier.size(width = 44.dp, height = 24.dp)
            )
            IconButton(onClick = { onDelete(expense.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────

@Composable
private fun PlanningEmptyState(icon: ImageVector, message: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), modifier = Modifier.size(72.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }
            Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ── Dialog: Agregar Presupuesto ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    categories: List<Category>,
    walletId: String,
    currencyCode: String,
    onAdd: (Budget) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "") }
    var amountText by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("MONTHLY") }
    var showCatMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PieChart, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Nuevo Presupuesto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Categoría") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Surface(
                        onClick = { showCatMenu = true },
                        modifier = Modifier.matchParentSize(),
                        color = Color.Transparent
                    ) {}
                    DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selectedCategory = cat.name; showCatMenu = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Límite de gasto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Período", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = period == "MONTHLY",
                        onClick = { period = "MONTHLY" },
                        label = { Text("Mensual") }
                    )
                    FilterChip(
                        selected = period == "WEEKLY",
                        onClick = { period = "WEEKLY" },
                        label = { Text("Semanal") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    if (selectedCategory.isBlank() || amount <= 0) return@Button
                    onAdd(Budget(walletId = walletId, categoryName = selectedCategory, limitAmount = amount, period = period, currencyCode = currencyCode))
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Dialog: Agregar Meta ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    currencyCode: String,
    onAdd: (SavingsGoal) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("0") }
    var hasDeadline by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var deadlineMs by remember { mutableStateOf(0L) }
    val datePickerState = rememberDatePickerState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val goalColors = listOf("#4338CA", "#065F46", "#7F1D1D", "#C2410C", "#6B21A8", "#0369A1")
    var selectedColor by remember { mutableStateOf(goalColors.first()) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMs ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
                        val localCal = Calendar.getInstance().apply {
                            set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
                        }
                        deadlineMs = localCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Nueva Meta de Ahorro", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la meta") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto objetivo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Ahorro inicial (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Fecha límite", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = hasDeadline, onCheckedChange = { hasDeadline = it; if (it) showDatePicker = true })
                }
                if (hasDeadline && deadlineMs > 0) {
                    Text(
                        "Vence: ${dateFormat.format(Date(deadlineMs))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goalColors.forEach { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        Surface(
                            onClick = { selectedColor = hex },
                            shape = CircleShape,
                            color = c,
                            modifier = Modifier.size(28.dp),
                            border = if (selectedColor == hex) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: return@Button
                    val initial = initialText.toDoubleOrNull() ?: 0.0
                    if (name.isBlank() || target <= 0) return@Button
                    onAdd(SavingsGoal(
                        name = name.trim(),
                        targetAmount = target,
                        currentAmount = initial.coerceAtMost(target),
                        deadline = if (hasDeadline) deadlineMs else 0L,
                        currencyCode = currencyCode,
                        color = selectedColor
                    ))
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Dialog: Depositar en Meta ──────────────────────────────────────────────────

@Composable
private fun DepositDialog(
    goal: SavingsGoal,
    symbol: String,
    onDeposit: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val remaining = goal.targetAmount - goal.currentAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Savings, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Depositar en \"${goal.name}\"", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Falta $symbol ${String.format("%.2f", remaining)} para completar la meta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto a depositar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    if (amount <= 0) return@Button
                    onDeposit(amount)
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Depositar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Dialog: Agregar Gasto Fijo ─────────────────────────────────────────────────

@Composable
private fun AddFixedDialog(
    categories: List<Category>,
    walletId: String,
    currencyCode: String,
    onAdd: (FixedExpense) -> Unit,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "") }
    var dayText by remember { mutableStateOf("1") }
    var showCatMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Repeat, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Nuevo Gasto Fijo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (ej. Alquiler)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto mensual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Box {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Categoría") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Surface(
                        onClick = { showCatMenu = true },
                        modifier = Modifier.matchParentSize(),
                        color = Color.Transparent
                    ) {}
                    DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selectedCategory = cat.name; showCatMenu = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dayText,
                    onValueChange = { v ->
                        val n = v.filter { c -> c.isDigit() }
                        if (n.isEmpty() || (n.toIntOrNull() ?: 0) in 1..28) dayText = n
                    },
                    label = { Text("Día del mes (1–28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    val day = dayText.toIntOrNull()?.coerceIn(1, 28) ?: 1
                    if (description.isBlank() || amount <= 0) return@Button
                    onAdd(FixedExpense(
                        description = description.trim(),
                        amount = amount,
                        categoryName = selectedCategory,
                        walletId = walletId,
                        dayOfMonth = day,
                        currencyCode = currencyCode,
                        isActive = true
                    ))
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
