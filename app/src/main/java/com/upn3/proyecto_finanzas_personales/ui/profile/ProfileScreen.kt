package com.upn3.proyecto_finanzas_personales.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.net.Uri
import com.yalantis.ucrop.UCrop
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import java.io.File
import com.upn3.proyecto_finanzas_personales.ui.auth.SharedAuthTextField
import com.upn3.proyecto_finanzas_personales.ui.components.ThemeSelector
import com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToNotas: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.currentUser

    var name by remember { mutableStateOf(user?.name ?: "") }
    var lastname by remember { mutableStateOf(user?.lastname ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var profilePic by remember { mutableStateOf(user?.profilePicture ?: "") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val cropLauncher = rememberLauncherForActivityResult(
        contract = StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let {
                viewModel.uploadProfilePicture(
                    context,
                    it
                ) { imageUrl ->
                    profilePic = imageUrl
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val destinationUri = Uri.fromFile(
                File(
                    context.cacheDir,
                    "cropped_${System.currentTimeMillis()}.jpg"
                )
            )

            val options = UCrop.Options().apply {
                setCircleDimmedLayer(true)
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false)
            }

            val intent = UCrop.of(
                it,
                destinationUri
            )
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(800, 800)
                .withOptions(options)
                .getIntent(context)

            cropLauncher.launch(intent)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.updateUser(name, lastname, email, "", profilePic) { onNavigateBack() } }) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), MaterialTheme.colorScheme.surface)
                        )
                    )
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(100.dp)) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePic.isEmpty()) {
                                Text(
                                    (name.firstOrNull() ?: "U").toString().uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(profilePic).crossfade(true).build(),
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("$name $lastname".trim().ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (email.isNotEmpty()) {
                            Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                ProfileSection(title = "Información Personal", icon = Icons.Default.Person) {
                    SharedAuthTextField(value = name, onValueChange = { name = it }, label = "Nombre", icon = Icons.Default.Badge)
                    SharedAuthTextField(value = lastname, onValueChange = { lastname = it }, label = "Apellidos", icon = Icons.Default.Badge)
                    SharedAuthTextField(value = email, onValueChange = { email = it }, label = "Correo Electrónico", icon = Icons.Default.Email)
                }

                ProfileSection(title = "Seguridad", icon = Icons.Default.Lock) {
                    Surface(
                        onClick = { showChangePasswordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Cambiar Contraseña", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Requiere la contraseña actual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                ProfileSection(title = "Apariencia", icon = Icons.Default.Palette) {
                    ThemeSelector(currentTheme = uiState.selectedTheme, onThemeSelected = { viewModel.selectTheme(it) }, showLabel = false)
                }

                ProfileSection(title = "Planificación Financiera", icon = Icons.Default.AccountBalance) {
                    Surface(
                        onClick = onNavigateToBudgets,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.PieChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Presupuestos & Metas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Límites de gasto, ahorro y gastos fijos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        onClick = onNavigateToCategories,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Gestión de Categorías", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Crear, editar y eliminar categorías", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                ProfileSection(title = "Notas", icon = Icons.Default.Notes) {
                    Surface(
                        onClick = onNavigateToNotas,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Mis Notas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Apuntes y recordatorios financieros", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                ProfileSection(title = "Datos y Copia de Seguridad", icon = Icons.Default.Backup) {
                    CsvDataManagement(viewModel)
                }

                if (uiState.errorMessage != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
                        Text(uiState.errorMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showChangePasswordDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var repeatPass by remember { mutableStateOf("") }
        var showOld by remember { mutableStateOf(false) }
        var showNew by remember { mutableStateOf(false) }
        var showRepeat by remember { mutableStateOf(false) }
        var dialogError by remember { mutableStateOf<String?>(null) }
        var isChanging by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isChanging) showChangePasswordDialog = false },
            title = { Text("Cambiar Contraseña", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it; dialogError = null },
                        label = { Text("Contraseña actual") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { showOld = !showOld }) {
                                Icon(if (showOld) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (showOld) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; dialogError = null },
                        label = { Text("Nueva contraseña") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { showNew = !showNew }) {
                                Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (showNew) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = repeatPass,
                        onValueChange = { repeatPass = it; dialogError = null },
                        label = { Text("Repetir nueva contraseña") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { showRepeat = !showRepeat }) {
                                Icon(if (showRepeat) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (showRepeat) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = repeatPass.isNotBlank() && repeatPass != newPass
                    )
                    if (repeatPass.isNotBlank() && repeatPass != newPass) {
                        Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                    dialogError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            oldPass.isBlank() -> dialogError = "Ingresa tu contraseña actual."
                            newPass.length < 6 -> dialogError = "La nueva contraseña debe tener al menos 6 caracteres."
                            newPass != repeatPass -> dialogError = "Las contraseñas no coinciden."
                            else -> {
                                isChanging = true
                                viewModel.changePassword(
                                    oldPass, newPass,
                                    onSuccess = { showChangePasswordDialog = false },
                                    onError = { dialogError = it; isChanging = false }
                                )
                            }
                        }
                    },
                    enabled = !isChanging
                ) {
                    if (isChanging) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Cambiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }, enabled = !isChanging) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProfileSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvDataManagement(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var encryptExport by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var showExportPassword by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var importPasswordInput by remember { mutableStateOf("") }
    var showImportPassword by remember { mutableStateOf(false) }

    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        when {
            msg.startsWith("EXPORT_OK:") -> {
                val parts = msg.split(":")
                val txCount = parts.getOrNull(1) ?: "?"
                val walletCount = parts.getOrNull(2) ?: "?"
                resultMessage = "Exportación exitosa: $txCount transacciones y $walletCount billeteras guardadas"
                resultIsError = false
                viewModel.clearError()
            }
            msg.startsWith("IMPORT_OK:") -> {
                val parts = msg.split(":")
                val txCount = parts.getOrNull(1) ?: "?"
                val walletCount = parts.getOrNull(2) ?: "?"
                resultMessage = "Importación exitosa: $txCount transacciones y $walletCount billeteras restauradas"
                resultIsError = false
                viewModel.clearError()
            }
            msg == "NEEDS_PASSWORD" -> {
                showPasswordDialog = true
                viewModel.clearError()
            }
            msg.startsWith("Error") -> {
                resultMessage = msg
                resultIsError = true
                viewModel.clearError()
            }
        }
    }

    val today = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(it, exportPassword.ifBlank { null }, context) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            viewModel.importData(it, null, context)
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; importPasswordInput = "" },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("Archivo protegido", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Este archivo está cifrado. Ingresa la contraseña para importarlo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = importPasswordInput,
                        onValueChange = { importPasswordInput = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { showImportPassword = !showImportPassword }) {
                                Icon(if (showImportPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (showImportPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingImportUri?.let { viewModel.importData(it, importPasswordInput, context) }
                        showPasswordDialog = false
                        importPasswordInput = ""
                    },
                    enabled = importPasswordInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Desbloquear") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; importPasswordInput = "" }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        if (resultMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (resultIsError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (resultIsError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = if (resultIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        resultMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (resultIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { resultMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Contenido del backup", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                BackupContentRow(icon = Icons.Default.SwapVert, label = "Transacciones", count = uiState.transactions.size)
                BackupContentRow(icon = Icons.Default.AccountBalanceWallet, label = "Billeteras", count = uiState.wallets.size)
                BackupContentRow(icon = Icons.Default.Category, label = "Categorías", count = uiState.categories.size)
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, null, modifier = Modifier.size(18.dp), tint = if (encryptExport) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            Text("Cifrar exportación", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Protege el archivo con contraseña AES-256", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = encryptExport, onCheckedChange = { encryptExport = it; if (!it) exportPassword = "" })
                }
                if (encryptExport) {
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { showExportPassword = !showExportPassword }) {
                                Icon(if (showExportPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (showExportPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                    if (exportPassword.isNotBlank()) {
                        val strength = when {
                            exportPassword.length >= 12 && exportPassword.any { it.isDigit() } && exportPassword.any { !it.isLetterOrDigit() } -> Triple("Fuerte", MaterialTheme.colorScheme.primary, 1f)
                            exportPassword.length >= 8 -> Triple("Media", Color(0xFFFFA726), 0.6f)
                            else -> Triple("Débil", MaterialTheme.colorScheme.error, 0.25f)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(progress = { strength.third }, modifier = Modifier.weight(1f).height(4.dp), color = strength.second, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            Text("Seguridad: ${strength.first}", style = MaterialTheme.typography.labelSmall, color = strength.second)
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val fileName = if (encryptExport) "finanzas_$today.enc.json" else "finanzas_$today.json"
                    exportLauncher.launch(fileName)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = !encryptExport || exportPassword.isNotBlank()
            ) {
                Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Exportar", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { importLauncher.launch("*/*") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Importar", fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            "Formato JSON · Compatible con versiones futuras · El cifrado usa AES-256",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BackupContentRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
            Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}
