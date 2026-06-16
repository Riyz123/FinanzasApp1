package com.upn3.proyecto_finanzas_personales.model

import java.util.UUID

data class Wallet(
    val id: String = UUID.randomUUID().toString(),
    val accountId: String = generateAccountId(),
    val name: String = "",
    val currencyCode: String = "PEN",
    val balance: Double = 0.0,
    val color: Long = 0xFF4CAF50
)

fun generateAccountId(): String = (100_000_000_000L..999_999_999_999L).random().toString()
