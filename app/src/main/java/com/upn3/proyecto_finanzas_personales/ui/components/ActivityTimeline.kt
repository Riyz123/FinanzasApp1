package com.upn3.proyecto_finanzas_personales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upn3.proyecto_finanzas_personales.model.AuditAction
import com.upn3.proyecto_finanzas_personales.model.AuditLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityTimeline(
    logs: List<AuditLog>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            logs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin historial de actividad",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                logs.forEachIndexed { index, log ->
                    TimelineRow(
                        log = log,
                        isFirst = index == 0,
                        isLast = index == logs.lastIndex
                    )
                }
            }
        }
    }
}

private data class EventStyle(val icon: ImageVector, val color: Color, val title: String)

private fun resolveEvent(log: AuditLog): EventStyle = when (log.action) {
    AuditAction.CREACION             -> EventStyle(Icons.Default.Add,           Color(0xFF2E7D32), "Transacción creada")
    AuditAction.EDICION              -> EventStyle(Icons.Default.Edit,          Color(0xFF1565C0), "Transacción editada")
    AuditAction.VOUCHER_AGREGADO     -> EventStyle(Icons.Default.Receipt,       Color(0xFF00695C), "Comprobante adjuntado")
    AuditAction.VOUCHER_ELIMINADO    -> EventStyle(Icons.Default.DeleteForever, Color(0xFFC62828), "Comprobante eliminado")
    AuditAction.VOUCHER_REEMPLAZADO  -> EventStyle(Icons.Default.SwapVert,      Color(0xFFE65100), "Comprobante reemplazado")
    AuditAction.TRANSFERENCIA        -> EventStyle(Icons.Default.SwapHoriz,     Color(0xFF6A1B9A), "Transferencia registrada")
    AuditAction.ELIMINACION          -> EventStyle(Icons.Default.Delete,        Color(0xFFC62828), "Transacción eliminada")
    AuditAction.UBICACION_REGISTRADA -> EventStyle(Icons.Default.LocationOn,    Color(0xFF00838F), "Ubicación registrada")
    AuditAction.UBICACION_ACTUALIZADA-> EventStyle(Icons.Default.EditLocation,  Color(0xFF0277BD), "Ubicación actualizada")
    AuditAction.UBICACION_ELIMINADA  -> EventStyle(Icons.Default.LocationOff,   Color(0xFF6D4C41), "Ubicación eliminada")
    else                             -> EventStyle(Icons.Default.Info,          Color(0xFF546E7A), log.action)
}

private fun buildDetails(log: AuditLog): List<String> {
    if (log.changedFields.isEmpty()) return emptyList()
    val isVoucherOnly = log.action in listOf(
        AuditAction.VOUCHER_AGREGADO, AuditAction.VOUCHER_ELIMINADO, AuditAction.VOUCHER_REEMPLAZADO
    )
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return log.changedFields.mapNotNull { (field, change) ->
        val old = change["old"] ?: ""
        val new = change["new"] ?: ""
        when {
            field == "comprobante" && isVoucherOnly -> null
            field == "comprobante" -> "Comprobante: modificado"
            field == "destinatario" && old.isEmpty() -> "Destinatario: $new"
            field == "destinatario" -> "Destinatario: $old → $new"
            field == "fecha" -> {
                val oldFmt = old.toLongOrNull()?.let { dateFmt.format(Date(it)) } ?: old
                val newFmt = new.toLongOrNull()?.let { dateFmt.format(Date(it)) } ?: new
                "Fecha: $oldFmt → $newFmt"
            }
            field == "monto" && old.isEmpty() -> "Monto: $new"
            field == "monto" -> "Monto: $old → $new"
            field == "ubicación" && old.isEmpty() -> "Ubicación capturada"
            field == "ubicación" && new.isEmpty() -> "Ubicación eliminada"
            field == "ubicación" -> "Ubicación actualizada"
            else -> "${field.replaceFirstChar { it.uppercase() }}: $old → $new"
        }
    }
}

@Composable
private fun TimelineRow(
    log: AuditLog,
    isFirst: Boolean,
    isLast: Boolean
) {
    val style = remember(log.action) { resolveEvent(log) }
    val details = remember(log.changedFields) { buildDetails(log) }
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).format(Date(log.timestamp))
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline rail
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (isFirst) 8.dp else 16.dp)
                    .background(if (isFirst) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(style.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    style.icon,
                    contentDescription = null,
                    tint = style.color,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 4.dp)
        ) {
            Text(
                style.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                details.forEach { detail ->
                    Text(
                        "• $detail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isLast) Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
