package com.upn3.proyecto_finanzas_personales.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upn3.proyecto_finanzas_personales.model.Category
import com.upn3.proyecto_finanzas_personales.model.TransactionType
import com.upn3.proyecto_finanzas_personales.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf(TransactionType.EXPENSE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GESTIONAR CATEGORÍAS") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newCategoryName = ""
                newCategoryType = TransactionType.EXPENSE
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Categoría")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Ingresos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            CategoryList(
                categories = uiState.categories.filter { it.type == TransactionType.INCOME },
                onDelete = { viewModel.deleteCategory(it.id) },
                onEdit = { editingCategory = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Gastos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            CategoryList(
                categories = uiState.categories.filter { it.type == TransactionType.EXPENSE },
                onDelete = { viewModel.deleteCategory(it.id) },
                onEdit = { editingCategory = it }
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nueva Categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = newCategoryType == TransactionType.INCOME,
                            onClick = { newCategoryType = TransactionType.INCOME }
                        )
                        Text("Ingreso")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = newCategoryType == TransactionType.EXPENSE,
                            onClick = { newCategoryType = TransactionType.EXPENSE }
                        )
                        Text("Gasto")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        viewModel.addCategory(Category(name = newCategoryName, type = newCategoryType))
                        newCategoryName = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    editingCategory?.let { cat ->
        var editName by remember(cat.id) { mutableStateOf(cat.name) }
        var editType by remember(cat.id) { mutableStateOf(cat.type) }

        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Editar Categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = editType == TransactionType.INCOME,
                            onClick = { editType = TransactionType.INCOME }
                        )
                        Text("Ingreso")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = editType == TransactionType.EXPENSE,
                            onClick = { editType = TransactionType.EXPENSE }
                        )
                        Text("Gasto")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateCategory(cat, cat.copy(name = editName.trim(), type = editType))
                        editingCategory = null
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CategoryList(
    categories: List<Category>,
    onDelete: (Category) -> Unit,
    onEdit: (Category) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (categories.isEmpty()) {
            Text("No hay categorías", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column {
                categories.forEach { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onEdit(category) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDelete(category) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                    if (category != categories.last()) HorizontalDivider()
                }
            }
        }
    }
}
