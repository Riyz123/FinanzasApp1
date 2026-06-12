package com.upn3.proyecto_finanzas_personales.model

data class LanguageOption(
    val code: String,
    val name: String,
    val flag: String,
    val isInstalled: Boolean = false,
    val canDownload: Boolean = false,
    val onlineOnly: Boolean = false
)