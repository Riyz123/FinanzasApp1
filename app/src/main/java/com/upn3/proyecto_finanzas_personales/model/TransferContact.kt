package com.upn3.proyecto_finanzas_personales.model

data class TransferContact(
    val recipientName: String = "",
    val recipientAlias: String = "",
    val bank: String = "",
    val motivo: String = ""
)
