package com.upn3.proyecto_finanzas_personales.model

import java.util.UUID

data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: Long = 0L, // 0 = sin fecha límite
    val currencyCode: String = "PEN",
    val color: String = "#4338CA"
)
