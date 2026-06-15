package com.upn3.proyecto_finanzas_personales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.upn3.proyecto_finanzas_personales.model.Transaction
import com.upn3.proyecto_finanzas_personales.model.Wallet
import java.text.SimpleDateFormat
import java.util.*

private val ConversionAmber = Color(0xFFFFAB00)

private fun parseConversionAmounts(description: String): Pair<String, String>? {
    val arrowIdx = description.indexOf("→")
    if (arrowIdx == -1) return null
    val prefix = "Conversión de saldo "
    if (!description.startsWith(prefix)) return null
    val fromPart = description.substring(prefix.length, arrowIdx).trim()
    val toPart = description.substring(arrowIdx + 1).trim()
    return Pair(fromPart, toPart)
}

private fun inferCurrencyCode(symbolAndAmount: String): String = when {
    symbolAndAmount.startsWith("S/.") -> "PEN"
    symbolAndAmount.startsWith("Mex$") -> "MXN"
    symbolAndAmount.startsWith("CLP$") -> "CLP"
    symbolAndAmount.startsWith("R$") -> "BRL"
    symbolAndAmount.startsWith("€") -> "EUR"
    symbolAndAmount.startsWith("£") -> "GBP"
    symbolAndAmount.startsWith("¥") -> "JPY"
    symbolAndAmount.startsWith("$") -> "USD"
    else -> "—"
}

@Composable
fun ConversionDetailDialog(
    transaction: Transaction,
    wallet: Wallet?,
    userEmail: String,
    onDismiss: () -> Unit
) {
    val parsed = parseConversionAmounts(transaction.description)
    val fromText = parsed?.first ?: "—"
    val toText = parsed?.second ?: "—"
    val fromCurrency = if (parsed != null) inferCurrencyCode(fromText) else "—"
    val toCurrency = transaction.currencyCode

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val date = Date(transaction.timestamp)

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
                                listOf(ConversionAmber.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                        .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 24.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CurrencyExchange,
                                    contentDescription = null,
                                    tint = ConversionAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "CONVERSIÓN DE MONEDA",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ConversionAmber,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // Flujo visual: MONEDA_ORIGEN → MONEDA_DESTINO
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    fromCurrency,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    fromText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = ConversionAmber,
                                modifier = Modifier.size(34.dp)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    toCurrency,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ConversionAmber
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    toText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ConversionAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Detalle de Conversión ─────────────────────────────────────
                DetailSection("Detalle de Conversión", Icons.Filled.CurrencyExchange) {
                    DetailRow(Icons.Filled.ArrowUpward, "Moneda origen", fromCurrency)
                    DetailRow(
                        Icons.Filled.ArrowDownward,
                        "Moneda destino",
                        toCurrency,
                        valueColor = ConversionAmber
                    )
                    if (fromText != "—") {
                        DetailRow(Icons.Filled.AttachMoney, "Cantidad origen", fromText)
                    }
                    if (toText != "—") {
                        DetailRow(
                            Icons.Filled.Payments,
                            "Cantidad destino",
                            toText,
                            valueColor = ConversionAmber
                        )
                    }
                    if (fromText != "—" && toText != "—") {
                        val fromNum = fromText.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                        val toNum = toText.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                        if (fromNum != null && toNum != null && fromNum > 0) {
                            DetailRow(
                                Icons.Filled.ShowChart,
                                "Tipo de cambio",
                                "1 $fromCurrency = ${"%.4f".format(toNum / fromNum)} $toCurrency"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Información Temporal ──────────────────────────────────────
                DetailSection("Información Temporal", Icons.Filled.Schedule) {
                    DetailRow(Icons.Filled.CalendarToday, "Fecha", dateFormat.format(date))
                    DetailRow(Icons.Filled.AccessTime, "Hora", timeFormat.format(date))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Auditoría ─────────────────────────────────────────────────
                DetailSection("Auditoría", Icons.Filled.AdminPanelSettings) {
                    DetailRow(
                        Icons.Filled.AutoAwesome,
                        "Tipo de conversión",
                        "Automática"
                    )
                    if (userEmail.isNotBlank()) {
                        DetailRow(Icons.Filled.Person, "Usuario", userEmail)
                    }
                    wallet?.let {
                        DetailRow(
                            Icons.Filled.AccountBalanceWallet,
                            "Billetera afectada",
                            it.name
                        )
                    }
                    DetailRow(
                        Icons.Filled.Tag,
                        "ID de registro",
                        transaction.id.take(8) + "…",
                        monospace = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
