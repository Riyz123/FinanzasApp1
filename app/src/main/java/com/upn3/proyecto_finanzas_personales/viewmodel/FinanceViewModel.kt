package com.upn3.proyecto_finanzas_personales.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.upn3.proyecto_finanzas_personales.data.UserPreferences
import com.upn3.proyecto_finanzas_personales.model.*
import com.upn3.proyecto_finanzas_personales.ui.theme.AppTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import com.upn3.proyecto_finanzas_personales.network.CurrencyService
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import com.upn3.proyecto_finanzas_personales.network.CloudinaryClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import android.util.Log

data class UserSearchResult(
    val email: String = "",
    val displayName: String = ""
)

data class WalletLookupResult(
    val ownerEmail: String = "",
    val ownerName: String = "",
    val wallet: Wallet = Wallet()
)

data class FinanceState(
    val balance: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val selectedWallet: Wallet? = null,
    val currentUser: User? = null,
    val errorMessage: String? = null,
    val selectedTheme: AppTheme = AppTheme.DEFAULT,
    val isLoading: Boolean = true,
    val isExchangeLoading: Boolean = false,
    val exchangeRatePreview: Double? = null,
    val globalBalance: Double = 0.0,
    val chartIncome: Double = 0.0,
    val chartExpense: Double = 0.0,
    val convertedTransactions: List<Transaction> = emptyList(),
    val preferredCurrency: String = "PEN",
    val lastRatesUpdate: Long = 0L,
    val searchQuery: String = "",
    val selectedTab: Int = 0, // 0: Todas, 1: Gastos, 2: Ingresos, 3: Transferencias, 4: Conversiones
    val filteredTransactions: List<Transaction> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList(),
    val isLoadingAuditLogs: Boolean = false,
    val budgets: List<com.upn3.proyecto_finanzas_personales.model.Budget> = emptyList(),
    val savingsGoals: List<com.upn3.proyecto_finanzas_personales.model.SavingsGoal> = emptyList(),
    val fixedExpenses: List<com.upn3.proyecto_finanzas_personales.model.FixedExpense> = emptyList(),
    val isLoadingBudgets: Boolean = false,
    val allWalletTransactions: List<Transaction> = emptyList(),
    val userSearchResults: List<UserSearchResult> = emptyList(),
    val isSearchingUsers: Boolean = false
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userPreferences = UserPreferences(application)

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/v6/latest/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val currencyService = retrofit.create(CurrencyService::class.java)
    private val gson = Gson()
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(FinanceState())
    val uiState: StateFlow<FinanceState> = _uiState.asStateFlow()

    private val allTransactions = mutableListOf<Transaction>()
    private val allCategories = mutableListOf<Category>()
    private val allWallets = mutableListOf<Wallet>()

    init {
        checkSession()
        observeLastUpdate()
    }

    private fun observeLastUpdate() {
        viewModelScope.launch {
            userPreferences.lastRatesUpdate.collect { timestamp ->
                _uiState.update { it.copy(lastRatesUpdate = timestamp) }
            }
        }
    }

    private fun checkSession() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.email != null) {
            viewModelScope.launch {
                try {
                    val userDoc = db.collection("users").document(currentUser.email!!).get().await()
                    if (userDoc.exists()) {
                        val user = userDoc.toObject(User::class.java)
                        if (user != null) {
                            val theme = try { AppTheme.valueOf(user.theme) } catch (e: Exception) { AppTheme.DEFAULT }
                            _uiState.update { it.copy(currentUser = user, selectedTheme = theme, isLoading = false) }
                            loadWallets()
                            loadTransactions()
                            loadCategories()
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun selectTheme(theme: AppTheme) {
        _uiState.update { it.copy(selectedTheme = theme) }
        val user = uiState.value.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(user.email)
                    .update("theme", theme.name).await()
                _uiState.update { it.copy(currentUser = user.copy(theme = theme.name)) }
            } catch (e: Exception) {
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun updateSelectedTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val baseList = if (state.searchQuery.isEmpty()) {
            state.transactions
        } else {
            state.transactions.filter { transaction ->
                transaction.description.contains(state.searchQuery, ignoreCase = true) ||
                        transaction.origin.contains(state.searchQuery, ignoreCase = true) ||
                        transaction.type.name.contains(state.searchQuery, ignoreCase = true)
            }
        }

        val filtered = when (state.selectedTab) {
            1 -> baseList.filter {
                it.type == TransactionType.EXPENSE &&
                        it.origin != "Ajuste de Moneda"
            }
            2 -> baseList.filter {
                it.type == TransactionType.INCOME &&
                        it.origin != "Ajuste de Moneda"
            }
            3 -> baseList.filter {
                it.origin == "Transferencia" || it.origin == "Transferencia Externa" || it.type == TransactionType.TRANSFER
            }
            4 -> baseList.filter {
                it.origin == "Ajuste de Moneda"
            }
            else -> baseList
        }
        _uiState.update { it.copy(filteredTransactions = filtered) }
    }

    fun uploadProfilePicture(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("profile_picture", ".jpg")
                tempFile.outputStream().use { output ->
                    inputStream?.copyTo(output)
                }

                val requestFile = tempFile.asRequestBody("image/*".toMediaType())
                val imagePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val preset = "profile_images".toRequestBody("text/plain".toMediaType())

                val response = CloudinaryClient.api.uploadImage(imagePart, preset)
                onSuccess(response.secureUrl)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al subir imagen: ${e.message}") }
            }
        }
    }

    fun updateUser(newName: String, newLastName: String, newEmail: String, newPass: String, newProfilePic: String, onSuccess: () -> Unit) {
        val currentUser = uiState.value.currentUser ?: return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                if (newPass.isNotBlank() && newPass != currentUser.password) {
                    auth.currentUser?.updatePassword(newPass)?.await()
                }
                if (newEmail != currentUser.email) {
                    auth.currentUser?.updateEmail(newEmail)?.await()
                }

                val updatedUser = currentUser.copy(
                    name = newName,
                    lastname = newLastName,
                    email = newEmail,
                    password = if (newPass.isNotBlank()) newPass else currentUser.password,
                    profilePicture = newProfilePic
                )

                if (newEmail != currentUser.email) {
                    val doc = db.collection("users").document(newEmail).get().await()
                    if (doc.exists()) {
                        _uiState.update { it.copy(errorMessage = "El nuevo correo ya está en uso") }
                        return@launch
                    }
                    db.collection("users").document(newEmail).set(updatedUser).await()
                    db.collection("users").document(currentUser.email).delete().await()
                } else {
                    db.collection("users").document(currentUser.email).set(updatedUser).await()
                }

                _uiState.update { it.copy(currentUser = updatedUser, errorMessage = null, isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar perfil: ${e.message}", isLoading = false) }
            }
        }
    }

    fun register(firstName: String, lastName: String, email: String, pass: String, repeatPass: String, onSuccess: () -> Unit) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || pass.isBlank() || repeatPass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Todos los campos son obligatorios") }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "Correo electrónico no válido") }
            return
        }
        if (pass != repeatPass) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
            return
        }

        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                val newUser = User(
                    email = email, 
                    password = pass,
                    name = firstName, 
                    lastname = lastName, 
                    theme = uiState.value.selectedTheme.name,
                    profilePicture = ""
                )
                db.collection("users").document(email).set(newUser).await()
                userPreferences.saveUserEmail(email)
                _uiState.update { it.copy(currentUser = newUser, errorMessage = null) }
                loadWallets()
                loadTransactions()
                loadCategories()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al registrar: ${e.message}") }
            }
        }
    }

    fun exportData(uri: Uri, password: String?, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = uiState.value.currentUser
                val exportDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val backupMap = mapOf(
                    "meta" to mapOf(
                        "version" to "2.0",
                        "exportDate" to exportDate,
                        "userName" to "${user?.name ?: ""} ${user?.lastname ?: ""}".trim(),
                        "totalTransactions" to allTransactions.size,
                        "totalWallets" to allWallets.size,
                        "totalCategories" to allCategories.size
                    ),
                    "wallets" to allWallets,
                    "categories" to allCategories,
                    "transactions" to allTransactions
                )
                val json = Gson().toJson(backupMap)

                val finalData = if (!password.isNullOrBlank()) {
                    val saltBytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
                    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    val keySpec = PBEKeySpec(password.toCharArray(), saltBytes, 65536, 256)
                    val secret = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.ENCRYPT_MODE, secret)
                    val iv = cipher.parameters.getParameterSpec(IvParameterSpec::class.java).iv
                    val encrypted = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
                    Gson().toJson(mapOf(
                        "encrypted" to true,
                        "salt" to Base64.encodeToString(saltBytes, Base64.NO_WRAP),
                        "iv" to Base64.encodeToString(iv, Base64.NO_WRAP),
                        "data" to Base64.encodeToString(encrypted, Base64.NO_WRAP)
                    ))
                } else {
                    json
                }

                context.contentResolver.openOutputStream(uri)?.use { it.write(finalData.toByteArray(Charsets.UTF_8)) }
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = "EXPORT_OK:${allTransactions.size}:${allWallets.size}") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = "Error al exportar: ${e.message}") }
                }
            }
        }
    }

    fun importData(uri: Uri, password: String?, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw Exception("No se pudo leer el archivo")

                val outer = com.google.gson.JsonParser.parseString(content).asJsonObject
                val json = if (outer.has("encrypted") && outer.get("encrypted").asBoolean) {
                    if (password.isNullOrBlank()) throw Exception("NEEDS_PASSWORD")
                    val saltBytes = Base64.decode(outer.get("salt").asString, Base64.NO_WRAP)
                    val iv = Base64.decode(outer.get("iv").asString, Base64.NO_WRAP)
                    val encrypted = Base64.decode(outer.get("data").asString, Base64.NO_WRAP)
                    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    val keySpec = PBEKeySpec(password.toCharArray(), saltBytes, 65536, 256)
                    val secret = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(iv))
                    String(cipher.doFinal(encrypted), Charsets.UTF_8)
                } else content

                val backup = com.google.gson.JsonParser.parseString(json).asJsonObject
                val gson = Gson()
                val userEmail = uiState.value.currentUser?.email ?: return@launch

                val transactions = backup.getAsJsonArray("transactions")
                    ?.map { gson.fromJson(it, Transaction::class.java) } ?: emptyList()
                val wallets = backup.getAsJsonArray("wallets")
                    ?.map { gson.fromJson(it, Wallet::class.java) } ?: emptyList()
                val categories = backup.getAsJsonArray("categories")
                    ?.map { gson.fromJson(it, Category::class.java) } ?: emptyList()

                if (transactions.isEmpty() && wallets.isEmpty()) throw Exception("Archivo vacío o formato inválido")

                transactions.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { t ->
                        batch.set(db.collection("users").document(userEmail).collection("transactions").document(t.id), t)
                    }
                    batch.commit().await()
                }
                wallets.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { w ->
                        batch.set(db.collection("users").document(userEmail).collection("wallets").document(w.id), w)
                    }
                    batch.commit().await()
                }
                categories.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { c ->
                        batch.set(db.collection("users").document(userEmail).collection("categories").document(c.id), c)
                    }
                    batch.commit().await()
                }

                loadTransactions(); loadWallets(); loadCategories()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = "IMPORT_OK:${transactions.size}:${wallets.size}") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = if (e.message == "NEEDS_PASSWORD") "NEEDS_PASSWORD" else "Error al importar: ${e.message}") }
                }
            }
        }
    }

    fun exportReportToPdf(
        uri: Uri,
        reportTitle: String,
        dateRangeText: String,
        transactions: List<Transaction>,
        totalIncome: Double,
        totalExpense: Double,
        totalTransfer: Double,
        currencySymbol: String,
        context: android.content.Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            fun mkText(color: Int, size: Float, bold: Boolean = false) =
                android.graphics.Paint().apply {
                    this.color = color; textSize = size; isFakeBoldText = bold; isAntiAlias = true
                }
            fun mkFill(color: Int) = android.graphics.Paint().apply { this.color = color; isAntiAlias = true }
            fun mkLine() = android.graphics.Paint().apply {
                this.color = android.graphics.Color.parseColor("#E5E7EB")
                style = android.graphics.Paint.Style.STROKE; strokeWidth = 0.5f; isAntiAlias = true
            }
            fun truncate(text: String, paint: android.graphics.Paint, maxWidth: Float): String {
                if (paint.measureText(text) <= maxWidth) return text
                var s = text
                while (s.isNotEmpty() && paint.measureText("$s…") > maxWidth) s = s.dropLast(1)
                return if (s.isEmpty()) "" else "$s…"
            }
            try {
                val pdfDoc = android.graphics.pdf.PdfDocument()
                val W = 595; val H = 842; val M = 36f; val CW = W - M * 2

                val primaryDark = android.graphics.Color.parseColor("#1E1B4B")
                val primary     = android.graphics.Color.parseColor("#4338CA")
                val primaryLight= android.graphics.Color.parseColor("#C7D2FE")
                val greenDark   = android.graphics.Color.parseColor("#064E3B")
                val redDark     = android.graphics.Color.parseColor("#7F1D1D")
                val grayText    = android.graphics.Color.parseColor("#374151")
                val grayAlt     = android.graphics.Color.parseColor("#F3F4F6")
                val white       = android.graphics.Color.WHITE

                val dateFmt = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
                val genDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

                val rowH = 16f; val tHdrH = 20f
                val firstHdrH = 172f; val otherHdrH = 34f; val footerH = 26f

                val colDateW = 72f; val colDescW = CW * 0.36f; val colCatW = CW * 0.22f; val colTypeW = 54f
                val colAmtW = CW - colDateW - colDescW - colCatW - colTypeW
                val colX = floatArrayOf(M, M + colDateW, M + colDateW + colDescW,
                    M + colDateW + colDescW + colCatW, M + colDateW + colDescW + colCatW + colTypeW)

                val rowsFirst = ((H - M - firstHdrH - tHdrH - footerH) / rowH).toInt().coerceAtLeast(1)
                val rowsOther = ((H - M * 2 - otherHdrH - tHdrH - footerH) / rowH).toInt().coerceAtLeast(1)

                val pages = mutableListOf<List<Transaction>>()
                if (transactions.isEmpty()) { pages.add(emptyList()) }
                else {
                    pages.add(transactions.take(rowsFirst))
                    var off = rowsFirst
                    while (off < transactions.size) { pages.add(transactions.subList(off, minOf(off + rowsOther, transactions.size))); off += rowsOther }
                }

                pages.forEachIndexed { pIdx, pageTxs ->
                    val pi = android.graphics.pdf.PdfDocument.PageInfo.Builder(W, H, pIdx + 1).create()
                    val page = pdfDoc.startPage(pi)
                    val cv = page.canvas
                    var y = M

                    if (pIdx == 0) {
                        cv.drawRoundRect(android.graphics.RectF(M, y, W - M, y + 44f), 8f, 8f, mkFill(primaryDark))
                        cv.drawText("FINANZAS", M + 14f, y + 29f, mkText(white, 20f, true))
                        cv.drawText("Reporte Financiero Personal", W - M - 202f, y + 18f, mkText(primaryLight, 8.5f))
                        cv.drawText("Generado: $genDate", W - M - 202f, y + 32f, mkText(primaryLight, 8f))
                        y += 54f

                        cv.drawText(reportTitle, M, y + 15f, mkText(primaryDark, 14f, true))
                        cv.drawText(dateRangeText, M, y + 29f, mkText(grayText, 10f))
                        y += 40f

                        val netBal = totalIncome - totalExpense
                        val cardW = (CW - 12f) / 3f
                        val cards = listOf(
                            Triple("INGRESOS", totalIncome, greenDark),
                            Triple("GASTOS", totalExpense, redDark),
                            Triple("BALANCE NETO", netBal,
                                if (netBal >= 0) android.graphics.Color.parseColor("#1E3A5F")
                                else android.graphics.Color.parseColor("#7C2D12"))
                        )
                        cards.forEachIndexed { i, (lbl, amt, col) ->
                            val cx = M + i * (cardW + 6f)
                            cv.drawRoundRect(android.graphics.RectF(cx, y, cx + cardW, y + 46f), 6f, 6f, mkFill(col))
                            cv.drawText(lbl, cx + 8f, y + 14f, mkText(primaryLight, 7.5f, true))
                            val pfx = when(lbl) { "INGRESOS" -> "+"; "BALANCE NETO" -> if (netBal >= 0) "+" else "-"; else -> "" }
                            val absAmt = if (amt < 0) -amt else amt
                            cv.drawText("$pfx$currencySymbol ${String.format("%.2f", absAmt)}", cx + 8f, y + 34f, mkText(white, 10f, true))
                        }
                        y += 56f

                        if (totalTransfer > 0) {
                            cv.drawRoundRect(android.graphics.RectF(M, y, W - M, y + 26f), 5f, 5f, mkFill(android.graphics.Color.parseColor("#3730A3")))
                            cv.drawText("TRANSFERENCIAS", M + 10f, y + 17f, mkText(primaryLight, 8f, true))
                            cv.drawText("$currencySymbol ${String.format("%.2f", totalTransfer)}", M + 140f, y + 17f, mkText(white, 10f, true))
                            y += 34f
                        } else { y += 4f }

                        cv.drawLine(M, y, W.toFloat() - M, y, mkLine())
                        y += 6f
                        cv.drawText("Detalle de Movimientos", M, y + 12f, mkText(primaryDark, 11f, true))
                        y += 20f
                    } else {
                        cv.drawRect(android.graphics.RectF(M, y, W.toFloat() - M, y + 24f), mkFill(primaryDark))
                        cv.drawText("FINANZAS  —  $reportTitle", M + 8f, y + 16f, mkText(white, 9f, true))
                        val pgLbl = "Pág. ${pIdx + 1} / ${pages.size}"
                        val pgP = mkText(primaryLight, 8.5f).also { it.textAlign = android.graphics.Paint.Align.RIGHT }
                        cv.drawText(pgLbl, W - M - 2f, y + 16f, pgP)
                        y += 32f
                    }

                    cv.drawRect(android.graphics.RectF(M, y, W.toFloat() - M, y + tHdrH), mkFill(primary))
                    val hp = mkText(white, 8.5f, true)
                    listOf("Fecha", "Descripción", "Categoría", "Tipo", "Monto").forEachIndexed { i, lbl ->
                        cv.drawText(lbl, colX[i] + 3f, y + 14f, hp)
                    }
                    y += tHdrH

                    val cp = mkText(grayText, 8f)
                    val ap = mkText(grayText, 8f, true)

                    pageTxs.forEachIndexed { ri, tx ->
                        cv.drawRect(android.graphics.RectF(M, y, W.toFloat() - M, y + rowH), mkFill(if (ri % 2 == 0) grayAlt else white))
                        val isConversion = tx.origin == "Ajuste de Moneda"
                        val txColor = when {
                            isConversion                       -> android.graphics.Color.parseColor("#6B21A8")
                            tx.type == TransactionType.INCOME  -> android.graphics.Color.parseColor("#065F46")
                            tx.type == TransactionType.EXPENSE -> android.graphics.Color.parseColor("#991B1B")
                            else                               -> android.graphics.Color.parseColor("#3730A3")
                        }
                        val typeStr = when {
                            isConversion                       -> "Conversión"
                            tx.origin == "Transferencia" || tx.origin == "Transferencia Externa" -> "Transfer."
                            tx.type == TransactionType.INCOME  -> "Ingreso"
                            tx.type == TransactionType.EXPENSE -> "Gasto"
                            else                               -> "Transfer."
                        }
                        val pfx = when {
                            isConversion || tx.type == TransactionType.TRANSFER
                                || tx.origin == "Transferencia" || tx.origin == "Transferencia Externa" -> ""
                            tx.type == TransactionType.INCOME  -> "+"
                            else                               -> "-"
                        }
                        val ty = y + rowH - 4f
                        cp.color = grayText
                        cv.drawText(dateFmt.format(java.util.Date(tx.timestamp)), colX[0] + 2f, ty, cp)
                        cv.drawText(truncate(tx.description, cp, colDescW - 6f), colX[1] + 2f, ty, cp)
                        cv.drawText(truncate(tx.origin, cp, colCatW - 6f), colX[2] + 2f, ty, cp)
                        cp.color = txColor; cv.drawText(typeStr, colX[3] + 2f, ty, cp)
                        val txSym = getCurrencySymbol(tx.currencyCode)
                        ap.color = txColor; cv.drawText("$pfx$txSym${String.format("%.2f", tx.amount)}", colX[4] + 2f, ty, ap)
                        cv.drawLine(M, y + rowH, W.toFloat() - M, y + rowH, mkLine())
                        y += rowH
                    }

                    if (pageTxs.isEmpty()) {
                        cv.drawText("No hay transacciones en este periodo", M + 40f, y + 20f, mkText(android.graphics.Color.parseColor("#9CA3AF"), 10f))
                    }

                    val fY = H - 16f
                    cv.drawLine(M, fY - 8f, W.toFloat() - M, fY - 8f, mkLine())
                    cv.drawText("Finanzas App  •  $genDate", M, fY, mkText(android.graphics.Color.parseColor("#9CA3AF"), 7.5f))
                    val pgNum = "Pág. ${pIdx + 1} de ${pages.size}"
                    val pgNP = mkText(android.graphics.Color.parseColor("#9CA3AF"), 7.5f).also { it.textAlign = android.graphics.Paint.Align.RIGHT }
                    cv.drawText(pgNum, W.toFloat() - M, fY, pgNP)

                    pdfDoc.finishPage(page)
                }

                context.contentResolver.openOutputStream(uri)?.use { pdfDoc.writeTo(it) }
                pdfDoc.close()
                withContext(Dispatchers.Main) { _uiState.update { it.copy(errorMessage = "REPORT_PDF_OK") } }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _uiState.update { it.copy(errorMessage = "Error al generar PDF: ${e.message}") } }
            }
        }
    }

    fun exportReportToCsv(
        uri: Uri,
        reportTitle: String,
        dateRangeText: String,
        transactions: List<Transaction>,
        totalIncome: Double,
        totalExpense: Double,
        totalTransfer: Double,
        currencySymbol: String,
        context: android.content.Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dateFmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                val genDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                val sb = StringBuilder()
                sb.append('﻿') // BOM for Excel UTF-8
                sb.appendLine("\"$reportTitle\"")
                sb.appendLine("\"Período:\",\"$dateRangeText\"")
                sb.appendLine("\"Generado:\",\"$genDate\"")
                sb.appendLine()
                sb.appendLine("\"--- RESUMEN ---\"")
                sb.appendLine("\"Ingresos:\",\"$currencySymbol ${String.format("%.2f", totalIncome)}\"")
                sb.appendLine("\"Gastos:\",\"$currencySymbol ${String.format("%.2f", totalExpense)}\"")
                if (totalTransfer > 0) sb.appendLine("\"Transferencias:\",\"$currencySymbol ${String.format("%.2f", totalTransfer)}\"")
                val netBal = totalIncome - totalExpense
                val netPfx = if (netBal >= 0) "+" else ""
                sb.appendLine("\"Balance Neto:\",\"$netPfx$currencySymbol ${String.format("%.2f", netBal)}\"")
                sb.appendLine()
                sb.appendLine("\"--- DETALLE DE MOVIMIENTOS ---\"")
                sb.appendLine("\"Fecha\",\"Descripción\",\"Categoría/Origen\",\"Tipo\",\"Signo\",\"Monto\",\"Moneda\"")
                transactions.forEach { tx ->
                    val isConversion = tx.origin == "Ajuste de Moneda"
                    val typeStr = when {
                        isConversion                       -> "Conversión"
                        tx.origin == "Transferencia" || tx.origin == "Transferencia Externa" -> "Transferencia"
                        tx.type == TransactionType.INCOME  -> "Ingreso"
                        tx.type == TransactionType.EXPENSE -> "Gasto"
                        else                               -> "Transferencia"
                    }
                    val sign = when {
                        isConversion || tx.type == TransactionType.TRANSFER
                            || tx.origin == "Transferencia" || tx.origin == "Transferencia Externa" -> ""
                        tx.type == TransactionType.INCOME  -> "+"
                        else                               -> "-"
                    }
                    val txSymbol = if (isConversion) getCurrencySymbol(tx.currencyCode) else currencySymbol
                    val date = dateFmt.format(java.util.Date(tx.timestamp))
                    val desc = tx.description.replace("\"", "\"\"")
                    val origin = tx.origin.replace("\"", "\"\"")
                    sb.appendLine("\"$date\",\"$desc\",\"$origin\",\"$typeStr\",\"$sign\",\"${String.format("%.2f", tx.amount)}\",\"$txSymbol\"")
                }
                sb.appendLine()
                sb.appendLine("\"\",\"\",\"\",\"\",\"Total Ingresos:\",\"${String.format("%.2f", totalIncome)}\",\"$currencySymbol\"")
                sb.appendLine("\"\",\"\",\"\",\"\",\"Total Gastos:\",\"${String.format("%.2f", totalExpense)}\",\"$currencySymbol\"")
                if (totalTransfer > 0) sb.appendLine("\"\",\"\",\"\",\"\",\"Total Transfer.:\",\"${String.format("%.2f", totalTransfer)}\",\"$currencySymbol\"")
                sb.appendLine("\"\",\"\",\"\",\"\",\"Balance Neto:\",\"$netPfx${String.format("%.2f", netBal)}\",\"$currencySymbol\"")
                context.contentResolver.openOutputStream(uri)?.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
                withContext(Dispatchers.Main) { _uiState.update { it.copy(errorMessage = "REPORT_CSV_OK") } }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _uiState.update { it.copy(errorMessage = "Error al generar CSV: ${e.message}") } }
            }
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa correo y contraseña") }
            return
        }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val userDoc = db.collection("users").document(email).get().await()
                if (userDoc.exists()) {
                    val user = userDoc.toObject(User::class.java)
                    if (user != null) {
                        val theme = try { AppTheme.valueOf(user.theme) } catch (e: Exception) { AppTheme.DEFAULT }
                        userPreferences.saveUserEmail(email)
                        _uiState.update { it.copy(currentUser = user, errorMessage = null, selectedTheme = theme) }
                        loadWallets()
                        loadTransactions()
                        loadCategories()
                        onSuccess()
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Perfil de usuario no encontrado") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al iniciar sesión: ${e.message}") }
            }
        }
    }

    fun selectWallet(wallet: Wallet) {
        val filtered = allTransactions.filter { it.walletId == wallet.id }
        _uiState.update { state ->
            state.copy(
                selectedWallet = wallet,
                transactions = filtered.sortedByDescending { t -> t.timestamp },
                balance = wallet.balance
            )
        }
        // No llama a calculateGlobalBalance(): cambiar de billetera no altera
        // ningún saldo, el total global no cambia. Llamarla causaba isLoading=true
        // que destruía toda la composición (pantalla blanca).
        calculateChartTotals()
        applyFilters()
    }

    fun setPreferredCurrency(currencyCode: String) {
        _uiState.update { it.copy(preferredCurrency = currencyCode) }
        calculateGlobalBalance()
    }

    private fun calculateGlobalBalance() {
        val state = _uiState.value
        val wallets = allWallets
        val targetCurrency = state.preferredCurrency
        if (wallets.isEmpty()) return

        // Cálculo silencioso en segundo plano — no toca isLoading para no bloquear la UI
        viewModelScope.launch {
            try {
                var rates: Map<String, Double> = emptyMap()
                try {
                    val response = withTimeout(5000) { currencyService.getCurrencyRate(targetCurrency) }
                    val fetchedRates = response.rates
                    if (fetchedRates != null) {
                        rates = fetchedRates
                        userPreferences.saveRates(gson.toJson(fetchedRates), System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    val cachedJson = userPreferences.cachedRates.first()
                    if (cachedJson != null) rates = gson.fromJson(cachedJson, Map::class.java) as Map<String, Double>
                }

                var total = 0.0
                for (wallet in wallets) {
                    if (wallet.currencyCode == targetCurrency) {
                        total += wallet.balance
                    } else {
                        val rateValue = rates[wallet.currencyCode]
                        val rateToTarget = when (rateValue) {
                            is Double -> rateValue
                            is Number -> rateValue.toDouble()
                            else -> 1.0
                        }
                        total += if (rateToTarget != 0.0) wallet.balance / rateToTarget else wallet.balance
                    }
                }
                _uiState.update { it.copy(globalBalance = total) }
            } catch (_: Exception) {}
        }
    }

    private fun calculateChartTotals() {
        val selectedWallet = _uiState.value.selectedWallet ?: return
        val targetCurrency = selectedWallet.currencyCode
        viewModelScope.launch {
            try {
                var rates: Map<String, Double> = emptyMap()
                try {
                    val response = withTimeout(5000) { currencyService.getCurrencyRate(targetCurrency) }
                    rates = response.rates ?: emptyMap()
                } catch (_: Exception) {
                    val cachedJson = userPreferences.cachedRates.first()
                    if (cachedJson != null) {
                        val type = object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type
                        rates = gson.fromJson(cachedJson, type)
                    }
                }

                var income = 0.0
                var expense = 0.0
                val convertedTransactions = allTransactions.filter { it.walletId == selectedWallet.id }.map { tx ->
                    val convertedAmount = if (tx.currencyCode == targetCurrency) tx.amount else {
                        val rate = rates[tx.currencyCode] ?: 1.0
                        tx.amount / rate
                    }
                    tx.copy(amount = convertedAmount, currencyCode = targetCurrency)
                }

                convertedTransactions.forEach { tx ->
                    when (tx.type) {
                        TransactionType.INCOME -> income += tx.amount
                        TransactionType.EXPENSE -> expense += tx.amount
                        TransactionType.TRANSFER -> {
                            // Legacy TRANSFER records: determine direction by description
                            val desc = tx.description.lowercase()
                            if (desc.startsWith("de ") || desc.startsWith("transferencia de ")) {
                                income += tx.amount
                            } else {
                                expense += tx.amount
                            }
                        }
                    }
                }
                _uiState.update { it.copy(chartIncome = income, chartExpense = expense, convertedTransactions = convertedTransactions) }
            } catch (_: Exception) {}
        }
    }

    fun loadWallets() {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(email).collection("wallets").get().await()
                val wallets = snapshot.toObjects(Wallet::class.java)
                allWallets.clear()
                allWallets.addAll(wallets)
                // Migrate wallets missing accountId
                val missingId = wallets.filter { it.accountId.isBlank() }
                if (missingId.isNotEmpty()) {
                    val batch = db.batch()
                    missingId.forEach { w ->
                        val newId = com.upn3.proyecto_finanzas_personales.model.generateAccountId()
                        val idx = allWallets.indexOfFirst { it.id == w.id }
                        if (idx != -1) allWallets[idx] = allWallets[idx].copy(accountId = newId)
                        batch.update(db.collection("users").document(email).collection("wallets").document(w.id), "accountId", newId)
                    }
                    try { batch.commit().await() } catch (_: Exception) {}
                }
                if (allWallets.isEmpty()) {
                    createWallet(Wallet(id = "default", name = "Billetera Principal", currencyCode = "PEN", balance = 0.0))
                } else {
                    if (_uiState.value.selectedWallet == null) _uiState.update { it.copy(selectedWallet = allWallets.first()) }
                    updateState()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al cargar billeteras: ${e.message}") }
            }
        }
    }

    fun createWallet(wallet: Wallet) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("wallets").document(wallet.id).set(wallet).await()
                if (!allWallets.any { it.id == wallet.id }) allWallets.add(wallet)
                if (_uiState.value.selectedWallet == null) _uiState.update { it.copy(selectedWallet = wallet) }
                updateState()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al crear billetera: ${e.message}") }
            }
        }
    }

    fun updateWallet(wallet: Wallet) {
        val email = uiState.value.currentUser?.email ?: return
        val oldWallet = allWallets.find { it.id == wallet.id } ?: return
        val finalBalance = if (wallet.balance == 0.0 && (oldWallet.balance) > 0.0 && uiState.value.exchangeRatePreview == null) oldWallet.balance else wallet.balance
        val walletToSave = wallet.copy(balance = finalBalance)

        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("wallets").document(walletToSave.id).set(walletToSave).await()
                val index = allWallets.indexOfFirst { it.id == walletToSave.id }
                if (index != -1) allWallets[index] = walletToSave

                if (oldWallet.currencyCode != walletToSave.currencyCode) {
                    val description = "Conversión de saldo ${getCurrencySymbol(oldWallet.currencyCode)}${"%.2f".format(oldWallet.balance)} → ${getCurrencySymbol(walletToSave.currencyCode)}${"%.2f".format(walletToSave.balance)}"
                    val event = Transaction(amount = 0.0, description = description, origin = "Ajuste de Moneda", type = TransactionType.INCOME, walletId = walletToSave.id, currencyCode = walletToSave.currencyCode, timestamp = System.currentTimeMillis())
                    db.collection("users").document(email).collection("transactions").document(event.id).set(event).await()
                    allTransactions.add(0, event)
                }
                updateState()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar billetera") }
            }
        }
    }

    fun deleteWallet(walletId: String) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("wallets").document(walletId).delete().await()
                val transactionsQuery = db.collection("users").document(email).collection("transactions").whereEqualTo("walletId", walletId).get().await()
                if (!transactionsQuery.isEmpty) {
                    db.runBatch { batch -> transactionsQuery.documents.forEach { doc -> batch.delete(doc.reference) } }.await()
                }
                allWallets.removeAll { it.id == walletId }
                allTransactions.removeAll { it.walletId == walletId }
                if (_uiState.value.selectedWallet?.id == walletId) _uiState.update { it.copy(selectedWallet = allWallets.firstOrNull()) }
                updateState()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al eliminar billetera: ${e.message}") }
            }
        }
    }

    fun fetchExchangeRatePreview(fromCode: String, toCode: String) {
        if (fromCode == toCode) {
            _uiState.update { it.copy(exchangeRatePreview = 1.0, isExchangeLoading = false) }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExchangeLoading = true) }
                var rates: Map<String, Double> = emptyMap()
                try {
                    val response = withTimeout(5000) { currencyService.getCurrencyRate(toCode) }
                    val fetchedRates = response.rates
                    if (fetchedRates != null) {
                        rates = fetchedRates
                        userPreferences.saveRates(gson.toJson(fetchedRates), System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    val cachedJson = userPreferences.cachedRates.first()
                    if (cachedJson != null) {
                        val type = object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type
                        rates = gson.fromJson(cachedJson, type)
                    }
                }
                val rateValue = rates[fromCode]
                val rateToSource = when (rateValue) {
                    is Double -> rateValue
                    is Number -> rateValue.toDouble()
                    else -> 1.0
                }
                val multiplier = if (rateToSource != 0.0) 1.0 / rateToSource else 1.0
                _uiState.update { it.copy(exchangeRatePreview = multiplier, isExchangeLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(exchangeRatePreview = 1.0, isExchangeLoading = false) }
            }
        }
    }

    fun clearExchangeRatePreview() {
        _uiState.update { it.copy(exchangeRatePreview = null) }
    }

    fun loadTransactions() {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(email).collection("transactions").orderBy("timestamp", Query.Direction.DESCENDING).get().await()
                allTransactions.clear()
                allTransactions.addAll(snapshot.toObjects(Transaction::class.java))
                updateState()
            } catch (e: Exception) {}
        }
    }

    fun loadCategories() {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(email).collection("categories").get().await()
                allCategories.clear()
                allCategories.addAll(snapshot.toObjects(Category::class.java))
                if (allCategories.isEmpty()) {
                    val defaultCategories = listOf(
                        Category(name = "Salario", type = TransactionType.INCOME),
                        Category(name = "Ventas", type = TransactionType.INCOME),
                        Category(name = "Comida", type = TransactionType.EXPENSE),
                        Category(name = "Transporte", type = TransactionType.EXPENSE),
                        Category(name = "Ocio", type = TransactionType.EXPENSE)
                    )
                    defaultCategories.forEach { addCategory(it) }
                } else {
                    updateState()
                }
            } catch (e: Exception) {}
        }
    }

    fun addCategory(category: Category) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("categories").document(category.id).set(category).await()
                allCategories.add(category)
                updateState()
            } catch (e: Exception) {}
        }
    }

    fun deleteCategory(id: String) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("categories").document(id).delete().await()
                allCategories.removeAll { it.id == id }
                updateState()
            } catch (e: Exception) {}
        }
    }

    private fun addAuditLog(email: String, transactionId: String, log: AuditLog) {
        viewModelScope.launch {
            try {
                db.collection("users").document(email)
                    .collection("transactions").document(transactionId)
                    .collection("audit_logs").document(log.id)
                    .set(log).await()
            } catch (e: Exception) {
                Log.w("FinanceViewModel", "Audit log failed: ${e.message}")
            }
        }
    }

    fun loadAuditLogs(transactionId: String) {
        val email = uiState.value.currentUser?.email ?: return
        _uiState.update { it.copy(isLoadingAuditLogs = true, auditLogs = emptyList()) }
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(email)
                    .collection("transactions").document(transactionId)
                    .collection("audit_logs")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get().await()
                val logs = snapshot.toObjects(AuditLog::class.java)
                _uiState.update { it.copy(auditLogs = logs, isLoadingAuditLogs = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(auditLogs = emptyList(), isLoadingAuditLogs = false) }
            }
        }
    }

    fun updateTransaction(transaction: Transaction, onSuccess: () -> Unit = {}) {
        val email = uiState.value.currentUser?.email ?: return
        val oldTransaction = allTransactions.find { it.id == transaction.id }
        val wallet = allWallets.find { it.id == transaction.walletId }
        val isAdjustment = transaction.origin == "Sistema"
        
        if (oldTransaction != null && wallet != null && !isAdjustment) {
            val balanceWithoutOld = wallet.balance - when(oldTransaction.type) {
                TransactionType.INCOME -> oldTransaction.amount
                TransactionType.EXPENSE, TransactionType.TRANSFER -> -oldTransaction.amount
            }
            val newBalance = balanceWithoutOld + when(transaction.type) {
                TransactionType.INCOME -> transaction.amount
                TransactionType.EXPENSE, TransactionType.TRANSFER -> -transaction.amount
            }
            if (newBalance < 0) {
                _uiState.update { it.copy(errorMessage = "Saldo insuficiente.") }
                return
            }
        }

        // Compute diff before coroutine so oldTransaction is still the original
        val auditChanges = mutableMapOf<String, Map<String, String>>()
        if (oldTransaction != null) {
            if (oldTransaction.amount != transaction.amount)
                auditChanges["monto"] = mapOf("old" to "%.2f".format(oldTransaction.amount), "new" to "%.2f".format(transaction.amount))
            if (oldTransaction.description != transaction.description)
                auditChanges["descripción"] = mapOf("old" to oldTransaction.description, "new" to transaction.description)
            if (oldTransaction.origin != transaction.origin)
                auditChanges["categoría"] = mapOf("old" to oldTransaction.origin, "new" to transaction.origin)
            if (oldTransaction.timestamp != transaction.timestamp)
                auditChanges["fecha"] = mapOf("old" to oldTransaction.timestamp.toString(), "new" to transaction.timestamp.toString())
        }
        val voucherAction = when {
            oldTransaction?.receiptPath == null && transaction.receiptPath != null -> AuditAction.VOUCHER_AGREGADO
            oldTransaction?.receiptPath != null && transaction.receiptPath == null -> AuditAction.VOUCHER_ELIMINADO
            oldTransaction?.receiptPath != null && transaction.receiptPath != null
                    && oldTransaction.receiptPath != transaction.receiptPath -> AuditAction.VOUCHER_REEMPLAZADO
            else -> null
        }
        if (voucherAction != null) {
            auditChanges["comprobante"] = mapOf(
                "old" to (oldTransaction?.receiptPath ?: ""),
                "new" to (transaction.receiptPath ?: "")
            )
        }
        val locationAction = when {
            oldTransaction?.latitude == null && transaction.latitude != null -> AuditAction.UBICACION_REGISTRADA
            oldTransaction?.latitude != null && transaction.latitude == null -> AuditAction.UBICACION_ELIMINADA
            oldTransaction?.latitude != null && transaction.latitude != null &&
                    (oldTransaction.latitude != transaction.latitude || oldTransaction.longitude != transaction.longitude) -> AuditAction.UBICACION_ACTUALIZADA
            else -> null
        }
        if (locationAction != null) {
            auditChanges["ubicación"] = mapOf(
                "old" to if (oldTransaction?.latitude != null) "${oldTransaction.latitude},${oldTransaction.longitude}" else "",
                "new" to if (transaction.latitude != null) "${transaction.latitude},${transaction.longitude}" else ""
            )
        }
        val auditAction = when {
            auditChanges.size == 1 && voucherAction != null && auditChanges.containsKey("comprobante") -> voucherAction
            auditChanges.size == 1 && locationAction != null && auditChanges.containsKey("ubicación") -> locationAction
            else -> AuditAction.EDICION
        }

        val updatedTransaction = transaction.copy(lastModified = System.currentTimeMillis())
        viewModelScope.launch {
            try {
                db.runBatch { batch ->
                    batch.set(db.collection("users").document(email).collection("transactions").document(transaction.id), updatedTransaction)
                    if (oldTransaction != null && wallet != null && !isAdjustment) {
                        val oldImpact = when(oldTransaction.type) {
                            TransactionType.INCOME -> oldTransaction.amount
                            TransactionType.EXPENSE, TransactionType.TRANSFER -> -oldTransaction.amount
                        }
                        val newImpact = when(transaction.type) {
                            TransactionType.INCOME -> transaction.amount
                            TransactionType.EXPENSE, TransactionType.TRANSFER -> -transaction.amount
                        }
                        val diff = newImpact - oldImpact
                        if (diff != 0.0) batch.update(db.collection("users").document(email).collection("wallets").document(wallet.id), "balance", wallet.balance + diff)
                    }
                }.await()

                val index = allTransactions.indexOfFirst { it.id == transaction.id }
                if (index != -1) allTransactions[index] = updatedTransaction
                if (oldTransaction != null && wallet != null && !isAdjustment) {
                    val oldImpact = when(oldTransaction.type) {
                        TransactionType.INCOME -> oldTransaction.amount
                        TransactionType.EXPENSE, TransactionType.TRANSFER -> -oldTransaction.amount
                    }
                    val newImpact = when(transaction.type) {
                        TransactionType.INCOME -> transaction.amount
                        TransactionType.EXPENSE, TransactionType.TRANSFER -> -transaction.amount
                    }
                    val wIndex = allWallets.indexOfFirst { it.id == wallet.id }
                    if (wIndex != -1) allWallets[wIndex] = allWallets[wIndex].copy(balance = allWallets[wIndex].balance + (newImpact - oldImpact))
                }
                updateState()
                addAuditLog(email, transaction.id, AuditLog(
                    transactionId = transaction.id,
                    action = auditAction,
                    userEmail = email,
                    changedFields = auditChanges
                ))
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar") }
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            auth.signOut()
            userPreferences.clearUserEmail()
            allTransactions.clear()
            _uiState.update { it.copy(currentUser = null, errorMessage = null, transactions = emptyList(), balance = 0.0, selectedTheme = AppTheme.DEFAULT) }
            onSuccess()
        }
    }

    fun deleteTransaction(id: String) {
        val email = uiState.value.currentUser?.email ?: return
        val transactionToDelete = allTransactions.find { it.id == id }
        val wallet = allWallets.find { it.id == transactionToDelete?.walletId }
        val isAdjustment = transactionToDelete?.origin == "Sistema"

        viewModelScope.launch {
            try {
                db.runBatch { batch ->
                    batch.delete(db.collection("users").document(email).collection("transactions").document(id))
                    if (transactionToDelete != null && wallet != null && !isAdjustment) {
                        val impact = when(transactionToDelete.type) {
                            TransactionType.INCOME -> -transactionToDelete.amount
                            TransactionType.EXPENSE, TransactionType.TRANSFER -> transactionToDelete.amount
                        }
                        batch.update(db.collection("users").document(email).collection("wallets").document(wallet.id), "balance", wallet.balance + impact)
                    }
                }.await()
                allTransactions.removeAll { it.id == id }
                if (transactionToDelete != null && wallet != null && !isAdjustment) {
                    val wIndex = allWallets.indexOfFirst { it.id == wallet.id }
                    if (wIndex != -1) {
                        val impact = when(transactionToDelete.type) {
                            TransactionType.INCOME -> -transactionToDelete.amount
                            TransactionType.EXPENSE, TransactionType.TRANSFER -> transactionToDelete.amount
                        }
                        allWallets[wIndex] = allWallets[wIndex].copy(balance = allWallets[wIndex].balance + impact)
                    }
                }
                updateState()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al eliminar") }
            }
        }
    }

    fun addTransaction(amount: Double, description: String, origin: String, type: TransactionType, walletId: String? = null, onSuccess: () -> Unit = {}) {
        addTransactionWithDate(amount, description, origin, type, System.currentTimeMillis(), walletId, null, onSuccess = onSuccess)
    }

    fun addTransactionWithDate(
        amount: Double,
        description: String,
        origin: String,
        type: TransactionType,
        timestamp: Long,
        walletId: String? = null,
        receiptPath: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        onSuccess: () -> Unit = {}
    ) {
        val email = uiState.value.currentUser?.email ?: return
        val targetWalletId = walletId ?: _uiState.value.selectedWallet?.id ?: "default"
        val wallet = allWallets.find { it.id == targetWalletId } ?: return

        if (type == TransactionType.EXPENSE && amount > wallet.balance) {
            _uiState.update { it.copy(errorMessage = "Saldo insuficiente.") }
            return
        }

        val transaction = Transaction(
            amount = amount,
            currencyCode = wallet.currencyCode,
            description = description,
            origin = origin,
            type = type,
            walletId = targetWalletId,
            timestamp = timestamp,
            receiptPath = receiptPath,
            latitude = latitude,
            longitude = longitude
        )
        viewModelScope.launch {
            try {
                db.runBatch { batch ->
                    batch.set(db.collection("users").document(email).collection("transactions").document(transaction.id), transaction)
                    val newBalance = when(type) {
                        TransactionType.INCOME -> wallet.balance + amount
                        TransactionType.EXPENSE, TransactionType.TRANSFER -> wallet.balance - amount
                    }
                    batch.update(db.collection("users").document(email).collection("wallets").document(targetWalletId), "balance", newBalance)
                }.await()
                allTransactions.add(0, transaction)
                val wIndex = allWallets.indexOfFirst { it.id == targetWalletId }
                if (wIndex != -1) allWallets[wIndex] = allWallets[wIndex].copy(balance = when(type) {
                    TransactionType.INCOME -> allWallets[wIndex].balance + amount
                    else -> allWallets[wIndex].balance - amount
                })
                updateState()
                addAuditLog(email, transaction.id, AuditLog(
                    transactionId = transaction.id,
                    action = AuditAction.CREACION,
                    userEmail = email
                ))
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al guardar") }
            }
        }
    }

    fun transferMoney(fromWallet: Wallet, toWallet: Wallet, amount: Double, onSuccess: () -> Unit = {}) {
        val email = uiState.value.currentUser?.email ?: return
        if (amount > fromWallet.balance) {
            _uiState.update { it.copy(errorMessage = "Saldo insuficiente.") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                var conversionRate = 1.0
                if (fromWallet.currencyCode != toWallet.currencyCode) {
                    var rates: Map<String, Double> = emptyMap()
                    try {
                        rates = withTimeout(5000) { currencyService.getCurrencyRate(toWallet.currencyCode).rates } ?: emptyMap()
                    } catch (e: Exception) {
                        val cachedJson = userPreferences.cachedRates.first()
                        if (cachedJson != null) rates = gson.fromJson(cachedJson, object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type)
                    }
                    val rateValue = rates[fromWallet.currencyCode]
                    conversionRate = if (rateValue != null && rateValue != 0.0) 1.0 / rateValue.toDouble() else 1.0
                }

                val convertedAmount = amount * conversionRate
                val expenseTrans = Transaction(amount = amount, currencyCode = fromWallet.currencyCode, description = "A ${toWallet.name}", origin = "Transferencia", type = TransactionType.EXPENSE, walletId = fromWallet.id)
                val incomeTrans = Transaction(amount = convertedAmount, currencyCode = toWallet.currencyCode, description = "De ${fromWallet.name}", origin = "Transferencia", type = TransactionType.INCOME, walletId = toWallet.id)

                db.runBatch { batch ->
                    val userRef = db.collection("users").document(email)
                    batch.set(userRef.collection("transactions").document(expenseTrans.id), expenseTrans)
                    batch.set(userRef.collection("transactions").document(incomeTrans.id), incomeTrans)
                    batch.update(userRef.collection("wallets").document(fromWallet.id), "balance", fromWallet.balance - amount)
                    batch.update(userRef.collection("wallets").document(toWallet.id), "balance", toWallet.balance + convertedAmount)
                }.await()

                allTransactions.add(0, expenseTrans)
                allTransactions.add(0, incomeTrans)
                val fIdx = allWallets.indexOfFirst { it.id == fromWallet.id }
                if (fIdx != -1) allWallets[fIdx] = allWallets[fIdx].copy(balance = allWallets[fIdx].balance - amount)
                val tIdx = allWallets.indexOfFirst { it.id == toWallet.id }
                if (tIdx != -1) allWallets[tIdx] = allWallets[tIdx].copy(balance = allWallets[tIdx].balance + convertedAmount)
                updateState()
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error en transferencia: ${e.message}", isLoading = false) }
            }
        }
    }

    fun addExternalTransfer(
        fromWallet: Wallet,
        amount: Double,
        contact: TransferContact,
        timestamp: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit = {}
    ) {
        val email = uiState.value.currentUser?.email ?: return
        if (amount > fromWallet.balance) {
            _uiState.update { it.copy(errorMessage = "Saldo insuficiente.") }
            return
        }
        val transaction = Transaction(
            amount = amount,
            currencyCode = fromWallet.currencyCode,
            description = "A ${contact.recipientName}",
            origin = "Transferencia Externa",
            type = TransactionType.EXPENSE,
            walletId = fromWallet.id,
            timestamp = timestamp,
            transferContact = contact
        )
        viewModelScope.launch {
            try {
                db.runBatch { batch ->
                    val userRef = db.collection("users").document(email)
                    batch.set(userRef.collection("transactions").document(transaction.id), transaction)
                    batch.update(userRef.collection("wallets").document(fromWallet.id), "balance", fromWallet.balance - amount)
                }.await()
                allTransactions.add(0, transaction)
                val fIdx = allWallets.indexOfFirst { it.id == fromWallet.id }
                if (fIdx != -1) allWallets[fIdx] = allWallets[fIdx].copy(balance = allWallets[fIdx].balance - amount)
                updateState()
                addAuditLog(email, transaction.id, AuditLog(
                    transactionId = transaction.id,
                    action = AuditAction.CREACION,
                    userEmail = email,
                    changedFields = mapOf(
                        "destinatario" to mapOf("old" to "", "new" to contact.recipientName),
                        "monto" to mapOf("old" to "", "new" to "%.2f".format(amount))
                    )
                ))
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error en transferencia: ${e.message}") }
            }
        }
    }

    fun adjustBalance(newBalance: Double) {
        val diff = newBalance - uiState.value.balance
        if (diff == 0.0) return
        addTransaction(kotlin.math.abs(diff), "Ajuste de Saldo", "Sistema", if (diff > 0) TransactionType.INCOME else TransactionType.EXPENSE)
    }

    fun resetTransactions(initialBalance: Double) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(email).collection("transactions").get().await()
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
                allTransactions.clear()
                if (initialBalance > 0) addTransaction(initialBalance, "Ajuste de Saldo", "Sistema", TransactionType.INCOME)
                updateState()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al reiniciar: ${e.message}") }
            }
        }
    }

    fun getCurrencySymbol(currencyCode: String): String {
        return when (currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "MXN" -> "Mex$"
            "CLP" -> "CLP$"
            "BRL" -> "R$"
            else -> "S/."
        }
    }

    // ── FASE 1: Presupuestos, Metas y Gastos Fijos ────────────────────────────

    fun loadPlanningData() {
        val email = uiState.value.currentUser?.email ?: return
        _uiState.update { it.copy(isLoadingBudgets = true) }
        viewModelScope.launch {
            try {
                val budgets = db.collection("users").document(email).collection("budgets")
                    .get().await().documents.mapNotNull { it.toObject(com.upn3.proyecto_finanzas_personales.model.Budget::class.java) }
                val goals = db.collection("users").document(email).collection("savings_goals")
                    .get().await().documents.mapNotNull { it.toObject(com.upn3.proyecto_finanzas_personales.model.SavingsGoal::class.java) }
                val fixed = db.collection("users").document(email).collection("fixed_expenses")
                    .get().await().documents.mapNotNull { it.toObject(com.upn3.proyecto_finanzas_personales.model.FixedExpense::class.java) }
                _uiState.update { it.copy(budgets = budgets, savingsGoals = goals, fixedExpenses = fixed, isLoadingBudgets = false) }
                checkAndExecuteFixedExpenses(fixed)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingBudgets = false) }
            }
        }
    }

    fun addBudget(budget: com.upn3.proyecto_finanzas_personales.model.Budget) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("budgets").document(budget.id).set(budget).await()
                _uiState.update { it.copy(budgets = it.budgets + budget) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al guardar presupuesto: ${e.message}") }
            }
        }
    }

    fun deleteBudget(id: String) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("budgets").document(id).delete().await()
                _uiState.update { it.copy(budgets = it.budgets.filter { b -> b.id != id }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al eliminar presupuesto: ${e.message}") }
            }
        }
    }

    fun addSavingsGoal(goal: com.upn3.proyecto_finanzas_personales.model.SavingsGoal) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("savings_goals").document(goal.id).set(goal).await()
                _uiState.update { it.copy(savingsGoals = it.savingsGoals + goal) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al guardar meta: ${e.message}") }
            }
        }
    }

    fun addDepositToGoal(goalId: String, amount: Double) {
        val email = uiState.value.currentUser?.email ?: return
        val goal = uiState.value.savingsGoals.find { it.id == goalId } ?: return
        val updated = goal.copy(currentAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount))
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("savings_goals").document(goalId).set(updated).await()
                _uiState.update { state -> state.copy(savingsGoals = state.savingsGoals.map { if (it.id == goalId) updated else it }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar meta: ${e.message}") }
            }
        }
    }

    fun deleteSavingsGoal(id: String) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("savings_goals").document(id).delete().await()
                _uiState.update { it.copy(savingsGoals = it.savingsGoals.filter { g -> g.id != id }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al eliminar meta: ${e.message}") }
            }
        }
    }

    fun addFixedExpense(expense: com.upn3.proyecto_finanzas_personales.model.FixedExpense) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("fixed_expenses").document(expense.id).set(expense).await()
                _uiState.update { it.copy(fixedExpenses = it.fixedExpenses + expense) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al guardar gasto fijo: ${e.message}") }
            }
        }
    }

    fun toggleFixedExpense(id: String, isActive: Boolean) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("fixed_expenses").document(id).update("isActive", isActive).await()
                _uiState.update { state -> state.copy(fixedExpenses = state.fixedExpenses.map { if (it.id == id) it.copy(isActive = isActive) else it }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar gasto fijo: ${e.message}") }
            }
        }
    }

    fun deleteFixedExpense(id: String) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("fixed_expenses").document(id).delete().await()
                _uiState.update { it.copy(fixedExpenses = it.fixedExpenses.filter { e -> e.id != id }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al eliminar gasto fijo: ${e.message}") }
            }
        }
    }

    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(userSearchResults = emptyList(), isSearchingUsers = false) }
            return
        }
        val currentEmail = uiState.value.currentUser?.email ?: return
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearchingUsers = true) }
            try {
                val q = query.lowercase().trim()
                val snapshot = db.collection("users").get().await()
                val results = snapshot.documents.mapNotNull { doc ->
                    val docEmail = doc.id
                    if (docEmail == currentEmail) return@mapNotNull null
                    val name = "${doc.getString("name") ?: ""} ${doc.getString("lastname") ?: ""}".trim()
                    val displayName = name.ifBlank { docEmail }
                    if (docEmail.lowercase().contains(q) || name.lowercase().contains(q)) {
                        UserSearchResult(email = docEmail, displayName = displayName)
                    } else null
                }
                _uiState.update { it.copy(userSearchResults = results, isSearchingUsers = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingUsers = false, userSearchResults = emptyList()) }
            }
        }
    }

    fun clearUserSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(userSearchResults = emptyList(), isSearchingUsers = false) }
    }

    fun findWalletByAccountId(
        accountId: String,
        onResult: (WalletLookupResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val snap = db.collectionGroup("wallets")
                    .whereEqualTo("accountId", accountId)
                    .get().await()
                if (snap.isEmpty) {
                    onError("No se encontró ninguna billetera con ese número de cuenta.")
                    return@launch
                }
                val doc = snap.documents.first()
                val wallet = doc.toObject(Wallet::class.java) ?: run {
                    onError("Error al leer la billetera.")
                    return@launch
                }
                val ownerEmail = doc.reference.parent.parent?.id ?: run {
                    onError("No se pudo identificar al titular.")
                    return@launch
                }
                val currentEmail = uiState.value.currentUser?.email ?: ""
                if (ownerEmail == currentEmail) {
                    onError("No puedes transferir a tu propia billetera por este medio.")
                    return@launch
                }
                val userDoc = db.collection("users").document(ownerEmail).get().await()
                val name = "${userDoc.getString("name") ?: ""} ${userDoc.getString("lastname") ?: ""}".trim()
                val ownerName = name.ifBlank { ownerEmail }
                onResult(WalletLookupResult(ownerEmail = ownerEmail, ownerName = ownerName, wallet = wallet))
            } catch (e: Exception) {
                onError("Error al buscar la billetera: ${e.localizedMessage}")
            }
        }
    }

    fun getUserWallets(email: String, onResult: (List<Wallet>) -> Unit) {
        viewModelScope.launch {
            try {
                val snap = db.collection("users").document(email).collection("wallets").get().await()
                onResult(snap.toObjects(Wallet::class.java))
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }

    fun transferToUser(
        fromWallet: Wallet,
        toEmail: String,
        toWallet: Wallet,
        amount: Double,
        description: String,
        onSuccess: () -> Unit = {}
    ) {
        val email = uiState.value.currentUser?.email ?: return
        if (amount > fromWallet.balance) {
            _uiState.update { it.copy(errorMessage = "Saldo insuficiente en ${fromWallet.name}.") }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                var conversionRate = 1.0
                if (fromWallet.currencyCode != toWallet.currencyCode) {
                    var rates: Map<String, Double> = emptyMap()
                    try {
                        rates = withTimeout(5000) { currencyService.getCurrencyRate(toWallet.currencyCode).rates } ?: emptyMap()
                    } catch (e: Exception) {
                        val cachedJson = userPreferences.cachedRates.first()
                        if (cachedJson != null) rates = gson.fromJson(cachedJson, object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type)
                    }
                    val rv = rates[fromWallet.currencyCode]
                    conversionRate = if (rv != null && rv != 0.0) 1.0 / rv.toDouble() else 1.0
                }
                val convertedAmount = amount * conversionRate
                val motivo = description.trim()
                val senderDesc = "Transferencia a $toEmail${if (motivo.isNotBlank()) " — $motivo" else ""}"
                val receiverDesc = "Transferencia de $email${if (motivo.isNotBlank()) " — $motivo" else ""}"
                val expenseTrans = Transaction(
                    amount = amount, currencyCode = fromWallet.currencyCode,
                    description = senderDesc, origin = "Transferencia",
                    type = TransactionType.EXPENSE, walletId = fromWallet.id,
                    transferContact = TransferContact(recipientName = toEmail, recipientAlias = toWallet.accountId, bank = "", motivo = motivo)
                )
                val incomeTrans = Transaction(
                    amount = convertedAmount, currencyCode = toWallet.currencyCode,
                    description = receiverDesc, origin = "Transferencia",
                    type = TransactionType.INCOME, walletId = toWallet.id
                )
                val newFromBalance = fromWallet.balance - amount
                val newToBalance = toWallet.balance + convertedAmount
                val traceFields = mapOf(
                    "fromUser" to mapOf("value" to email),
                    "toUser" to mapOf("value" to toEmail),
                    "fromWallet" to mapOf("value" to "${fromWallet.name} (#${fromWallet.accountId})"),
                    "toWallet" to mapOf("value" to "${toWallet.name} (#${toWallet.accountId})"),
                    "originalAmount" to mapOf("value" to "$amount ${fromWallet.currencyCode}"),
                    "convertedAmount" to mapOf("value" to "$convertedAmount ${toWallet.currencyCode}"),
                    "conversionRate" to mapOf("value" to conversionRate.toString()),
                    "description" to mapOf("value" to motivo)
                )
                db.runBatch { batch ->
                    val senderRef = db.collection("users").document(email)
                    val receiverRef = db.collection("users").document(toEmail)
                    batch.set(senderRef.collection("transactions").document(expenseTrans.id), expenseTrans)
                    batch.update(senderRef.collection("wallets").document(fromWallet.id), "balance", newFromBalance)
                    batch.set(receiverRef.collection("transactions").document(incomeTrans.id), incomeTrans)
                    batch.update(receiverRef.collection("wallets").document(toWallet.id), "balance", newToBalance)
                }.await()
                allTransactions.add(0, expenseTrans)
                val fIdx = allWallets.indexOfFirst { it.id == fromWallet.id }
                if (fIdx != -1) allWallets[fIdx] = allWallets[fIdx].copy(balance = newFromBalance)
                updateState()
                // Trazabilidad emisor
                addAuditLog(email, expenseTrans.id, AuditLog(transactionId = expenseTrans.id, action = AuditAction.TRANSFERENCIA, userEmail = email, changedFields = traceFields))
                // Trazabilidad receptor (escribe directamente en su colección)
                val receiverLog = AuditLog(transactionId = incomeTrans.id, action = AuditAction.TRANSFERENCIA, userEmail = toEmail, changedFields = traceFields)
                try {
                    db.collection("users").document(toEmail)
                        .collection("transactions").document(incomeTrans.id)
                        .collection("audit_logs").document(receiverLog.id)
                        .set(receiverLog).await()
                } catch (_: Exception) {}
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error en transferencia: ${e.message}", isLoading = false) }
            }
        }
    }

    fun updateBudget(budget: com.upn3.proyecto_finanzas_personales.model.Budget) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("budgets").document(budget.id).set(budget).await()
                _uiState.update { s -> s.copy(budgets = s.budgets.map { if (it.id == budget.id) budget else it }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar presupuesto") }
            }
        }
    }

    fun updateSavingsGoal(goal: com.upn3.proyecto_finanzas_personales.model.SavingsGoal) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("savings_goals").document(goal.id).set(goal).await()
                _uiState.update { s -> s.copy(savingsGoals = s.savingsGoals.map { if (it.id == goal.id) goal else it }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar meta") }
            }
        }
    }

    fun updateFixedExpense(expense: com.upn3.proyecto_finanzas_personales.model.FixedExpense) {
        val email = uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(email).collection("fixed_expenses").document(expense.id).set(expense).await()
                _uiState.update { s -> s.copy(fixedExpenses = s.fixedExpenses.map { if (it.id == expense.id) expense else it }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar gasto fijo") }
            }
        }
    }

    fun depositToGoalFromWallet(
        goalId: String,
        fromWalletId: String,
        amountInWalletCurrency: Double,
        onSuccess: () -> Unit = {}
    ) {
        val email = uiState.value.currentUser?.email ?: return
        val goal = _uiState.value.savingsGoals.find { it.id == goalId } ?: return
        val fromWallet = allWallets.find { it.id == fromWalletId } ?: return
        if (amountInWalletCurrency > fromWallet.balance) {
            _uiState.update { it.copy(errorMessage = "Saldo insuficiente en ${fromWallet.name}") }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                var amountInGoalCurrency = amountInWalletCurrency
                if (fromWallet.currencyCode != goal.currencyCode) {
                    var rates: Map<String, Double> = emptyMap()
                    try {
                        rates = withTimeout(5000) { currencyService.getCurrencyRate(goal.currencyCode).rates } ?: emptyMap()
                    } catch (_: Exception) {
                        val cachedJson = userPreferences.cachedRates.first()
                        if (cachedJson != null) rates = gson.fromJson(cachedJson, object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type)
                    }
                    val rate = rates[fromWallet.currencyCode]
                    if (rate != null && rate != 0.0) amountInGoalCurrency = amountInWalletCurrency / rate
                }
                val symFrom = getCurrencySymbol(fromWallet.currencyCode)
                val symGoal = getCurrencySymbol(goal.currencyCode)
                val desc = if (fromWallet.currencyCode != goal.currencyCode)
                    "Depósito meta \"${goal.name}\" ($symFrom${String.format("%.2f", amountInWalletCurrency)} → $symGoal${String.format("%.2f", amountInGoalCurrency)})"
                else
                    "Depósito en meta \"${goal.name}\""
                val tx = Transaction(
                    amount = amountInWalletCurrency, currencyCode = fromWallet.currencyCode,
                    description = desc, origin = "Meta de Ahorro",
                    type = TransactionType.EXPENSE, walletId = fromWallet.id
                )
                val newGoal = goal.copy(currentAmount = (goal.currentAmount + amountInGoalCurrency).coerceAtMost(goal.targetAmount))
                val newBalance = fromWallet.balance - amountInWalletCurrency
                db.runBatch { batch ->
                    val ref = db.collection("users").document(email)
                    batch.set(ref.collection("transactions").document(tx.id), tx)
                    batch.update(ref.collection("wallets").document(fromWallet.id), "balance", newBalance)
                    batch.set(ref.collection("savings_goals").document(goalId), newGoal)
                }.await()
                allTransactions.add(0, tx)
                val wIdx = allWallets.indexOfFirst { it.id == fromWallet.id }
                if (wIdx != -1) allWallets[wIdx] = allWallets[wIdx].copy(balance = newBalance)
                _uiState.update { s -> s.copy(savingsGoals = s.savingsGoals.map { if (it.id == goalId) newGoal else it }) }
                updateState()
                addAuditLog(email, tx.id, AuditLog(transactionId = tx.id, action = AuditAction.CREACION, userEmail = email))
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al depositar: ${e.message}", isLoading = false) }
            }
        }
    }

    fun executeFixedExpense(expenseId: String, onSuccess: () -> Unit = {}) {
        val email = uiState.value.currentUser?.email ?: return
        val expense = _uiState.value.fixedExpenses.find { it.id == expenseId } ?: return
        val wallet = allWallets.find { it.id == expense.walletId } ?: return
        viewModelScope.launch {
            try {
                var deduction = expense.amount
                if (expense.currencyCode != wallet.currencyCode) {
                    var rates: Map<String, Double> = emptyMap()
                    try {
                        rates = withTimeout(5000) { currencyService.getCurrencyRate(wallet.currencyCode).rates } ?: emptyMap()
                    } catch (_: Exception) {
                        val cachedJson = userPreferences.cachedRates.first()
                        if (cachedJson != null) rates = gson.fromJson(cachedJson, object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type)
                    }
                    val rate = rates[expense.currencyCode]
                    if (rate != null && rate != 0.0) deduction = expense.amount / rate
                }
                if (deduction > wallet.balance) {
                    _uiState.update { it.copy(errorMessage = "Saldo insuficiente para \"${expense.description}\"") }
                    return@launch
                }
                val symE = getCurrencySymbol(expense.currencyCode)
                val symW = getCurrencySymbol(wallet.currencyCode)
                val desc = if (expense.currencyCode != wallet.currencyCode)
                    "${expense.description} ($symE${String.format("%.2f", expense.amount)} → $symW${String.format("%.2f", deduction)})"
                else expense.description
                val tx = Transaction(
                    amount = deduction, currencyCode = wallet.currencyCode,
                    description = desc, origin = expense.categoryName,
                    type = TransactionType.EXPENSE, walletId = wallet.id
                )
                val newBalance = wallet.balance - deduction
                db.runBatch { batch ->
                    val ref = db.collection("users").document(email)
                    batch.set(ref.collection("transactions").document(tx.id), tx)
                    batch.update(ref.collection("wallets").document(wallet.id), "balance", newBalance)
                }.await()
                allTransactions.add(0, tx)
                val wIdx = allWallets.indexOfFirst { it.id == wallet.id }
                if (wIdx != -1) allWallets[wIdx] = allWallets[wIdx].copy(balance = newBalance)
                updateState()
                addAuditLog(email, tx.id, AuditLog(transactionId = tx.id, action = AuditAction.CREACION, userEmail = email))
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al ejecutar gasto fijo: ${e.message}") }
            }
        }
    }

    private fun checkAndExecuteFixedExpenses(fixedExpenses: List<com.upn3.proyecto_finanzas_personales.model.FixedExpense>) {
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        val thisMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
        fixedExpenses.filter { it.isActive && it.dayOfMonth == today }.forEach { expense ->
            val alreadyExecuted = allTransactions.any { tx ->
                tx.walletId == expense.walletId &&
                tx.origin == expense.categoryName &&
                tx.description.startsWith(expense.description) &&
                java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp)) == thisMonth
            }
            if (!alreadyExecuted) executeFixedExpense(expense.id)
        }
    }

    private fun updateState() {
        val currentId = _uiState.value.selectedWallet?.id
        val updatedWallet = allWallets.find { it.id == currentId }
            ?: _uiState.value.selectedWallet
            ?: allWallets.firstOrNull()
        val filtered = if (updatedWallet != null) allTransactions.filter { it.walletId == updatedWallet.id } else allTransactions
        _uiState.update { it.copy(
            transactions = filtered.sortedByDescending { t -> t.timestamp },
            allWalletTransactions = allTransactions.toList(),
            categories = allCategories.toList(),
            wallets = allWallets.toList(),
            selectedWallet = updatedWallet,
            balance = updatedWallet?.balance ?: allWallets.sumOf { w -> w.balance }
        ) }
        applyFilters()
        calculateGlobalBalance()
        calculateChartTotals()
    }
}
