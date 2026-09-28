package es.tendencias.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.tendencias.app.data.*
import es.tendencias.app.widget.TrendsUpdateWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    onConfigSaved: () -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { WidgetPreferencesRepository(context) }
    val initialConfig = remember { repository.loadConfig(appWidgetId) }

    var numberOfTrends by remember { mutableIntStateOf(initialConfig.numberOfTrends) }
    var textFormat by remember { mutableStateOf(initialConfig.textFormat) }
    var showRanking by remember { mutableStateOf(initialConfig.showRanking) }
    var density by remember { mutableStateOf(initialConfig.density) }
    var background by remember { mutableStateOf(initialConfig.background) }
    var showLastUpdate by remember { mutableStateOf(initialConfig.showLastUpdate) }
    var tapAction by remember { mutableStateOf(initialConfig.tapAction) }
    var showManualRefresh by remember { mutableStateOf(initialConfig.showManualRefresh) }
    var updateFrequency by remember { mutableStateOf(initialConfig.updateFrequency) }
    var showVolume by remember { mutableStateOf(initialConfig.showVolume) }
    var showPubDate by remember { mutableStateOf(initialConfig.showPubDate) }

    // Dropdown options
    val numberOptions = remember {
        listOf(
            WidgetPreferencesRepository.AUTO to "Automático",
            1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5",
            6 to "6", 7 to "7", 8 to "8", 9 to "9", 10 to "10"
        )
    }

    val textFormatOptions = remember {
        listOf(
            TextFormat.FULL to "Completo",
            TextFormat.SHORT_PHRASE to "Frase corta",
            TextFormat.ONE_WORD to "Una palabra",
            TextFormat.TWO_WORDS to "Dos palabras",
            TextFormat.ONE_LINE to "Una línea"
        )
    }

    val densityOptions = remember {
        listOf(
            Density.COMPACT to "Compacta",
            Density.NORMAL to "Normal",
            Density.WIDE to "Amplia",
            Density.AUTO to "Automática"
        )
    }

    val backgroundOptions = remember {
        listOf(
            Background.TRANSPARENT to "Transparente",
            Background.LIGHT to "Claro",
            Background.DARK to "Oscuro",
            Background.SYSTEM to "Sistema"
        )
    }

    val tapActionOptions = remember {
        listOf(
            TapAction.OPEN_TRENDS to "Abrir Google Trends",
            TapAction.OPEN_LINK to "Abrir enlace asociado",
            TapAction.SEARCH_WEB to "Buscar en Internet",
            TapAction.NONE to "No hacer nada"
        )
    }

    val updateFrequencyOptions = remember {
        listOf(
            UpdateFrequency.MANUAL to "Manual",
            UpdateFrequency.MIN_15 to "15 minutos",
            UpdateFrequency.MIN_30 to "30 minutos",
            UpdateFrequency.ONE_HOUR to "1 hora",
            UpdateFrequency.TWO_HOURS to "2 horas"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configurar widget",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Número de tendencias
            ConfigDropdown(
                label = "Número de tendencias",
                options = numberOptions,
                selectedValue = numberOfTrends,
                onSelected = { numberOfTrends = it }
            )

            // 2. Formato del texto
            ConfigDropdown(
                label = "Formato del texto",
                options = textFormatOptions,
                selectedValue = textFormat,
                onSelected = { textFormat = it }
            )

            // 3. Mostrar ranking
            ConfigSwitch(
                label = "Mostrar ranking",
                checked = showRanking,
                onCheckedChange = { showRanking = it }
            )

            // 4. Densidad
            ConfigDropdown(
                label = "Densidad",
                options = densityOptions,
                selectedValue = density,
                onSelected = { density = it }
            )

            // 5. Fondo
            ConfigDropdown(
                label = "Fondo",
                options = backgroundOptions,
                selectedValue = background,
                onSelected = { background = it }
            )

            // 6. Mostrar última actualización
            ConfigSwitch(
                label = "Mostrar última actualización",
                checked = showLastUpdate,
                onCheckedChange = { showLastUpdate = it }
            )

            // 7. Acción al tocar una tendencia
            ConfigDropdown(
                label = "Acción al tocar una tendencia",
                options = tapActionOptions,
                selectedValue = tapAction,
                onSelected = { tapAction = it }
            )

            // 8. Mostrar botón de actualización manual
            ConfigSwitch(
                label = "Mostrar botón de actualización manual",
                checked = showManualRefresh,
                onCheckedChange = { showManualRefresh = it }
            )

            // 9. Frecuencia de actualización
            ConfigDropdown(
                label = "Frecuencia de actualización",
                options = updateFrequencyOptions,
                selectedValue = updateFrequency,
                onSelected = { updateFrequency = it }
            )

            // 10. Mostrar volumen
            ConfigSwitch(
                label = "Mostrar volumen",
                checked = showVolume,
                onCheckedChange = { showVolume = it }
            )

            // 11. Mostrar fecha de la tendencia
            ConfigSwitch(
                label = "Mostrar fecha de la tendencia",
                checked = showPubDate,
                onCheckedChange = { showPubDate = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onCancelled) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val newConfig = WidgetPreferencesRepository.WidgetConfig(
                            numberOfTrends = numberOfTrends,
                            textFormat = textFormat,
                            showRanking = showRanking,
                            imageMode = ImageMode.NONE,
                            density = density,
                            background = background,
                            showLastUpdate = showLastUpdate,
                            tapAction = tapAction,
                            showManualRefresh = showManualRefresh,
                            updateFrequency = updateFrequency,
                            showVolume = showVolume,
                            showPubDate = showPubDate
                        )
                        repository.saveConfig(appWidgetId, newConfig)

                        if (initialConfig.updateFrequency != updateFrequency) {
                            when (updateFrequency) {
                                UpdateFrequency.MANUAL -> TrendsUpdateWorker.cancel(context)
                                UpdateFrequency.MIN_15 -> TrendsUpdateWorker.schedule(context, 15)
                                UpdateFrequency.MIN_30 -> TrendsUpdateWorker.schedule(context, 30)
                                UpdateFrequency.ONE_HOUR -> TrendsUpdateWorker.schedule(context, 60)
                                UpdateFrequency.TWO_HOURS -> TrendsUpdateWorker.schedule(context, 120)
                            }
                        }

                        onConfigSaved()
                    }
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ConfigDropdown(
    label: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selectedValue }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun ConfigSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
