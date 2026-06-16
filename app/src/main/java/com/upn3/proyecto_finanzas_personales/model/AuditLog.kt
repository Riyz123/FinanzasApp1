package com.upn3.proyecto_finanzas_personales.model

import java.util.UUID

object AuditAction {
    const val CREACION = "CREACION"
    const val EDICION = "EDICION"
    const val VOUCHER_AGREGADO = "VOUCHER_AGREGADO"
    const val VOUCHER_ELIMINADO = "VOUCHER_ELIMINADO"
    const val VOUCHER_REEMPLAZADO = "VOUCHER_REEMPLAZADO"
    const val TRANSFERENCIA = "TRANSFERENCIA"
    const val ELIMINACION = "ELIMINACION"
    const val UBICACION_REGISTRADA = "UBICACION_REGISTRADA"
    const val UBICACION_ACTUALIZADA = "UBICACION_ACTUALIZADA"
    const val UBICACION_ELIMINADA = "UBICACION_ELIMINADA"
}

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val transactionId: String = "",
    val action: String = AuditAction.CREACION,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String = "",
    // changedFields["monto"]["old"] = "100.0", ["new"] = "200.0"
    val changedFields: Map<String, Map<String, String>> = emptyMap()
)
