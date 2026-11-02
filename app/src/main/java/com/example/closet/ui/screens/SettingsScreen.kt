package com.example.closet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

// =========================================================================
// TEMI DELL'APP (INCLUSO TEMA AMOLED)
// =========================================================================

enum class AppTheme(val label: String, val subtitle: String) {
    SYSTEM("Sistema", "Automatico"),
    LIGHT("Chiaro", "Luminoso"),
    DARK("Scuro", "One UI Dark"),
    AMOLED("AMOLED", "Nero puro")
}

// =========================================================================
// MODELLO DATI PER LE SCHEDE DELL'ISOLA PRINCIPALE (RIGA SINGOLA)
// =========================================================================

const val MAX_ISLAND_TABS = 4

enum class IslandTabType(
    val id: String,
    val label: String,
    val isMandatory: Boolean = false,
    val functionKey: String? = null
) {
    HOME("HOME", "Home", isMandatory = true, functionKey = null),
    WARDROBE("WARDROBE", "Armadio", isMandatory = false, functionKey = "WARDROBE"),
    FUNCTIONS("FUNCTIONS", "Funzioni", isMandatory = true, functionKey = null),
    CALENDAR("CALENDAR", "Calendario", isMandatory = false, functionKey = "CALENDAR"),
    CLOSET_PHOTOS("CLOSET_PHOTOS", "Foto armadio", isMandatory = false, functionKey = "CLOSET_PHOTOS"),
    MOVE("MOVE", "Spostamento", isMandatory = false, functionKey = "MOVE"),
    LAUNDRY("LAUNDRY", "Panni", isMandatory = false, functionKey = "LAUNDRY"),
    TRIP("TRIP", "Viaggio", isMandatory = false, functionKey = "TRIP"),
    TRANSFERS_ACTIVITY("TRANSFERS_ACTIVITY", "Trasferimenti", isMandatory = false, functionKey = "TRANSFERS_ACTIVITY"),
    TOOLS("TOOLS", "Strumenti", isMandatory = false, functionKey = "TOOLS")
}

/**
 * Garantisce che HOME e FUNCTIONS siano sempre presenti nell'isola,
 * senza duplicati e rispettando il limite massimo di schede per singola riga.
 */
fun normalizeIslandTabs(tabs: List<IslandTabType>): List<IslandTabType> {
    val distinct = tabs.distinct().toMutableList()
    if (!distinct.contains(IslandTabType.HOME)) {
        distinct.add(0, IslandTabType.HOME)
    }
    if (!distinct.contains(IslandTabType.FUNCTIONS)) {
        distinct.add(IslandTabType.FUNCTIONS)
    }
    if (distinct.size > MAX_ISLAND_TABS) {
        val result = mutableListOf<IslandTabType>()
        for (t in distinct) {
            if (result.size < MAX_ISLAND_TABS) {
                result.add(t)
            }
        }
        if (!result.contains(IslandTabType.HOME)) {
            result[0] = IslandTabType.HOME
        }
        if (!result.contains(IslandTabType.FUNCTIONS)) {
            val replaceIdx = result.indexOfLast { !it.isMandatory }.takeIf { it != -1 } ?: (result.size - 1)
            result[replaceIdx] = IslandTabType.FUNCTIONS
        }
        return result.distinct()
    }
    return distinct
}

// =========================================================================
// MODELLI DATI PER LE FUNZIONI PREFERITE (TASTO STELLA ★)
// =========================================================================

enum class FavoriteActionType(
    val id: String,
    val label: String
) {
    ADD_CLOTHING("ADD_CLOTHING", "Aggiungi capo"),
    SEARCH_WARDROBE("SEARCH_WARDROBE", "Cerca capo"),
    TORCH("TORCH", "Torcia"),
    CALENDAR("CALENDAR", "Calendario"),
    ADD_CALENDAR_EVENT("ADD_CALENDAR_EVENT", "Nuovo evento"),
    MOVE("MOVE", "Spostamento"),
    LAUNDRY("LAUNDRY", "Panni"),
    TRIP("TRIP", "Viaggio"),
    TRANSFERS("TRANSFERS", "Trasferimenti"),
    CLOSET_PHOTOS("CLOSET_PHOTOS", "Foto armadio"),
    TOOLS("TOOLS", "Strumenti"),
    SETTINGS("SETTINGS", "Impostazioni")
}

