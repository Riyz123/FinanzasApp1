package com.upn3.proyecto_finanzas_personales.model

import java.util.UUID

data class Nota(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String = "",
    val contenido: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
