package com.upn3.proyecto_finanzas_personales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.upn3.proyecto_finanzas_personales.model.AuditLog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.upn3.proyecto_finanzas_personales.model.Transaction
import com.upn3.proyecto_finanzas_personales.model.TransactionType
import com.upn3.proyecto_finanzas_personales.model.Wallet
import java.text.SimpleDateFormat
import java.util.*

internal val IncomeGreen = Color(0xFF00C853)
internal val ExpenseRed = Color(0xFFFF1744)
internal val TransferBlue = Color(0xFF2979FF)

fun resolveCategoryIcon(iconName: String): ImageVector = when (iconName.lowercase()) {
    "restaurant" -> Icons.Filled.Restaurant
    "fastfood" -> Icons.Filled.Fastfood
    "local_grocery_store", "grocery" -> Icons.Filled.LocalGroceryStore
    "shopping_cart", "shoppingcart", "shopping" -> Icons.Filled.ShoppingCart
    "home", "house" -> Icons.Filled.Home
    "directions_car", "car", "transport" -> Icons.Filled.DirectionsCar
    "health_and_safety", "health" -> Icons.Filled.HealthAndSafety
    "local_hospital", "medical" -> Icons.Filled.LocalHospital
    "school", "education" -> Icons.Filled.School
    "work", "business", "office" -> Icons.Filled.Work
    "sports_esports", "games", "entertainment" -> Icons.Filled.SportsEsports
    "movie", "theater" -> Icons.Filled.Movie
    "account_balance", "bank" -> Icons.Filled.AccountBalance
    "savings", "saving" -> Icons.Filled.Savings
    "trending_up", "investment", "stocks" -> Icons.Filled.TrendingUp
    "attach_money", "income", "salary", "money" -> Icons.Filled.AttachMoney
    "receipt", "bill", "invoice" -> Icons.Filled.Receipt
    "flight", "travel", "trip" -> Icons.Filled.Flight
    "wifi", "internet" -> Icons.Filled.Wifi
    "phone", "mobile", "smartphone" -> Icons.Filled.Phone
    "swap_horiz", "transfer" -> Icons.Filled.SwapHoriz
    "pets" -> Icons.Filled.Pets
    "fitness_center", "gym", "fitness" -> Icons.Filled.FitnessCenter
    "local_gas_station", "gas", "fuel" -> Icons.Filled.LocalGasStation
    "water_drop", "water" -> Icons.Filled.WaterDrop
    "power", "electricity" -> Icons.Filled.Power
    "sistema", "system", "ajuste" -> Icons.Filled.Settings
    "child_care", "kids", "children" -> Icons.Filled.ChildCare
    else -> Icons.Filled.Category
}

