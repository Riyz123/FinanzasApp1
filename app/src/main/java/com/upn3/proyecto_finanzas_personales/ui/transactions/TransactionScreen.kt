package com.upn3.proyecto_finanzas_personales.ui.transactions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.upn3.proyecto_finanzas_personales.location.LocationHelper
import com.upn3.proyecto_finanzas_personales.model.Category
import com.upn3.proyecto_finanzas_personales.model.TransactionType
import com.upn3.proyecto_finanzas_personales.ui.components.NumericKeyboard
import com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ── Estado de la captura de ubicación ────────────────────────────────────────

enum class LocationStatus {
    IDLE,        // Sin intentar aún
    CAPTURING,   // En proceso
    CAPTURED,    // Ubicación obtenida
    GPS_OFF,     // GPS/redes desactivados
    NO_PERMISSION, // Permiso denegado
    DENIED_PERMANENTLY, // Denegado permanentemente (ir a Ajustes)
    ERROR        // Fallo genérico
}

// ── Pantalla de transacciones ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // ── Estado de formulario ──────────────────────────────────────────────────
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedReceiptUri by remember { mutableStateOf<Uri?>(null) }

    // ── Estado de ubicación ───────────────────────────────────────────────────
    var locationStatus by remember { mutableStateOf(LocationStatus.IDLE) }
    var capturedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val locationHelper = remember { LocationHelper(context) }

    // ── Lanzadores ───────────────────────────────────────────────────────────

    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            UCrop.getOutput(result.data!!)?.let { resultUri ->
                val voucherDir = File(context.filesDir, "vouchers").also { it.mkdirs() }
                val voucherFile = File(voucherDir, "voucher_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(resultUri)?.use { input ->
                    voucherFile.outputStream().use { input.copyTo(it) }
                }
                selectedReceiptUri = Uri.fromFile(voucherFile)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val destUri = Uri.fromFile(File(context.cacheDir, "voucher_${System.currentTimeMillis()}.jpg"))
            val intent = UCrop.of(it, destUri)
                .withOptions(UCrop.Options().apply { setFreeStyleCropEnabled(true) })
                .getIntent(context)
            cropLauncher.launch(intent)
        }
    }

    // Lanzador de permisos de ubicación
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        when {
            fineGranted || coarseGranted -> {
                // Permiso concedido → capturar ubicación
                scope.launch {
                    locationStatus = LocationStatus.CAPTURING
                    if (!locationHelper.isLocationEnabled()) {
                        locationStatus = LocationStatus.GPS_OFF
                        showGpsDialog = true
                        return@launch
                    }
                    capturedLocation = locationHelper.getCurrentLocation()
                    locationStatus = if (capturedLocation != null)
                        LocationStatus.CAPTURED else LocationStatus.ERROR
                }
            }
            else -> {
                // Verificar si fue denegado permanentemente
                val showRationale = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
                locationStatus = if (showRationale)
                    LocationStatus.DENIED_PERMANENTLY else LocationStatus.NO_PERMISSION
            }
        }
    }

    // ── Al abrir la pantalla: solicitar permiso y capturar ubicación ──────────
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when {
            hasFine || hasCoarse -> {
                locationStatus = LocationStatus.CAPTURING
                if (!locationHelper.isLocationEnabled()) {
                    locationStatus = LocationStatus.GPS_OFF
                    showGpsDialog = true
                    return@LaunchedEffect
                }
                capturedLocation = locationHelper.getCurrentLocation()
                locationStatus = if (capturedLocation != null)
                    LocationStatus.CAPTURED else LocationStatus.ERROR
            }
            else -> {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // ── Date Picker ───────────────────────────────────────────────────────────
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            val d = selectedDate
            set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMs ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
                        selectedDate = Calendar.getInstance().apply {
                            set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // ── Diálogo: GPS apagado ──────────────────────────────────────────────────
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            icon = { Icon(Icons.Filled.LocationOff, null) },
            title = { Text("GPS desactivado") },
            text = { Text("La ubicación está desactivada. La transacción se guardará sin coordenadas. Activa el GPS para registrar la ubicación.") },
            confirmButton = {
                TextButton(onClick = {
                    showGpsDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Abrir Ajustes") }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) { Text("Continuar sin ubicación") }
            }
        )
    }

    // ── Diálogo: permiso denegado permanentemente ─────────────────────────────
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = { Icon(Icons.Filled.LocationDisabled, null) },
            title = { Text("Permiso de ubicación") },
            text = { Text("El permiso de ubicación fue denegado permanentemente. Para habilitarlo ve a Ajustes → Aplicaciones → Permisos.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) { Text("Abrir Ajustes") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ── UI Principal ──────────────────────────────────────────────────────────
    val filteredCategories = uiState.categories.filter { it.type == type }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NUEVA TRANSACCIÓN") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text(if (selectedReceiptUri == null) "📷 VOUCHER" else "✅ VOUCHER")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Contenido scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 4.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ── Selector Ingreso / Gasto ──────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(2.dp)
                ) {
                    Button(
                        onClick = { type = TransactionType.INCOME; selectedCategory = null },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (type == TransactionType.INCOME) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) { Text("INGRESO", style = MaterialTheme.typography.labelSmall) }

                    Button(
                        onClick = { type = TransactionType.EXPENSE; selectedCategory = null },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else Color.Transparent,
                            contentColor = if (type == TransactionType.EXPENSE) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) { Text("GASTO", style = MaterialTheme.typography.labelSmall) }
                }

                // ── Display del monto ─────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val symbol = viewModel.getCurrencySymbol(uiState.selectedWallet?.currencyCode ?: "PEN")
                        Text(
                            "MONTO ($symbol)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp)
                        )
                        Text(
                            text = if (amount.isEmpty()) "$symbol 0.00" else "$symbol $amount",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (type == TransactionType.INCOME)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                // ── Descripción ───────────────────────────────────────────────
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; viewModel.clearError() },
                    label = { Text("Descripción") },
                    placeholder = { Text("¿En qué se usó?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                // ── Fecha ─────────────────────────────────────────────────────
                OutlinedTextField(
                    value = dateFormatter.format(selectedDate.time),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // ── Indicador de ubicación ────────────────────────────────────
                LocationStatusIndicator(
                    status = locationStatus,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onOpenSettings = { showPermissionDialog = true },
                    onOpenGpsSettings = { showGpsDialog = true }
                )

                // ── Categorías ────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Categoría", style = MaterialTheme.typography.labelLarge)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredCategories) { category ->
                            val isSelected = selectedCategory == category
                            Surface(
                                onClick = { selectedCategory = category; viewModel.clearError() },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        item {
                            Surface(
                                onClick = onNavigateToCategories,
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                    Text("NUEVA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ── Panel fijo inferior: teclado + guardar ────────────────────────
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                NumericKeyboard(
                    onKeyPress = { key ->
                        if (key == "." && amount.contains(".")) return@NumericKeyboard
                        if (amount.contains(".") && amount.substringAfter(".").length >= 2) return@NumericKeyboard
                        if (amount.length < 10) { amount += key; viewModel.clearError() }
                    },
                    onDelete = { if (amount.isNotEmpty()) amount = amount.dropLast(1) },
                    onClear = { amount = "" }
                )

                Box(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    uiState.errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        when {
                            amt <= 0 -> viewModel.setError("El monto debe ser mayor a 0")
                            description.isBlank() -> viewModel.setError("La descripción es requerida")
                            selectedCategory == null -> viewModel.setError("Debe seleccionar una categoría")
                            else -> {
                                viewModel.addTransactionWithDate(
                                    amount = amt,
                                    description = description,
                                    origin = selectedCategory!!.name,
                                    type = type,
                                    timestamp = selectedDate.timeInMillis,
                                    receiptPath = selectedReceiptUri?.toString(),
                                    latitude = capturedLocation?.first,
                                    longitude = capturedLocation?.second
                                ) { onNavigateBack() }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("GUARDAR TRANSACCIÓN", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearError() }
    }
}

// ── Componente: Indicador de estado de ubicación ──────────────────────────────

@Composable
private fun LocationStatusIndicator(
    status: LocationStatus,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGpsSettings: () -> Unit
) {
    val (icon, text, tint, showAction, actionLabel) = when (status) {
        LocationStatus.IDLE -> LocationIndicatorData(
            Icons.Filled.LocationSearching, "Preparando ubicación...",
            MaterialTheme.colorScheme.onSurfaceVariant, false, ""
        )
        LocationStatus.CAPTURING -> LocationIndicatorData(
            Icons.Filled.LocationSearching, "Capturando ubicación GPS...",
            MaterialTheme.colorScheme.primary, false, ""
        )
        LocationStatus.CAPTURED -> LocationIndicatorData(
            Icons.Filled.LocationOn, "Ubicación capturada correctamente",
            Color(0xFF00C853), false, ""
        )
        LocationStatus.GPS_OFF -> LocationIndicatorData(
            Icons.Filled.LocationOff, "GPS desactivado — sin ubicación",
            Color(0xFFFF9800), true, "Activar GPS"
        )
        LocationStatus.NO_PERMISSION -> LocationIndicatorData(
            Icons.Filled.LocationDisabled, "Sin permiso de ubicación",
            MaterialTheme.colorScheme.onSurfaceVariant, true, "Solicitar permiso"
        )
        LocationStatus.DENIED_PERMANENTLY -> LocationIndicatorData(
            Icons.Filled.LocationDisabled, "Permiso denegado — ir a Ajustes",
            MaterialTheme.colorScheme.error, true, "Abrir Ajustes"
        )
        LocationStatus.ERROR -> LocationIndicatorData(
            Icons.Filled.LocationOff, "Sin ubicación disponible",
            MaterialTheme.colorScheme.onSurfaceVariant, false, ""
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (status == LocationStatus.CAPTURING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = tint
                )
            } else {
                Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = tint,
                modifier = Modifier.weight(1f)
            )
            if (showAction) {
                TextButton(
                    onClick = {
                        when (status) {
                            LocationStatus.NO_PERMISSION -> onRequestPermission()
                            LocationStatus.DENIED_PERMANENTLY -> onOpenSettings()
                            LocationStatus.GPS_OFF -> onOpenGpsSettings()
                            else -> {}
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}

private data class LocationIndicatorData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String,
    val tint: Color,
    val showAction: Boolean,
    val actionLabel: String
)
