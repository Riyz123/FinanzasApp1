# Documentación del Código — App Móvil Finanzas

**Paquete base:** `com.upn3.proyecto_finanzas_personales`  
**Ruta raíz:** `app/src/main/java/com/upn3/proyecto_finanzas_personales/`

---

## Índice

1. [Modelos (Data Classes)](#modelos)
2. [Capa de Datos (Room + DAOs)](#capa-de-datos)
3. [Red (Retrofit / Cloudinary / Monedas)](#red)
4. [ViewModel y Estado](#viewmodel-y-estado)
5. [Pantallas (UI Screens)](#pantallas)
6. [Componentes reutilizables](#componentes)
7. [Tema y Estilos](#tema)
8. [Utilidades](#utilidades)
9. [Entrada principal (MainActivity)](#mainactivity)

---

## Modelos

Ubicación: `model/`

### `Transaction`
Archivo: `model/Transaction.kt`

Representa una transacción financiera en memoria (Firestore / ViewModel). **No es una entidad Room**.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | UUID único |
| `walletId` | `String` | ID de la billetera asociada |
| `amount` | `Double` | Monto de la transacción |
| `currencyCode` | `String` | Código de moneda (ej. "PEN", "USD") |
| `description` | `String` | Descripción ingresada por el usuario |
| `origin` | `String` | Categoría o fuente (ej. "Comida", "Transferencia") |
| `type` | `TransactionType` | INCOME / EXPENSE / TRANSFER |
| `timestamp` | `Long` | Milisegundos epoch de la transacción |
| `lastModified` | `Long?` | Última modificación (null si nunca editada) |
| `receiptPath` | `String?` | URL Cloudinary del comprobante (null si no hay) |
| `latitude` | `Double?` | Latitud GPS (null si no capturada) |
| `longitude` | `Double?` | Longitud GPS (null si no capturada) |
| `transferContact` | `TransferContact?` | Datos del destinatario si es transferencia externa |

**Enum `TransactionType`:** `INCOME`, `EXPENSE`, `TRANSFER` (legacy)

**Dónde se usa:**
- `FinanceViewModel` — toda la lógica de negocio opera sobre `List<Transaction>`
- `DashboardScreen` — muestra las últimas transacciones del wallet activo
- `TransactionScreen` — lista filtrada de transacciones con búsqueda
- `ReportsScreen` — genera reportes y exporta a PDF/CSV
- `TransactionDetailDialog` — muestra detalle de una sola transacción

---

### `TransactionEntity`
Archivo: `model/TransactionEntity.kt`

Entidad Room para persistencia **offline local**. Tiene campos extra de transferencia que `Transaction` no tiene.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | UUID (coincide con Firestore) |
| `userEmail` | `String` | Email del dueño (clave de partición local) |
| `amount` | `Double` | Monto |
| `description` | `String` | Descripción |
| `origin` | `String` | Categoría o fuente |
| `type` | `String` | "INCOME" / "EXPENSE" / "TRANSFER" (guardado como texto) |
| `timestamp` | `Long` | Milisegundos epoch |
| `latitude` | `Double?` | GPS |
| `longitude` | `Double?` | GPS |
| `lastModified` | `Long?` | Última modificación |
| `receiptPath` | `String?` | URL comprobante |
| `recipientName` | `String?` | Nombre del destinatario de transferencia |
| `recipientAlias` | `String?` | Alias / cuenta destino |
| `recipientBank` | `String?` | Banco destino |
| `transferMotivo` | `String?` | Motivo declarado de la transferencia |

**Dónde se usa:** `TransactionDao` (CRUD Room), `AppDatabase`

---

### `Wallet`
Archivo: `model/Wallet.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | UUID de la billetera |
| `accountId` | `String` | Número de cuenta de 12 dígitos (generado con `generateAccountId()`) |
| `name` | `String` | Nombre de la billetera (ej. "BCP Soles") |
| `currencyCode` | `String` | Moneda principal (default "PEN") |
| `balance` | `Double` | Saldo actual |
| `color` | `Long` | Color ARGB como Long (default verde) |

**Función auxiliar:** `generateAccountId()` — genera un número aleatorio de 12 dígitos entre 100\_000\_000\_000 y 999\_999\_999\_999.

**Dónde se usa:**
- `FinanceViewModel` — `loadWallets()`, `createWallet()`, `updateWallet()`, `deleteWallet()`, `selectWallet()`
- `DashboardScreen` — HeroPager de billeteras, selector de wallet activa
- `ReportsScreen` — chips de filtro por billetera
- `BudgetScreen` — selector de wallet al crear presupuesto o gasto fijo

---

### `Category`
Archivo: `model/Category.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | UUID |
| `name` | `String` | Nombre de la categoría (ej. "Comida") |
| `type` | `TransactionType` | Si aplica a INCOME o EXPENSE |
| `icon` | `String` | Nombre del ícono Material (ej. "Restaurant") |

**Dónde se usa:**
- `FinanceViewModel` — `loadCategories()`, `addCategory()`, `deleteCategory()`
- `CategoryScreen` — gestión CRUD de categorías
- `DashboardScreen` — selector al agregar una transacción
- `TransactionDetailDialog` — muestra el ícono de la categoría

---

### `Budget`
Archivo: `model/Budget.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | UUID |
| `walletId` | `String` | Wallet a la que aplica |
| `categoryName` | `String` | Categoría sobre la que se limita el gasto |
| `limitAmount` | `Double` | Monto máximo del período |
| `period` | `String` | "MONTHLY" o "WEEKLY" |
| `currencyCode` | `String` | Moneda del límite |

**Entidad Room paralela:** `BudgetEntity` (mismos campos, se persiste offline).

**Dónde se usa:**
- `FinanceViewModel` — `addBudget()`, `updateBudget()`, `deleteBudget()`, `loadPlanningData()`
- `BudgetScreen` — pestaña "Presupuestos"

---

### `SavingsGoal`
Archivo: `model/SavingsGoal.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | UUID |
| `name` | `String` | Nombre de la meta (ej. "Viaje a París") |
| `targetAmount` | `Double` | Monto objetivo |
| `currentAmount` | `Double` | Monto acumulado hasta ahora |
| `deadline` | `Long` | Fecha límite en milisegundos epoch |
| `currencyCode` | `String` | Moneda de la meta |
| `color` | `String` | Color hex para identificación visual |

**Entidad Room paralela:** `SavingsGoalEntity`.

**Dónde se usa:**
- `FinanceViewModel` — `addSavingsGoal()`, `updateSavingsGoal()`, `deleteSavingsGoal()`, `addDepositToGoal()`, `depositToGoalFromWallet()`
- `BudgetScreen` — pestaña "Metas de Ahorro"

---

### `FixedExpense`
Archivo: `model/FixedExpense.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | UUID |
| `description` | `String` | Nombre del gasto fijo (ej. "Netflix") |
| `amount` | `Double` | Monto mensual |
| `categoryName` | `String` | Categoría asociada |
| `walletId` | `String` | Wallet desde la que se descuenta |
| `dayOfMonth` | `Int` | Día del mes en que se cobra |
| `currencyCode` | `String` | Moneda |
| `isActive` | `Boolean` | Si está activo o suspendido |

**Entidad Room paralela:** `FixedExpenseEntity`.

**Dónde se usa:**
- `FinanceViewModel` — `addFixedExpense()`, `updateFixedExpense()`, `deleteFixedExpense()`, `toggleFixedExpense()`, `executeFixedExpense()`
- `BudgetScreen` — pestaña "Gastos Fijos"

---

### `AuditLog`
Archivo: `model/AuditLog.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | UUID del log |
| `transactionId` | `String` | ID de la transacción auditada |
| `action` | `String` | Acción (constantes en `AuditAction`: "CREATED", "UPDATED", "DELETED") |
| `timestamp` | `Long` | Cuándo ocurrió |
| `userEmail` | `String` | Quién realizó la acción |
| `changedFields` | `Map<String, Map<String, String>>` | Mapa campo → { "old": valor, "new": valor } |

**Entidad Room paralela:** `AuditLogEntity` — `changedFields` se serializa como JSON en el campo `changedFieldsJson: String`.

**Dónde se usa:**
- `FinanceViewModel` — `loadAuditLogs()`, se genera audit automáticamente en `updateTransaction()` y `deleteTransaction()`
- `TransactionDetailDialog` — muestra el historial de cambios en la pestaña "Auditoría"
- `ActivityTimeline` — visualiza los logs como línea de tiempo

---

### `TransferContact`
Archivo: `model/TransferContact.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `recipientName` | `String` | Nombre del destinatario |
| `recipientAlias` | `String` | Alias o número de cuenta |
| `bank` | `String` | Banco destino |
| `motivo` | `String` | Motivo declarado de la transferencia |

**Dónde se usa:**
- `Transaction.transferContact` — embebido en la transacción de transferencia externa
- `FinanceViewModel.addExternalTransfer()` — construye y guarda el contacto
- `DashboardScreen` — se arma al confirmar una transferencia "A otra persona"

---

### `User`
Archivo: `model/User.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `email` | `String` | Email (usado como ID en Firestore) |
| `password` | `String` | Contraseña hasheada |
| `name` | `String` | Nombre |
| `lastname` | `String` | Apellido |
| `theme` | `String` | Tema visual seleccionado |
| `profilePicture` | `String` | URL de la foto de perfil (Cloudinary) |

**Dónde se usa:**
- `FinanceViewModel` — `login()`, `register()`, `updateUser()`, `logout()`
- `ProfileScreen` — edición de perfil

---

### `LanguageOption`
Archivo: `model/LanguageOption.kt`

| Campo | Tipo | Descripción |
|---|---|---|
| `code` | `String` | Código ISO (ej. "es", "en") |
| `name` | `String` | Nombre legible |
| `flag` | `String` | Emoji de bandera |
| `isInstalled` | `Boolean` | Si el pack de idioma está instalado |
| `canDownload` | `Boolean` | Si se puede descargar |
| `onlineOnly` | `Boolean` | Si solo funciona con internet |

**Dónde se usa:** `ProfileScreen` — selector de idioma.

---

## Capa de Datos

Ubicación: `data/`

### `AppDatabase`
Archivo: `data/AppDatabase.kt`

Base de datos Room con versión actual **4** (4 migraciones). Contiene las siguientes entidades:
- `TransactionEntity`
- `User`
- `AuditLogEntity`
- `BudgetEntity`
- `SavingsGoalEntity`
- `FixedExpenseEntity`

Expone los DAOs: `TransactionDao`, `AuditLogDao`, `PlanningDao`.

**Dónde se usa:** `FinanceViewModel` — se inyecta en el constructor para acceso offline.

---

### `TransactionDao`
Archivo: `data/TransactionDao.kt`

Interfaz Room DAO para `TransactionEntity`.

| Función | Descripción |
|---|---|
| `getAll(email)` | Retorna todas las transacciones del usuario |
| `insert(entity)` | Inserta o reemplaza una transacción |
| `delete(id)` | Elimina por ID |
| `deleteAll(email)` | Elimina todas las transacciones del usuario |

---

### `AuditLogDao`
Archivo: `data/AuditLogDao.kt`

| Función | Descripción |
|---|---|
| `getByTransactionId(txId)` | Retorna los logs de una transacción |
| `getByUser(email)` | Retorna todos los logs del usuario |
| `insert(entity)` | Inserta un log |
| `deleteByTransactionId(txId)` | Limpia logs al borrar una transacción |

---

### `PlanningDao`
Archivo: `data/PlanningDao.kt`

Maneja las tres tablas de planificación.

| Función | Descripción |
|---|---|
| `getBudgets(email)` | Lista presupuestos del usuario |
| `insertBudget(entity)` | Inserta/reemplaza presupuesto |
| `deleteBudget(id)` | Elimina presupuesto |
| `getGoals(email)` | Lista metas de ahorro |
| `insertGoal(entity)` | Inserta/reemplaza meta |
| `deleteGoal(id)` | Elimina meta |
| `getFixedExpenses(email)` | Lista gastos fijos |
| `insertFixedExpense(entity)` | Inserta/reemplaza gasto fijo |
| `deleteFixedExpense(id)` | Elimina gasto fijo |

---

### `UserPreferences`
Archivo: `data/UserPreferences.kt`

Gestión de preferencias persistentes con **DataStore** (no Room).

| Clave | Tipo | Descripción |
|---|---|---|
| email del usuario | `String` | Email de sesión activa |
| tasas de cambio | `String` (JSON) | Caché de tasas de conversión |
| idioma preferido | `String` | Código de idioma |
| timestamp de caché | `Long` | Cuándo se guardó el caché de tasas |

**Dónde se usa:** `FinanceViewModel` — para persistir sesión y caché de monedas entre reinicios.

---

## Red

Ubicación: `network/`

### `CurrencyService`
Archivo: `network/CurrencyService.kt`

Interfaz Retrofit para consultar tasas de cambio en tiempo real.

```
GET /currency/{code}  →  CurrencyResponse(rate: Double)
```

**Dónde se usa:** `FinanceViewModel.fetchExchangeRatePreview()` y `calculateGlobalBalance()`.

---

### `CloudinaryApi` / `CloudinaryClient`
Archivos: `network/CloudinaryApi.kt`, `network/CloudinaryClient.kt`

`CloudinaryApi` — interfaz Retrofit:
```
POST /upload  (multipart)  →  CloudinaryResponse(secureUrl: String)
```

`CloudinaryClient` — singleton con cliente lazy inicializado apuntando a la cuenta Cloudinary configurada.

**Dónde se usa:** `FinanceViewModel.uploadProfilePicture()` — sube foto de perfil y retorna la URL segura.

---

## ViewModel y Estado

### `FinanceViewModel`
Archivo: `viewmodel/FinanceViewModel.kt`

Extiende `AndroidViewModel`. Es el **único ViewModel** de la app. Expone `uiState: StateFlow<FinanceState>` que todas las pantallas observan con `collectAsStateWithLifecycle()`.

#### `FinanceState` — campos principales

| Campo | Tipo | Descripción |
|---|---|---|
| `isLoading` | `Boolean` | `true` solo durante el login/carga inicial — causa loader a pantalla completa en `MainActivity` |
| `currentUser` | `User?` | Usuario autenticado actualmente |
| `wallets` | `List<Wallet>` | Todas las billeteras del usuario |
| `selectedWallet` | `Wallet?` | Billetera activa en el Dashboard |
| `transactions` | `List<Transaction>` | Transacciones del wallet seleccionado (filtradas) |
| `allWalletTransactions` | `List<Transaction>` | Todas las transacciones de todos los wallets (sin filtro de wallet) |
| `convertedTransactions` | `List<Transaction>` | Transacciones convertidas a la moneda global |
| `filteredTransactions` | `List<Transaction>` | Transacciones tras aplicar búsqueda y tab activo |
| `categories` | `List<Category>` | Categorías del usuario |
| `balance` | `Double` | Saldo del wallet seleccionado |
| `globalBalance` | `Double` | Suma de todos los wallets convertida a moneda preferida |
| `budgets` | `List<Budget>` | Presupuestos activos |
| `savingsGoals` | `List<SavingsGoal>` | Metas de ahorro |
| `fixedExpenses` | `List<FixedExpense>` | Gastos fijos |
| `auditLogs` | `List<AuditLog>` | Logs cargados para la transacción seleccionada |
| `isLoadingAuditLogs` | `Boolean` | Indicador de carga de audit logs |
| `userSearchResults` | `List<UserSearchResult>` | Resultados de búsqueda de usuarios para transferencias |
| `chartIncomeTotal` | `Double` | Total ingresos para el gráfico del Dashboard |
| `chartExpenseTotal` | `Double` | Total gastos para el gráfico del Dashboard |
| `errorMessage` | `String?` | Mensaje de error a mostrar |
| `exchangeRatePreview` | `Double?` | Tasa de cambio para vista previa de conversión |
| `preferredCurrency` | `String` | Moneda para el balance global (default "PEN") |

#### Clases auxiliares internas del ViewModel

| Clase | Campos | Descripción |
|---|---|---|
| `UserSearchResult` | `email`, `displayName` | Resultado de búsqueda de usuario para transferencias |
| `WalletLookupResult` | `ownerEmail`, `ownerName`, `wallet: Wallet` | Resultado de búsqueda de wallet por número de cuenta |

#### Funciones del ViewModel

| Función | Descripción |
|---|---|
| `login(email, pass, onSuccess)` | Autentifica con Firebase, carga wallets, transacciones y categorías, establece `isLoading = false` al terminar |
| `register(firstName, lastName, email, pass, repeatPass, onSuccess)` | Crea usuario en Firestore, crea wallet inicial, inicia sesión |
| `logout(onSuccess)` | Limpia estado local y DataStore |
| `updateUser(...)` | Actualiza nombre, email, contraseña y foto de perfil en Firestore |
| `uploadProfilePicture(context, uri, onSuccess)` | Sube imagen a Cloudinary |
| `selectWallet(wallet)` | Cambia wallet activa, filtra `transactions`, recalcula chart totals — **NO llama** `calculateGlobalBalance()` ni cambia `isLoading` |
| `loadWallets()` | Escucha en tiempo real el snapshot de Firestore de wallets |
| `createWallet(wallet)` | Agrega wallet a Firestore |
| `updateWallet(wallet)` | Actualiza wallet; si cambia la moneda genera transacción de conversión automática |
| `deleteWallet(walletId)` | Borra wallet y todas sus transacciones de Firestore |
| `loadTransactions()` | Escucha en tiempo real las transacciones del usuario en Firestore |
| `addTransaction(...)` | Crea transacción simple en timestamp actual |
| `addTransactionWithDate(...)` | Crea transacción con timestamp, comprobante (Cloudinary) y coordenadas GPS |
| `updateTransaction(transaction, onSuccess)` | Actualiza transacción con validación de saldo, genera `AuditLog` con campos cambiados |
| `deleteTransaction(id)` | Borra transacción y revierte el impacto en el saldo del wallet |
| `transferMoney(fromWallet, toWallet, amount, onSuccess)` | Transfiere entre dos wallets propias con conversión de moneda; crea EXPENSE en origen e INCOME en destino |
| `transferToUser(fromWallet, toEmail, toWallet, amount, description, onSuccess)` | Transfiere a wallet de otro usuario; crea EXPENSE en el wallet propio e INCOME en el wallet ajeno en Firestore |
| `addExternalTransfer(fromWallet, amount, contact, timestamp, onSuccess)` | Registra transferencia externa (a banco u otro sistema), crea EXPENSE |
| `adjustBalance(newBalance)` | Genera transacción de ajuste para alcanzar el saldo indicado |
| `resetTransactions(initialBalance)` | Borra todas las transacciones del wallet activo |
| `searchUsers(query)` | Busca usuarios en Firestore por nombre o email (con debounce) |
| `clearUserSearch()` | Limpia resultados de `searchUsers` |
| `findWalletByAccountId(accountId, onResult, onError)` | Busca en Firestore con `collectionGroup("wallets")` por `accountId` de 12 dígitos; valida que no sea propio wallet |
| `getUserWallets(email, onResult)` | Obtiene los wallets de otro usuario por email |
| `loadCategories()` | Carga categorías desde Firestore; si no existen crea categorías por defecto |
| `addCategory(category)` | Agrega categoría nueva |
| `deleteCategory(id)` | Elimina categoría |
| `loadAuditLogs(transactionId)` | Carga el historial de cambios de una transacción |
| `loadPlanningData()` | Carga presupuestos, metas y gastos fijos |
| `addBudget(budget)` / `updateBudget` / `deleteBudget` | CRUD de presupuestos |
| `addSavingsGoal(goal)` / `updateSavingsGoal` / `deleteSavingsGoal` | CRUD de metas |
| `addDepositToGoal(goalId, amount)` | Deposita a meta sin tocar wallets |
| `depositToGoalFromWallet(goalId, fromWalletId, amount, onSuccess)` | Descuenta del wallet y acredita a la meta con conversión de moneda |
| `addFixedExpense(expense)` / `updateFixedExpense` / `deleteFixedExpense` / `toggleFixedExpense` | CRUD y activación/desactivación de gastos fijos |
| `executeFixedExpense(expenseId, onSuccess)` | Ejecuta manualmente un gasto fijo, creando la transacción correspondiente |
| `fetchExchangeRatePreview(fromCode, toCode)` | Consulta tasa de cambio para vista previa en UI |
| `clearExchangeRatePreview()` | Limpia la tasa de vista previa |
| `setPreferredCurrency(currencyCode)` | Cambia la moneda del balance global y recalcula |
| `getCurrencySymbol(currencyCode)` | Devuelve el símbolo de una moneda (ej. "PEN" → "S/.") |
| `exportData(uri, password, context)` | Exporta backup JSON (opcionalmente cifrado) de wallets, categorías y transacciones |
| `importData(uri, password, context)` | Importa backup JSON (con soporte de contraseña) |
| `exportReportToPdf(...)` | Genera PDF de reporte con encabezado, resumen y tabla de transacciones |
| `exportReportToCsv(...)` | Genera CSV de reporte |
| `calculateGlobalBalance()` | Recalcula el balance global sumando todos los wallets con conversión de moneda — **silencioso**, no toca `isLoading` |
| `calculateChartTotals()` | Calcula ingresos y gastos del wallet activo para el gráfico del Dashboard; trata TRANSFER legacy por prefijo de descripción |
| `applyFilters()` | Filtra `filteredTransactions` según tab activo y `searchQuery` |
| `updateSearchQuery(query)` | Actualiza búsqueda y llama `applyFilters()` |
| `updateSelectedTab(tab)` | Cambia tab activo y llama `applyFilters()` |
| `selectTheme(theme)` | Cambia tema y persiste en Firestore |

---

## Pantallas

### `MainActivity`
Archivo: `MainActivity.kt`

Punto de entrada Compose. Observa `uiState.isLoading`:
- Si `isLoading == true` → muestra `CircularProgressIndicator` a pantalla completa (destruye toda la composición)
- Si `isLoading == false` → muestra `FinanceApp(financeViewModel)` con la navegación completa

**Importante:** Cualquier `isLoading = true` fuera del flujo de login/carga inicial causará una pantalla blanca y reconstrucción completa de la UI.

---

### `AuthScreen`
Archivo: `ui/auth/AuthScreen.kt`

Pantalla de inicio de sesión. Campos: email, contraseña. Llama `viewModel.login()`. Navega a `RegisterScreen` si el usuario no tiene cuenta.

---

### `RegisterScreen`
Archivo: `ui/auth/RegisterScreen.kt`

Pantalla de registro. Llama `viewModel.register()`. Navega de vuelta a `AuthScreen` al completar.

---

### `DashboardScreen`
Archivo: `ui/dashboard/DashboardScreen.kt`

Pantalla principal después del login.

**Funcionalidades:**
- **HeroPager de billeteras** — HorizontalPager con una card por wallet; al deslizar actualiza el wallet activo via `snapshotFlow { pagerState.currentPage }` → `viewModel.selectWallet()`
- **Balance y gráfico** — muestra el balance del wallet activo y un gráfico de ingresos vs gastos
- **Lista de transacciones recientes** — últimas transacciones del wallet activo
- **Botones de acción rápida** — agregar ingreso, gasto, transferencia, conversión
- **Flujo de transferencia "Entre mis billeteras"** — modal con selector de wallet destino, vista previa de conversión, campo de monto
- **Flujo de transferencia "A otra persona"** (3 pasos):
  1. Buscar por nombre/email → `viewModel.searchUsers()`
  2. Ingresar número de cuenta de 12 dígitos → validar contra wallets del usuario encontrado
  3. Confirmar destino → ingresar monto y motivo → `viewModel.transferToUser()`
- **Ajuste de saldo** — modal para corregir el saldo del wallet activo

---

### `TransactionScreen`
Archivo: `ui/transactions/TransactionScreen.kt`

Lista completa de transacciones con:
- Tabs de filtro: Todas / Gastos / Ingresos / Transferencias / Conversiones
- Buscador por texto libre
- Toca una transacción → `TransactionDetailDialog`
- Captura de ubicación GPS al agregar transacción

---

### `CategoryScreen`
Archivo: `ui/categories/CategoryScreen.kt`

Gestión de categorías. Muestra lista separada por tipo (INCOME / EXPENSE). Permite crear con nombre e ícono, y eliminar. Llama `viewModel.addCategory()` / `viewModel.deleteCategory()`.

---

### `BudgetScreen`
Archivo: `ui/budget/BudgetScreen.kt`

Pantalla con tres pestañas:

| Pestaña | Modelo | Funciones del VM usadas |
|---|---|---|
| Presupuestos | `Budget` | `addBudget`, `updateBudget`, `deleteBudget` |
| Metas de Ahorro | `SavingsGoal` | `addSavingsGoal`, `updateSavingsGoal`, `deleteSavingsGoal`, `depositToGoalFromWallet` |
| Gastos Fijos | `FixedExpense` | `addFixedExpense`, `updateFixedExpense`, `deleteFixedExpense`, `toggleFixedExpense`, `executeFixedExpense` |

---

### `ProfileScreen`
Archivo: `ui/profile/ProfileScreen.kt`

Permite editar nombre, email, contraseña y foto de perfil. Selector de tema visual (6 temas). Selector de idioma. Botón de cerrar sesión. Importar/exportar backup JSON.

---

### `ReportsScreen`
Archivo: `ui/reports/ReportsScreen.kt`

Genera reportes financieros por período.

**Funcionalidades:**
- Selector de tipo de reporte (Diario, Semanal, Mensual, Anual, Personalizado)
- Selector de billetera — chips: "Todas las billeteras" + cada wallet individual; usa estado local `reportWalletFilter` (no llama `viewModel.selectWallet()`)
- Resumen con tarjetas animadas (crossfade): Ingresos, Gastos, Transferencias, Balance neto
- Lista "Detalle de Movimientos" con `LazyColumn(state = listState)` — preserva scroll
- `AnimatedContent` para transición suave al cambiar billetera
- Exportar a PDF (`viewModel.exportReportToPdf()`) o CSV (`viewModel.exportReportToCsv()`)
- `allWalletTransactions` — fuente de datos cuando `reportWalletFilter` != null o "all"

**Estado local clave:**
- `reportWalletFilter: String?` — null = wallet activa, "all" = todas, walletId = específica
- `listState: LazyListState` — preserva posición de scroll
- `reportType` — tipo de período seleccionado

---

## Componentes

### `TransactionDetailDialog`
Archivo: `ui/components/TransactionDetailDialog.kt`

Dialog modal que muestra todos los campos de una transacción. Pestañas: Detalle / Auditoría. Permite editar o eliminar. Si tiene `receiptPath`, muestra el comprobante. Si tiene coordenadas GPS, abre `LocationMapDialog`.

**Props relevantes:** `transaction`, `categoryIconName`, `wallet`, `userEmail`, `currencySymbol`, `onDismiss`, `auditLogs`, `onLoadAuditLogs`

---

### `ConversionDetailDialog`
Archivo: `ui/components/ConversionDetailDialog.kt`

Dialog para mostrar el detalle de una transacción de conversión de moneda (origin = "Ajuste de Moneda").

---

### `LocationMapDialog`
Archivo: `ui/components/LocationMapDialog.kt`

Dialog con mapa de Google Maps mostrando un marcador en las coordenadas de la transacción.

---

### `NumericKeyboard`
Archivo: `ui/components/NumericKeyboard.kt`

Teclado numérico personalizado usado en los modales de ingreso de montos del Dashboard.

---

### `ThemeSelector`
Archivo: `ui/components/ThemeSelector.kt`

Grid de chips de colores para seleccionar el tema visual. Llama `viewModel.selectTheme()`.

---

### `ActivityTimeline`
Archivo: `ui/components/ActivityTimeline.kt`

Visualización de línea de tiempo para `List<AuditLog>`. Muestra acción, timestamp y campos cambiados de forma legible.

---

## Tema

### `Color.kt`
Archivo: `ui/theme/Color.kt`

Define 6 paletas de colores: `DEFAULT` (azul índigo), `OCEAN` (cian), `GOLD` (ámbar), `PURPLE` (violeta), `ROSE` (rosa), `LIGHT` (claro).

### `Theme.kt`
Archivo: `ui/theme/Theme.kt`

- `AppTheme` enum — los 6 temas disponibles
- `getColorScheme(theme, darkMode)` — retorna el `ColorScheme` de Material3 correspondiente
- `Proyecto_Finanzas_PersonalesTheme` — composable raíz de tema que `MainActivity` aplica según `uiState.currentUser?.theme`

### `Type.kt`
Archivo: `ui/theme/Type.kt`

Define la tipografía (`Typography`) de Material3 usada globalmente.

---

## Utilidades

### `LocationHelper`
Archivo: `location/LocationHelper.kt`

| Función | Descripción |
|---|---|
| `isLocationEnabled(context)` | Verifica si el GPS del dispositivo está activo |
| `getCurrentLocation(context, onResult)` | Obtiene la ubicación actual via `FusedLocationProviderClient` y retorna latitud/longitud en el callback |

**Dónde se usa:** `TransactionScreen` — captura coordenadas al agregar una transacción con ubicación.

---

## Flujos importantes

### Login / Carga inicial
```
AuthScreen → viewModel.login()
  → Firebase Auth
  → loadWallets() (snapshot listener)
  → loadTransactions() (snapshot listener)
  → loadCategories()
  → isLoading = false
  → MainActivity muestra FinanceApp
```

### Transferencia entre usuarios (flujo híbrido)
```
DashboardScreen — pestaña "A otra persona"
  Paso 1: viewModel.searchUsers(query) → muestra lista de usuarios
  Paso 2: Usuario seleccionado → viewModel.getUserWallets(email)
          → usuario ingresa 12 dígitos → validación local contra wallets del destinatario
  Paso 3: Wallet confirmada → ingresar monto y motivo
          → viewModel.transferToUser(fromWallet, toEmail, toWallet, amount, desc)
             → EXPENSE en wallet propio
             → INCOME en wallet ajeno (escritura directa a Firestore del otro usuario)
```

### Reporte por billetera (sin pantalla blanca)
```
ReportsScreen — chip de wallet
  onClick: reportWalletFilter = wallet.id   ← NO llama viewModel.selectWallet()
  reportData recomputa usando uiState.allWalletTransactions filtrado localmente
  AnimatedContent hace crossfade en las tarjetas de resumen
  listState preserva el scroll
```

### Balance global
```
calculateGlobalBalance()  ← llamado al cargar transacciones y al cambiar moneda preferida
  → consulta tasas de cambio (con caché en DataStore)
  → suma todos los wallets convertidos a preferredCurrency
  → _uiState.update { it.copy(globalBalance = total) }
  ← NO toca isLoading (ejecuta en background silencioso)
```

---

*Documentación generada el 16/06/2026*
