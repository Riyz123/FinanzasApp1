package com.upn3.proyecto_finanzas_personales.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.upn3.proyecto_finanzas_personales.model.Transaction
import com.upn3.proyecto_finanzas_personales.model.TransactionType
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yalantis.ucrop.UCrop
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import java.io.File

import com.upn3.proyecto_finanzas_personales.ui.auth.SharedAuthTextField
import com.upn3.proyecto_finanzas_personales.ui.components.ThemeSelector
import com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.widget.Toast
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.currentUser

    var name by remember { mutableStateOf(user?.name ?: "") }
    var lastname by remember { mutableStateOf(user?.lastname ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var profilePic by remember { mutableStateOf(user?.profilePicture ?: "") }

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
                    TextButton(onClick = { viewModel.updateUser(name, lastname, email, password, profilePic) { onNavigateBack() } }) {
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
            // ── Avatar header ─────────────────────────────────────────────────
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

                // ── Información personal ──────────────────────────────────────
                ProfileSection(title = "Información Personal", icon = Icons.Default.Person) {
                    SharedAuthTextField(value = name, onValueChange = { name = it }, label = "Nombre", icon = Icons.Default.Badge)
                    SharedAuthTextField(value = lastname, onValueChange = { lastname = it }, label = "Apellidos", icon = Icons.Default.Badge)
                    SharedAuthTextField(value = email, onValueChange = { email = it }, label = "Correo Electrónico", icon = Icons.Default.Email)
                }

                // ── Seguridad ─────────────────────────────────────────────────
                ProfileSection(title = "Seguridad", icon = Icons.Default.Lock) {
                    SharedAuthTextField(value = password, onValueChange = { password = it }, label = "Nueva contraseña (dejar vacío para no cambiar)", icon = Icons.Default.Lock, isPassword = true)
                }

                // ── Apariencia ────────────────────────────────────────────────
                ProfileSection(title = "Apariencia", icon = Icons.Default.Palette) {
                    ThemeSelector(currentTheme = uiState.selectedTheme, onThemeSelected = { viewModel.selectTheme(it) }, showLabel = false)
                }

                // ── Datos ─────────────────────────────────────────────────────
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

/**
 * Componente para la gestión de Importación/Exportación CSV.
 * Permite al usuario elegir un archivo para importar o una ubicación para exportar.
 * Incluye un campo de contraseña opcional para cifrar los datos.
 */
@Composable
fun CsvDataManagement(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportPassword by remember { mutableStateOf("") }
    var showExportPassField by remember { mutableStateOf(false) }

    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportPassDialog by remember { mutableStateOf(false) }
    var importPasswordInput by remember { mutableStateOf("") }

    // Launcher para seleccionar ubicación de guardado (Exportar)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportTransactionsToCsv(it, exportPassword.ifBlank { null }, context) }
    }

    // Launcher para abrir archivo (Importar)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(selectedUri)?.bufferedReader()?.use { it.readText() } ?: ""
                    
                    // Comprobar si parece estar en Base64 (indicador de que está cifrado)
                    val isEncrypted = try {
                        val decoded = Base64.decode(content, Base64.DEFAULT)
                        // Si se pudo decodificar y tiene al menos el tamaño del IV (16 bytes)
                        decoded.size > 16
                    } catch (e: Exception) {
                        false
                    }

                    withContext(Dispatchers.Main) {
                        if (isEncrypted) {
                            importUri = selectedUri
                            showImportPassDialog = true
                        } else {
                            viewModel.importTransactionsFromCsv(selectedUri, null, context)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // El ViewModel manejará errores si llamamos a import con null y falla,
                        // pero aquí estamos leyendo previamente.
                        viewModel.importTransactionsFromCsv(selectedUri, null, context)
                    }
                }
            }
        }
    }

    if (showImportPassDialog) {
        AlertDialog(
            onDismissRequest = { 
                showImportPassDialog = false
                importPasswordInput = ""
            },
            title = { Text("Archivo Cifrado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Este archivo parece estar protegido. Ingresa la contraseña:")
                    OutlinedTextField(
                        value = importPasswordInput,
                        onValueChange = { importPasswordInput = it },
                        label = { Text("Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    importUri?.let { uri ->
                        viewModel.importTransactionsFromCsv(uri, importPasswordInput, context)
                    }
                    showImportPassDialog = false
                    importPasswordInput = ""
                }) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportPassDialog = false
                    importPasswordInput = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Configuración de Exportación", style = MaterialTheme.typography.titleSmall)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = showExportPassField,
                    onCheckedChange = { showExportPassField = it }
                )
                Text("Cifrar exportación", style = MaterialTheme.typography.bodyMedium)
            }

            if (showExportPassField) {
                OutlinedTextField(
                    value = exportPassword,
                    onValueChange = { exportPassword = it },
                    label = { Text("Contraseña de Cifrado") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { exportLauncher.launch("finanzas_backup.csv") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Exportar")
                }

                Button(
                    onClick = { importLauncher.launch("text/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Importar")
                }
            }
            
            Text(
                "Al importar, se detectará automáticamente si el archivo requiere contraseña.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
