package com.upn3.proyecto_finanzas_personales.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val action: String,
    val timestamp: Long,
    val userEmail: String,
    val changedFieldsJson: String = "{}"
)
