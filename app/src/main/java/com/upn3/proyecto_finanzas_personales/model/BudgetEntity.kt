package com.upn3.proyecto_finanzas_personales.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val walletId: String = "",
    val categoryName: String = "",
    val limitAmount: Double = 0.0,
    val period: String = "MONTHLY",
    val currencyCode: String = "PEN"
)