fun getCurrencyFullName(code: String): String = when (code) {
    "PEN" -> "Sol Peruano (PEN)"
    "USD" -> "Dólar Americano (USD)"
    "EUR" -> "Euro (EUR)"
    "GBP" -> "Libra Esterlina (GBP)"
    "JPY" -> "Yen Japonés (JPY)"
    "MXN" -> "Peso Mexicano (MXN)"
    "CLP" -> "Peso Chileno (CLP)"
    "BRL" -> "Real Brasileño (BRL)"
    else -> code
}

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    categoryIconName: String,
    wallet: Wallet?,
    userEmail: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onViewVoucher: (() -> Unit)? = null,
    auditLogs: List<AuditLog> = emptyList(),
    isLoadingAuditLogs: Boolean = false,
    onLoadAuditLogs: () -> Unit = {}
) {
    val typeColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
    }
    val typeLabel = when (transaction.type) {
        TransactionType.INCOME -> "Ingreso"
        TransactionType.EXPENSE -> "Gasto"
        TransactionType.TRANSFER -> "Transferencia"
    }
    val typeIcon = when (transaction.type) {
        TransactionType.INCOME -> Icons.Filled.TrendingUp
        TransactionType.EXPENSE -> Icons.Filled.TrendingDown
        TransactionType.TRANSFER -> Icons.Filled.SwapHoriz
    }
    val amountSign = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val creationDate = Date(transaction.timestamp)
    val categoryIcon = resolveCategoryIcon(categoryIconName)
    var showMapDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transaction.id) { onLoadAuditLogs() }

    // Mapa de ubicación
    if (showMapDialog && transaction.latitude != null && transaction.longitude != null) {
        LocationMapDialog(
            latitude = transaction.latitude,
            longitude = transaction.longitude,
            transactionDescription = transaction.description.ifBlank { transaction.origin },
            onDismiss = { showMapDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                // ── Encabezado ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(typeColor.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                        .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(typeColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIcon,
                                    contentDescription = null,
                                    tint = typeColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            typeLabel,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            typeIcon,
                                            contentDescription = null,
                                            tint = typeColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = typeColor.copy(alpha = 0.12f),
                                        labelColor = typeColor
                                    ),
                                    border = AssistChipDefaults.assistChipBorder(
                                        enabled = true,
                                        borderColor = typeColor.copy(alpha = 0.3f)
                                    )
                                )
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Cerrar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = transaction.origin.ifBlank { "Sin categoría" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$amountSign$currencySymbol${String.format("%.2f", transaction.amount)}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 38.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = typeColor
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Información General ───────────────────────────────────────
                DetailSection("Información General", Icons.Filled.Info) {
                    DetailRow(
                        Icons.Filled.Category,
                        "Categoría",
                        transaction.origin.ifBlank { "—" }
                    )
                    if (transaction.description.isNotBlank()) {
                        DetailRow(
                            Icons.Filled.Notes,
                            "Descripción",
                            transaction.description
                        )
                    }
                    DetailRow(
                        Icons.Filled.CurrencyExchange,
                        "Moneda",
                        getCurrencyFullName(transaction.currencyCode)
                    )
                    DetailRow(
                        Icons.Filled.CalendarToday,
                        "Fecha creación",
                        "${dateFormat.format(creationDate)}  •  ${timeFormat.format(creationDate)}"
                    )
                    if (transaction.lastModified != null) {
                        DetailRow(
                            Icons.Filled.Edit,
                            "Última edición",
                            "${dateFormat.format(Date(transaction.lastModified))}  •  ${timeFormat.format(Date(transaction.lastModified))}"
                        )
                    } else {
                        DetailRow(
                            Icons.Filled.Edit,
                            "Última edición",
                            "Sin modificaciones",
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DetailRow(
                        Icons.Filled.CheckCircle,
                        "Estado",
                        "Completado",
                        valueColor = IncomeGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Información Financiera ────────────────────────────────────
                DetailSection("Información Financiera", Icons.Filled.AccountBalance) {
                    DetailRow(
                        Icons.Filled.AttachMoney,
                        "Monto original",
                        "$currencySymbol${String.format("%.2f", transaction.amount)}",
                        valueColor = typeColor
                    )
                    DetailRow(
                        Icons.Filled.Payments,
                        "Moneda original",
                        transaction.currencyCode
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Información Técnica ───────────────────────────────────────
                DetailSection("Información Técnica", Icons.Filled.Code) {
                    DetailRow(
                        Icons.Filled.Tag,
                        "ID de transacción",
                        transaction.id.take(8) + "…",
                        monospace = true
                    )
                    wallet?.let { w ->
                        DetailRow(Icons.Filled.AccountBalanceWallet, "Billetera", w.name)
                    }
                    if (userEmail.isNotBlank()) {
                        DetailRow(Icons.Filled.Person, "Usuario creador", userEmail)
                    }
                }

                // ── Ubicación GPS (si disponible) ─────────────────────────────
                if (transaction.latitude != null && transaction.longitude != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailSection("Ubicación GPS", Icons.Filled.LocationOn) {
                        DetailRow(
                            Icons.Filled.North,
                            "Latitud",
                            "%.6f".format(transaction.latitude),
                            monospace = true
                        )
                        DetailRow(
                            Icons.Filled.East,
                            "Longitud",
                            "%.6f".format(transaction.longitude),
                            monospace = true
                        )
                    }
                }

                // ── Destinatario (solo para Transferencias a terceros) ────────
                transaction.transferContact?.let { contact ->
                    if (contact.recipientName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailSection("Destinatario", Icons.Filled.Person) {
                            DetailRow(Icons.Filled.Person, "Nombre", contact.recipientName)
                            if (contact.recipientAlias.isNotBlank())
                                DetailRow(Icons.Filled.AlternateEmail, "Alias / Cuenta", contact.recipientAlias)
                            if (contact.bank.isNotBlank())
                                DetailRow(Icons.Filled.AccountBalance, "Banco", contact.bank)
                            if (contact.motivo.isNotBlank())
                                DetailRow(Icons.Filled.Notes, "Motivo", contact.motivo)
                        }
                    }
                }

                // ── Historial de Actividad ────────────────────────────────────
                Spacer(modifier = Modifier.height(12.dp))
                DetailSection("Historial de Actividad", Icons.Filled.History) {
                    ActivityTimeline(
                        logs = auditLogs,
                        isLoading = isLoadingAuditLogs,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Pie de diálogo ────────────────────────────────────────────
                val hasLocation = transaction.latitude != null && transaction.longitude != null
                val hasVoucher = !transaction.receiptPath.isNullOrBlank() && onViewVoucher != null
                val hasActions = hasVoucher || hasLocation

                if (hasActions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasVoucher) {
                            OutlinedButton(
                                onClick = onViewVoucher!!,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Receipt, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Voucher")
                            }
                        }
                        if (hasLocation) {
                            Button(
                                onClick = { showMapDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Map, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ver Mapa")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ── Componentes compartidos ───────────────────────────────────────────────────

@Composable
internal fun DetailSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color? = null,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = if (monospace)
                MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            else
                MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 190.dp)
        )
    }
}
