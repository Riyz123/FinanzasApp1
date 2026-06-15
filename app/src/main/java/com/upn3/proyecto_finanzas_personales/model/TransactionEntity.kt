package com.upn3.proyecto_finanzas_personales.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userEmail: String,
    val amount: Double,
    val description: String,
    val origin: String,
    val type: String,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastModified: Long? = null,
    val receiptPath: String? = null,
    val recipientName: String? = null,
    val recipientAlias: String? = null,
    val recipientBank: String? = null,
    val transferMotivo: String? = null
)