@Immutable
data class PlacedFavoriteShortcut(
    val id: String = java.util.UUID.randomUUID().toString(),
    val action: FavoriteActionType,
    val row: Int,
    val col: Int
)

private fun colorToHexString(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

private fun extractHueFromColor(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

// =========================================================================
// SCHERMATA IMPOSTAZIONI (ONE UI 9.0)
// =========================================================================

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentAccentColor: Color,
    isCustomAccent: Boolean,
    onAccentColorChange: (Color) -> Unit,
    onResetAccentColor: () -> Unit,
    useDistinctFunctionColors: Boolean,
    onToggleDistinctFunctionColors: (Boolean) -> Unit,
    islandTabs: List<IslandTabType> = listOf(IslandTabType.HOME, IslandTabType.WARDROBE, IslandTabType.FUNCTIONS),
    onSaveIslandTabs: (List<IslandTabType>) -> Unit = {},
    favoriteShortcuts: List<PlacedFavoriteShortcut> = emptyList(),
    onSaveFavoriteShortcuts: (List<PlacedFavoriteShortcut>) -> Unit = {},
    onClearAllData: () -> Unit,
    onResetSettings: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val scrollState = rememberScrollState()

    var showIslandTabsEditorDialog by remember { mutableStateOf(false) }
    var showFavoriteEditorDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    var sliderHue by remember(currentAccentColor) {
        mutableFloatStateOf(extractHueFromColor(currentAccentColor))
    }

    val hexCode = remember(currentAccentColor) {
        colorToHexString(currentAccentColor)
    }

    val topFadeColor = when {
        currentTheme == AppTheme.AMOLED -> Color(0xFF000000)
        colors.isDark -> Color(0xFF13171E)
        else -> Color(0xFFF2F4F8)
    }
    val bottomFadeColor = colors.fadeColorBottom

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Area Scrollabile con contenuto e titolo che scorre
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Intestazione con Titolo che scorre con il resto della schermata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spazio riservato al tasto indietro fluttuante quando lo scroll è in cima
                Spacer(modifier = Modifier.width(52.dp))
                Text(
                    text = "Impostazioni",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 0.4.sp
                )
            }

            // =====================================================
            // SEZIONE 1: ASPETTO E TEMA (SLIDER A 4 POSIZIONI)
            // =====================================================
                    SettingsSectionHeader(
                        title = "Aspetto e Tema",
                        icon = Icons.Outlined.DarkMode
                    )

                    Theme4PositionSlider(
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange
                    )

                    // =====================================================
                    // SEZIONE 2: COLORE PRINCIPALE (SLIDER + HEX BOX)
                    // =====================================================
                    SettingsSectionHeader(
                        title = "Colore Principale",
                        icon = Icons.Outlined.Palette
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tonalità personalizzata",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceSecondary,
                                    border = BorderStroke(1.dp, currentAccentColor.copy(alpha = 0.65f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(currentAccentColor)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                        Text(
                                            text = hexCode,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }

                            HueColorSlider(
                                currentHue = sliderHue,
                                activeColor = currentAccentColor,
                                onHueChanged = { newHue ->
                                    sliderHue = newHue
                                    val newColor = Color.hsv(newHue, 0.72f, 0.92f)
                                    onAccentColorChange(newColor)
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val quickPresets = remember {
                                    listOf(
                                        Color(0xFF53B5A2),
                                        Color(0xFF388AF6),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFEC4899),
                                        Color(0xFFF97316),
                                        Color(0xFF10B981)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    quickPresets.forEach { preset ->
                                        val isCurrent = colorToHexString(preset) == hexCode
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(preset)
                                                .border(
                                                    width = if (isCurrent) 2.dp else 0.5.dp,
                                                    color = if (isCurrent) colors.textPrimary else Color.White.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    sliderHue = extractHueFromColor(preset)
                                                    onAccentColorChange(preset)
                                                }
                                        )
                                    }
                                }

                                if (isCustomAccent) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.surfaceSecondary,
                                        modifier = Modifier.clickable { onResetAccentColor() }
                                    ) {
                                        Text(
                                            text = "Default",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = colors.cardBorder)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onToggleDistinctFunctionColors(!useDistinctFunctionColors) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "Colori distinti nelle Funzioni",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                Switch(
                                    checked = useDistinctFunctionColors,
                                    onCheckedChange = { onToggleDistinctFunctionColors(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.accent,
                                        uncheckedThumbColor = colors.textSecondary,
                                        uncheckedTrackColor = colors.surfaceSecondary
                                    ),
                                    modifier = Modifier.scale(0.88f)
                                )
                            }
                        }
                    }

                    // =====================================================
                    // SEZIONE 3: ISOLA DI NAVIGAZIONE & TASTO STELLA (★)
                    // =====================================================
                    SettingsSectionHeader(
                        title = "Isola e Scorciatoie",
                        icon = Icons.Outlined.StarOutline
                    )

                    // TASTO 1: GESTIONE DELLE SCHEDE DELL'ISOLA
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showIslandTabsEditorDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ViewCarousel,
                                        contentDescription = "Gestione delle schede",
                                        tint = colors.accent,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }

                                Text(
                                    text = "Gestione delle schede",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // TASTO 2: MODIFICA FUNZIONI PREFERITE (TASTO STELLA)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFavoriteEditorDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val starColor = Color(0xFFEAB308)
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(starColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Funzioni preferite",
                                        tint = starColor,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }

                                Text(
                                    text = "Modifica funzioni preferite",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // =====================================================
                    // SEZIONE 4: GESTIONE DATI E RIPRISTINO
                    // =====================================================
                    SettingsSectionHeader(
                        title = "Dati e Ripristino",
                        icon = Icons.Outlined.Storage
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            SettingsActionRow(
                                icon = Icons.Outlined.RestartAlt,
                                iconTint = colors.accent,
                                title = "Ripristina impostazioni predefinite",
                                onClick = { showResetSettingsDialog = true }
                            )

                            HorizontalDivider(
                                color = colors.cardBorder,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            SettingsActionRow(
                                icon = Icons.Outlined.DeleteForever,
                                iconTint = Color(0xFFE55353),
                                title = "Cancella tutti i dati dell'armadio",
                                titleColor = Color(0xFFE55353),
                                onClick = { showClearDataDialog = true }
                            )
                        }
                    }

                    // =====================================================
                    // SEZIONE 5: AGGIORNAMENTO SOFTWARE
                    // =====================================================
                    val context = LocalContext.current
                    val appVersionName = remember(context) {
                        try {
                            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            packageInfo.versionName ?: "1.0.0"
                        } catch (e: Exception) {
                            "1.0.0"
                        }
                    }

                    AppUpdateSection(
                        currentVersionName = appVersionName,
                        colors = colors
                    )

                    // =====================================================
                    // INFO VERSIONE APP E AUTORE (Auto-aggiornante)
                    // =====================================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Closet v$appVersionName",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "Autore: Riccardo Bettini",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textMuted
                        )
                    }
                }

        // SFUMATURA SUPERIORE IN USCITA
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            topFadeColor,
                            topFadeColor.copy(alpha = 0.85f),
                            topFadeColor.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                )
                .zIndex(15f)
        )

        // SFUMATURA INFERIORE
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            bottomFadeColor.copy(alpha = 0.72f),
                            bottomFadeColor
                        )
                    )
                )
                .zIndex(10f)
        )

        // Tasto Indietro Fluttuante e Fisso sullo schermo
        Surface(
            shape = CircleShape,
            color = colors.surfaceSecondary,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 18.dp)
                .size(40.dp)
                .zIndex(20f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Dialog Gestione Schede dell'Isola (Riga Singola)
    if (showIslandTabsEditorDialog) {
        IslandTabsEditorDialog(
            initialTabs = islandTabs,
            useDistinctColors = useDistinctFunctionColors,
            onDismiss = { showIslandTabsEditorDialog = false },
            onSave = { updatedTabs ->
                onSaveIslandTabs(updatedTabs)
                showIslandTabsEditorDialog = false
            }
        )
    }

    // Dialog Editor Funzioni Preferite (Tasto Stella)
    if (showFavoriteEditorDialog) {
        FavoriteShortcutsEditorDialog(
            initialShortcuts = favoriteShortcuts,
            useDistinctColors = useDistinctFunctionColors,
            onDismiss = { showFavoriteEditorDialog = false },
            onSave = { updatedList ->
                onSaveFavoriteShortcuts(updatedList)
                showFavoriteEditorDialog = false
            }
        )
    }

    // Dialog conferma ripristino impostazioni
    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Ripristinare le impostazioni?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Il tema, il colore principale, le schede dell'isola e la disposizione dei widget torneranno ai valori predefiniti. I tuoi capi nell'armadio non verranno toccati.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSettings()
                        showResetSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = CircleShape
                ) {
                    Text("Ripristina", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsDialog = false }) {
                    Text("Annulla", color = colors.textPrimary)
                }
            }
        )
    }

    // Dialog conferma cancellazione totale dati
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Cancellare tutti i dati?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tutti i capi, le cartelle, le foto degli armadi, gli impegni del calendario e i trasferimenti verranno eliminati definitivamente.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) {
                    Text("Elimina tutto", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Annulla", color = colors.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    val colors = LocalOneUIColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.accent,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun Theme4PositionSlider(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    val colors = LocalOneUIColors.current
    val themes = remember { AppTheme.entries }
    val selectedIndex = themes.indexOf(currentTheme).coerceIn(0, 3)

    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "themeSliderAnim"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        border = BorderStroke(0.5.dp, colors.cardBorder),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .onGloballyPositioned { coords ->
                containerWidthPx = coords.size.width.toFloat()
            }
            .pointerInput(themes) {
                detectTapGestures { offset ->
                    if (size.width > 0) {
                        val segmentWidth = size.width / 4f
                        val tappedIndex = (offset.x / segmentWidth).toInt().coerceIn(0, 3)
                        onThemeChange(themes[tappedIndex])
                    }
                }
            }
            .pointerInput(themes) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) {
                        val segmentWidth = size.width / 4f
                        val draggedIndex = (change.position.x / segmentWidth).toInt().coerceIn(0, 3)
                        if (themes[draggedIndex] != currentTheme) {
                            onThemeChange(themes[draggedIndex])
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val density = LocalDensity.current
            val thumbPadding = 4.dp
            val thumbPaddingPx = with(density) { thumbPadding.toPx() }
            val netWidthPx = (containerWidthPx - thumbPaddingPx * 2).coerceAtLeast(1f)
            val segmentWidthPx = netWidthPx / 4f
            val thumbWidthDp = with(density) { segmentWidthPx.toDp() }
            val thumbOffsetXDp = with(density) { (animatedIndex * segmentWidthPx).toDp() }

            // Cursore/Pillola mobile a molla One UI
            if (containerWidthPx > 0f) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (colors.isDark) colors.surfaceSecondary else Color.White,
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.4f)),
                    shadowElevation = 3.dp,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .offset(x = thumbOffsetXDp)
                        .width(thumbWidthDp)
                        .fillMaxHeight()
                ) {}
            }

            // 4 Opzioni cliccabili distribuite uniformemente
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                themes.forEachIndexed { index, theme ->
                    val isSelected = index == selectedIndex
                    val (icon, label) = when (theme) {
                        AppTheme.SYSTEM -> Icons.Outlined.BrightnessAuto to "Sistema"
                        AppTheme.LIGHT -> Icons.Outlined.LightMode to "Chiaro"
                        AppTheme.DARK -> Icons.Outlined.DarkMode to "Scuro"
                        AppTheme.AMOLED -> Icons.Outlined.Contrast to "AMOLED"
                    }

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) colors.accent else colors.textSecondary,
                        animationSpec = tween(180),
                        label = "themeIconTint_$index"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) colors.textPrimary else colors.textSecondary,
                        animationSpec = tween(180),
                        label = "themeTextColor_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onThemeChange(theme) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HueColorSlider(
    currentHue: Float,
    activeColor: Color,
    onHueChanged: (Float) -> Unit
) {
    val hueColors = remember {
        listOf(
            Color.hsv(0f, 0.75f, 0.95f),
            Color.hsv(60f, 0.75f, 0.95f),
            Color.hsv(120f, 0.75f, 0.95f),
            Color.hsv(180f, 0.75f, 0.95f),
            Color.hsv(240f, 0.75f, 0.95f),
            Color.hsv(300f, 0.75f, 0.95f),
            Color.hsv(360f, 0.75f, 0.95f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onHueChanged(fraction * 360f)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onHueChanged(fraction * 360f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val trackHeight = 14.dp.toPx()
            val topOffset = (size.height - trackHeight) / 2f

            drawRoundRect(
                brush = Brush.horizontalGradient(hueColors),
                topLeft = Offset(0f, topOffset),
                size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )

            val thumbX = ((currentHue.coerceIn(0f, 360f) / 360f) * size.width)
                .coerceIn(12.dp.toPx(), size.width - 12.dp.toPx())
            val center = Offset(thumbX, size.height / 2f)

            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = activeColor,
                radius = 9.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = 12.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    titleColor: Color = LocalOneUIColors.current.textPrimary,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = if (colors.isDark) 0.20f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

// =========================================================================
// SISTEMA ICONE E COLORI PER PREFERITI E SCHEDE DELL'ISOLA
// =========================================================================

@Immutable
data class FavoriteActionVisual(
    val icon: ImageVector,
    val distinctColor: Color
)

@Composable
fun getIslandTabVisual(
    tab: IslandTabType,
    useDistinctColors: Boolean
): FavoriteActionVisual {
    val colors = LocalOneUIColors.current
    val raw = when (tab) {
        IslandTabType.HOME ->
            Icons.Outlined.Home to colors.accent
        IslandTabType.WARDROBE ->
            Icons.Outlined.Checkroom to Color(0xFF14B8A6)
        IslandTabType.FUNCTIONS ->
            Icons.Outlined.GridView to colors.accent
        IslandTabType.CALENDAR ->
            Icons.Outlined.CalendarMonth to Color(0xFF388AF6)
        IslandTabType.CLOSET_PHOTOS ->
            Icons.Outlined.CameraAlt to Color(0xFF53B5A2)
        IslandTabType.MOVE ->
            Icons.Outlined.SyncAlt to Color(0xFF10B981)
        IslandTabType.LAUNDRY ->
            Icons.Outlined.LocalLaundryService to Color(0xFFEF4444)
        IslandTabType.TRIP ->
            Icons.Outlined.Work to Color(0xFF8B5CF6)
        IslandTabType.TRANSFERS_ACTIVITY ->
            Icons.Outlined.History to Color(0xFFF97316)
        IslandTabType.TOOLS ->
            Icons.Outlined.Construction to Color(0xFFEAB308)
    }
    return FavoriteActionVisual(
        icon = raw.first,
        distinctColor = if (useDistinctColors && !tab.isMandatory) raw.second else colors.accent
    )
}

@Composable
fun getFavoriteActionVisual(
    action: FavoriteActionType,
    useDistinctColors: Boolean,
    isTorchOn: Boolean = false
): FavoriteActionVisual {
    val colors = LocalOneUIColors.current
    val raw = when (action) {
        FavoriteActionType.ADD_CLOTHING ->
            Icons.Default.Add to colors.accent
        FavoriteActionType.SEARCH_WARDROBE ->
            Icons.Default.Search to Color(0xFF06B6D4)
        FavoriteActionType.TORCH ->
            (if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOff) to Color(0xFFEAB308)
        FavoriteActionType.CALENDAR ->
            Icons.Outlined.CalendarMonth to Color(0xFF388AF6)
        FavoriteActionType.ADD_CALENDAR_EVENT ->
            Icons.Default.EditCalendar to Color(0xFF388AF6)
        FavoriteActionType.MOVE ->
            Icons.Outlined.SyncAlt to Color(0xFF10B981)
        FavoriteActionType.LAUNDRY ->
            Icons.Outlined.LocalLaundryService to Color(0xFFEF4444)
        FavoriteActionType.TRIP ->
            Icons.Outlined.Work to Color(0xFF8B5CF6)
        FavoriteActionType.TRANSFERS ->
            Icons.Outlined.History to Color(0xFFF97316)
        FavoriteActionType.CLOSET_PHOTOS ->
            Icons.Outlined.CameraAlt to Color(0xFF53B5A2)
        FavoriteActionType.TOOLS ->
            Icons.Outlined.Construction to Color(0xFFEAB308)
        FavoriteActionType.SETTINGS ->
            Icons.Outlined.Settings to Color(0xFF64748B)
    }
    return FavoriteActionVisual(
        icon = raw.first,
        distinctColor = if (useDistinctColors || action == FavoriteActionType.TORCH) raw.second else colors.accent
    )
}

fun compactFavoriteRows(
    items: List<PlacedFavoriteShortcut>
): List<PlacedFavoriteShortcut> {
    if (items.isEmpty()) return emptyList()
    val distinctSortedRows = items.map { it.row }.distinct().sorted()
    val topOffset = if (distinctSortedRows.size < 4) 1 else 0
    val rowMap = distinctSortedRows.withIndex().associate { (newIdx, oldRow) -> oldRow to (newIdx + topOffset) }
    return items.map { item ->
        item.copy(
            row = rowMap[item.row] ?: topOffset,
            col = item.col.coerceIn(0, 3)
        )
    }.sortedWith(compareBy({ it.row }, { it.col }))
}

fun resolveFavoriteGridDragLayout(
    baseItems: List<PlacedFavoriteShortcut>,
    draggedId: String,
    targetRow: Int,
    targetCol: Int
): List<PlacedFavoriteShortcut> {
    val dragged = baseItems.find { it.id == draggedId } ?: return baseItems
    val safeCol = targetCol.coerceIn(0, 3)
    val safeRow = targetRow.coerceAtLeast(0)

    if (dragged.row == safeRow && dragged.col == safeCol) return baseItems

    val others = baseItems.filter { it.id != draggedId }.toMutableList()
    val collidingIdx = others.indexOfFirst { it.row == safeRow && it.col == safeCol }

    if (collidingIdx != -1) {
        val colliding = others[collidingIdx]
        others[collidingIdx] = colliding.copy(row = dragged.row, col = dragged.col)
    }

    others.add(dragged.copy(row = safeRow, col = safeCol))
    return others
}

/**
 * Isola Popup che compare sopra il tasto Stella (★) con raggio di curvatura 27.dp
 * identico al tasto stella e all'isola principale (altezza 54.dp, raggio 27.dp).
 * Nel caso di 1 colonna ha larghezza esattamente 54.dp, uguale al tasto stella.
 * La colonna più a destra è sempre perfettamente allineata al tasto stella.
 */
@Composable
fun FavoriteShortcutsIslandPopup(
    shortcuts: List<PlacedFavoriteShortcut>,
    useDistinctColors: Boolean,
    isTorchOn: Boolean,
    onShortcutClick: (FavoriteActionType) -> Unit,
    onOpenEditorClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val compacted = remember(shortcuts) { compactFavoriteRows(shortcuts) }

    Surface(
        shape = RoundedCornerShape(27.dp),
        color = colors.islandBackground,
        border = BorderStroke(0.5.dp, colors.cardBorder),
        tonalElevation = 8.dp,
        shadowElevation = 18.dp
    ) {
        if (compacted.isEmpty()) {
            Row(
                modifier = Modifier
                    .height(54.dp)
                    .clickable { onOpenEditorClick() }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Configura funzioni preferite",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        } else {
            val minRow = compacted.minOf { it.row }
            val maxRow = compacted.maxOf { it.row }
            val minCol = compacted.minOf { it.col }
            val maxCol = compacted.maxOf { it.col }

            val byCell = remember(compacted) {
                compacted.associateBy { it.row to it.col }
            }

            Column(
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (r in minRow..maxRow) {
                    Row(
                        modifier = Modifier.wrapContentSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in minCol..maxCol) {
                            val item = byCell[r to c]
                            Box(
                                modifier = Modifier.size(54.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item != null) {
                                    val visual = getFavoriteActionVisual(
                                        action = item.action,
                                        useDistinctColors = useDistinctColors,
                                        isTorchOn = isTorchOn
                                    )
                                    val isActiveTorch = item.action == FavoriteActionType.TORCH && isTorchOn

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isActiveTorch) visual.distinctColor
                                                else visual.distinctColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f)
                                            )
                                            .border(
                                                width = if (isActiveTorch) 1.5.dp else 0.5.dp,
                                                color = if (isActiveTorch) Color.White.copy(alpha = 0.6f) else visual.distinctColor.copy(alpha = 0.35f),
                                                shape = CircleShape
                                            )
                                            .clickable { onShortcutClick(item.action) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = visual.icon,
                                            contentDescription = item.action.label,
                                            tint = if (isActiveTorch) Color(0xFF1A1600) else visual.distinctColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(42.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// DIALOG GESTIONE SCHEDE DELL'ISOLA INFERIORE (RIGA SINGOLA - STILE WIDGET)
// =========================================================================

fun resolveSingleRowDragLayout(
    baseTabs: List<IslandTabType>,
    draggedTab: IslandTabType,
    targetCol: Int
): List<IslandTabType> {
    if (baseTabs.isEmpty()) return baseTabs
    val safeCol = targetCol.coerceIn(0, baseTabs.size - 1)
    val currentIdx = baseTabs.indexOf(draggedTab)
    if (currentIdx == -1 || currentIdx == safeCol) return baseTabs

    val mutable = baseTabs.toMutableList()
    mutable.removeAt(currentIdx)
    mutable.add(safeCol, draggedTab)
    return mutable
}

