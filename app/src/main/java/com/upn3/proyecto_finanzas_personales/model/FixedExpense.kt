package com.upn3.proyecto_finanzas_personales.model

import java.util.UUID

data class FixedExpense(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val amount: Double = 0.0,
    val categoryName: String = "",
    val walletId: String = "",
    val dayOfMonth: Int = 1, // 1–28
    val currencyCode: String = "PEN",
    val isActive: Boolean = true
)
