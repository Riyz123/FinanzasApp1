package com.upn3.proyecto_finanzas_personales.model

import java.util.UUID

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val walletId: String = "",
    val categoryName: String = "",
    val limitAmount: Double = 0.0,
    val period: String = "MONTHLY", // MONTHLY | WEEKLY
    val currencyCode: String = "PEN"
)
