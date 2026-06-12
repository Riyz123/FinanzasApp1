package com.upn3.proyecto_finanzas_personales.utils

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object LanguageManager {

    fun createTranslator(
        sourceLanguage: String,
        targetLanguage: String
    ): Translator {

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()

        return Translation.getClient(options)
    }

    fun getMlKitLanguage(code: String): String {

        return when (code) {

            "es" -> TranslateLanguage.SPANISH
            "en" -> TranslateLanguage.ENGLISH
            "pt" -> TranslateLanguage.PORTUGUESE

            else -> TranslateLanguage.ENGLISH
        }
    }
}