# Módulo: Notas

Pantalla CRUD accesible desde Perfil → Notas → Mis Notas. Usa Firestore como almacenamiento y el ViewModel compartido.

---

## Modelo (`model/Nota.kt`)

| Campo       | Tipo   | Valor por defecto              |
|-------------|--------|-------------------------------|
| `id`        | String | `UUID.randomUUID().toString()` |
| `titulo`    | String | `""`                           |
| `contenido` | String | `""`                           |
| `timestamp` | Long   | `System.currentTimeMillis()`   |

Colección Firestore: `users/{email}/notas/{id}`

---

## Estado (`FinanceState`)

```kotlin
val notas: List<Nota> = emptyList()
```

Se carga al hacer login junto con categorías.

---

## ViewModel (`FinanceViewModel`)

| Función               | Qué hace                                                     |
|-----------------------|--------------------------------------------------------------|
| `loadNotas()`         | Lee `users/{email}/notas`, actualiza `state.notas`           |
| `addNota(nota)`       | Guarda en Firestore con `nota.id` como doc ID, recarga lista |
| `updateNota(nota)`    | Sobreescribe el documento existente, recarga lista           |
| `deleteNota(id)`      | Elimina el documento por ID, recarga lista                   |

`loadNotas()` se llama dentro de `login()` automáticamente.

---

## Pantalla (`ui/notas/NotasScreen.kt`)

### Composables

| Nombre        | Rol                                                     |
|---------------|---------------------------------------------------------|
| `NotasScreen` | Pantalla raíz: Scaffold + buscador + lista + FAB        |
| `NotaItem`    | Card con título, vista previa de contenido, fecha, íconos editar/eliminar |
| `NotaDialog`  | AlertDialog reutilizable para crear y editar. Recibe `initial: Nota` y callbacks `onConfirm`/`onDismiss` |

### Búsqueda

Filtra en memoria con `remember(uiState.notas, query)` comparando `titulo` y `contenido` ignorando mayúsculas. No realiza llamadas a Firestore.

### Validaciones

- Título obligatorio (muestra error inline si está en blanco).
- Contenido opcional.

---

## Navegación (`MainActivity.kt`)

```
"profile" → onNavigateToNotas → navController.navigate("notas")
"notas"   → NotasScreen(viewModel, onNavigateBack = popBackStack)
```

---

## Cómo copiar este patrón para otro CRUD

1. Crear `model/NuevoModelo.kt` con `id`, campos de negocio y `timestamp`.
2. Añadir `val nuevoModelos: List<NuevoModelo> = emptyList()` en `FinanceState`.
3. Agregar `loadNuevoModelos()`, `addNuevoModelo()`, `updateNuevoModelo()`, `deleteNuevoModelo()` en el ViewModel.
4. Llamar `loadNuevoModelos()` dentro de `login()`.
5. Crear `ui/nuevomodelo/NuevoModeloScreen.kt` copiando la estructura de `NotasScreen`.
6. Registrar ruta en `MainActivity.kt` y pasar callback desde la pantalla de origen.
