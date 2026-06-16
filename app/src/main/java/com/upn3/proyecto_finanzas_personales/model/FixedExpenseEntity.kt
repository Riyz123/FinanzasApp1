package com.upn3.proyecto_finanzas_personales.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "fixed_expenses")
data class FixedExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val amount: Double = 0.0,
    val categoryName: String = "",
    val walletId: String = "",
    val dayOfMonth: Int = 1,
    val currencyCode: String = "PEN",
    val isActive: Boolean = true
)
