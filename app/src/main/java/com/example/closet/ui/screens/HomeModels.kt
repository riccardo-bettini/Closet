package com.example.closet.ui.screens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =========================================================================
// DEFINIZIONE COLORI E TEMA ONE UI 9.0
// =========================================================================

@Immutable
data class OneUIColors(
    val isDark: Boolean,
    val backgroundBrush: Brush,
    val fadeColorBottom: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val islandBackground: Color,
    val surfaceSecondary: Color,
    val dialogBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color = Color(0xFF53B5A2),
    val accentContainer: Color,
    val eventChipBg: Color
)

val DarkOneUIColors = OneUIColors(
    isDark = true,
    backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF13171E), Color(0xFF0C0E12), Color(0xFF07080A))),
    fadeColorBottom = Color(0xFF07080A),
    cardBackground = Color(0xFF14171E).copy(alpha = 0.90f),
    cardBorder = Color.White.copy(alpha = 0.08f),
    islandBackground = Color(0xFF1E2128).copy(alpha = 0.94f),
    surfaceSecondary = Color(0xFF1E222A),
    dialogBackground = Color(0xFF1E222A),
    textPrimary = Color.White,
    textSecondary = Color(0xFF8F949F),
    textMuted = Color(0xFF5A606D),
    accentContainer = Color(0xFF1B2E2A),
    eventChipBg = Color(0xFF1C1F26)
)

val AmoledOneUIColors = OneUIColors(
    isDark = true,
    backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF000000), Color(0xFF000000))),
    fadeColorBottom = Color(0xFF000000),
    cardBackground = Color(0xFF0A0C10).copy(alpha = 0.96f),
    cardBorder = Color.White.copy(alpha = 0.11f),
    islandBackground = Color(0xFF101218).copy(alpha = 0.96f),
    surfaceSecondary = Color(0xFF12151C),
    dialogBackground = Color(0xFF0D0F14),
    textPrimary = Color.White,
    textSecondary = Color(0xFF8F949F),
    textMuted = Color(0xFF5A606D),
    accentContainer = Color(0xFF122420),
    eventChipBg = Color(0xFF12151C)
)

val LightOneUIColors = OneUIColors(
    isDark = false,
    backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFF2F4F8), Color(0xFFE8EBF0), Color(0xFFDFE3EA))),
    fadeColorBottom = Color(0xFFDFE3EA),
    cardBackground = Color(0xFFFFFFFF).copy(alpha = 0.95f),
    cardBorder = Color(0xFF000000).copy(alpha = 0.06f),
    islandBackground = Color(0xFFFFFFFF).copy(alpha = 0.92f),
    surfaceSecondary = Color(0xFFE6E9F0),
    dialogBackground = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF121417),
    textSecondary = Color(0xFF5F6573),
    textMuted = Color(0xFF9096A4),
    accent = Color(0xFF1B8A74),
    accentContainer = Color(0xFFD4EFE8),
    eventChipBg = Color(0xFFF0F2F6)
)

val LocalOneUIColors = compositionLocalOf { DarkOneUIColors }

// =========================================================================
// ENUM MODALITA' E NAVIGAZIONE
// =========================================================================

enum class BottomIslandMode {
    NORMAL,
    ITEM_EDIT,
    FUNCTION_EDIT,
    WIDGET_EDIT
}

enum class NavigationSection {
    FUNCTIONS,
    WARDROBE,
    HOME,
    SETTINGS
}

enum class HomeLayoutMode {
    INFINITE_SCROLL,
    PAGED
}

enum class PageIndicatorVisibility {
    NEVER,
    ON_SCROLL,
    ALWAYS
}

enum class PageIndicatorPosition {
    BOTTOM,
    RIGHT,
    LEFT
}

// =========================================================================
// MAPPING TRA ISLAND TAB TYPE E FUNCTION SUB SCREEN
// =========================================================================

fun IslandTabType.toFunctionSubScreenOrNull(): FunctionSubScreen? = when (this) {
    IslandTabType.WARDROBE -> FunctionSubScreen.WARDROBE
    IslandTabType.CALENDAR -> FunctionSubScreen.CALENDAR
    IslandTabType.CLOSET_PHOTOS -> FunctionSubScreen.CLOSET_PHOTOS
    IslandTabType.MOVE -> FunctionSubScreen.MOVE
    IslandTabType.LAUNDRY -> FunctionSubScreen.LAUNDRY
    IslandTabType.TRIP -> FunctionSubScreen.TRIP
    IslandTabType.TRANSFERS_ACTIVITY -> FunctionSubScreen.TRANSFERS_ACTIVITY
    IslandTabType.TOOLS -> FunctionSubScreen.TOOLS
    else -> null
}

fun FunctionSubScreen.toIslandTabType(): IslandTabType = when (this) {
    FunctionSubScreen.WARDROBE -> IslandTabType.WARDROBE
    FunctionSubScreen.CALENDAR -> IslandTabType.CALENDAR
    FunctionSubScreen.CLOSET_PHOTOS -> IslandTabType.CLOSET_PHOTOS
    FunctionSubScreen.MOVE -> IslandTabType.MOVE
    FunctionSubScreen.LAUNDRY -> IslandTabType.LAUNDRY
    FunctionSubScreen.TRIP -> IslandTabType.TRIP
    FunctionSubScreen.TRANSFERS_ACTIVITY -> IslandTabType.TRANSFERS_ACTIVITY
    FunctionSubScreen.TOOLS -> IslandTabType.TOOLS
}

// =========================================================================
// MODELLI E TIPI DEI WIDGET
// =========================================================================

enum class WidgetType(val rowSpan: Int, val colSpan: Int, val title: String) {
    CALENDAR_TODAY_1X1(1, 1, "Oggi Mini"),
    CALENDAR_TODAY_2X1(1, 2, "Oggi Compatto"),
    CALENDAR_TODAY_2X2(2, 2, "Oggi"),
    CALENDAR_MONTH_2X2(2, 2, "Mese"),
    CALENDAR_AGENDA_4X2(2, 4, "Mese & Eventi"),
    CALENDAR_ADD_EVENT_1X1(1, 1, "Nuovo Evento Mini"),
    CALENDAR_ADD_EVENT_1X2(1, 2, "Nuovo Evento"),

    WARDROBE_COUNT_1X1(1, 1, "Conteggio Mini"),
    WARDROBE_COUNT_1X2(1, 2, "Conteggio Compatto"),
    WARDROBE_COUNT_2X2(2, 2, "Conteggio Capi"),
    WARDROBE_COUNT_2X4(2, 4, "Conteggio Ampio"),

    WARDROBE_LIST_1X4(1, 4, "Striscia Capi"),
    WARDROBE_LIST_2X2(2, 2, "Elenco Compatto"),
    WARDROBE_LIST_2X4(2, 4, "Elenco Capi Rapido"),

    WARDROBE_SEARCH_1X1(1, 1, "Cerca Mini"),
    WARDROBE_SEARCH_1X2(1, 2, "Cerca Capi"),
    WARDROBE_ADD_ITEM_1X1(1, 1, "Aggiungi Mini"),
    WARDROBE_ADD_ITEM_1X2(1, 2, "Aggiungi Capo"),
    WARDROBE_ADD_ITEM_2X2(2, 2, "Aggiungi Capo"),

    FUNCTION_CALENDAR_1X1(1, 1, "Scorciatoia Calendario"),
    FUNCTION_TOOLS_1X1(1, 1, "Scorciatoia Strumenti"),
    FUNCTION_QUICK_ACTIONS_1X4(1, 4, "Scorciatoie Funzioni"),
    FUNCTION_QUICK_ACTIONS_2X2(2, 2, "Pannello Funzioni"),
    FUNCTION_PHOTO_1X1(1, 1, "Scorciatoia Foto"),
    FUNCTION_PHOTO_2X2(2, 2, "Panoramica Armadio"),
    FUNCTION_PHOTO_2X4(2, 4, "Panoramica Ampia"),
    FUNCTION_MOVE_1X1(1, 1, "Scorciatoia Sposta"),
    FUNCTION_MOVE_1X2(1, 2, "Spostamento Rapido"),
    FUNCTION_MOVE_2X2(2, 2, "Trasferimento Capi"),
    FUNCTION_LAUNDRY_1X1(1, 1, "Scorciatoia Panni"),
    FUNCTION_LAUNDRY_1X2(1, 2, "Timer Panni"),
    FUNCTION_LAUNDRY_2X2(2, 2, "Stato Panni & Timer"),
    FUNCTION_LAUNDRY_2X4(2, 4, "Pannello Panni Esteso"),
    FUNCTION_TRIP_1X1(1, 1, "Scorciatoia Viaggio"),
    FUNCTION_TRIP_1X2(1, 2, "Valigia Compatta"),
    FUNCTION_TRIP_2X2(2, 2, "Valigia Viaggio"),
    FUNCTION_TRIP_2X4(2, 4, "Stato Viaggio Panoramico"),
    FUNCTION_TRANSFERS_1X1(1, 1, "Scorciatoia Registro"),
    FUNCTION_TRANSFERS_1X2(1, 2, "Attività Trasferimenti"),
    FUNCTION_TRANSFERS_2X2(2, 2, "Registro Trasferimenti"),

    UTILITY_TORCH_1X1(1, 1, "Torcia Mini"),
    UTILITY_TORCH_1X2(1, 2, "Torcia Rapida"),
    UTILITY_TORCH_2X2(2, 2, "Pannello Torcia"),

    UTILITY_COMPASS_1X1(1, 1, "Bussola Mini"),
    UTILITY_COMPASS_1X2(1, 2, "Bussola Compatta"),
    UTILITY_COMPASS_2X2(2, 2, "Bussola Grafica"),
    UTILITY_COMPASS_2X4(2, 4, "Bussola & Orientamento"),

    UTILITY_LEVEL_1X1(1, 1, "Livella Mini"),
    UTILITY_LEVEL_1X2(1, 2, "Livella Compatta"),
    UTILITY_LEVEL_2X2(2, 2, "Livella a Bolla"),
    UTILITY_LEVEL_2X4(2, 4, "Livella Estesa"),

    CLOCK_DIGITAL_1X2(1, 2, "Orologio Digitale Compatto"),
    CLOCK_DIGITAL_2X2(2, 2, "Orologio Digitale"),
    CLOCK_DIGITAL_2X4(2, 4, "Orologio Digitale Esteso"),
    CLOCK_ANALOG_1X1(1, 1, "Orologio Analogico Mini"),
    CLOCK_ANALOG_2X2(2, 2, "Orologio Analogico"),
    CLOCK_ANALOG_2X4(2, 4, "Orologio Analogico & Data")
}

// =========================================================================
// FAMIGLIE DEI WIDGET E DIMENSIONI (SAMSUNG ONE UI 9.0)
// =========================================================================

enum class WidgetFamily(val displayName: String) {
    CALENDAR_TODAY("Oggi"),
    CALENDAR_MONTH_AGENDA("Mese & Agenda"),
    CALENDAR_ADD_EVENT("Nuovo Evento"),
    WARDROBE_COUNT("Conteggio Capi"),
    WARDROBE_LIST("Elenco Capi"),
    WARDROBE_SEARCH("Cerca Capi"),
    WARDROBE_ADD_ITEM("Aggiungi Capo"),
    FUNCTION_CALENDAR("Scorciatoia Calendario"),
    FUNCTION_TOOLS("Scorciatoia Strumenti"),
    FUNCTION_QUICK_ACTIONS("Pannello Funzioni"),
    FUNCTION_PHOTO("Foto Armadio"),
    FUNCTION_MOVE("Spostamento Capi"),
    FUNCTION_LAUNDRY("Panni"),
    FUNCTION_TRIP("Viaggio"),
    FUNCTION_TRANSFERS("Registro Trasferimenti"),
    UTILITY_TORCH("Torcia"),
    UTILITY_COMPASS("Bussola"),
    UTILITY_LEVEL("Livella"),
    CLOCK_DIGITAL("Orologio Digitale"),
    CLOCK_ANALOG("Orologio Analogico")
}

val WidgetType.family: WidgetFamily
    get() = when (this) {
        WidgetType.CALENDAR_TODAY_1X1,
        WidgetType.CALENDAR_TODAY_2X1,
        WidgetType.CALENDAR_TODAY_2X2 -> WidgetFamily.CALENDAR_TODAY

        WidgetType.CALENDAR_MONTH_2X2,
        WidgetType.CALENDAR_AGENDA_4X2 -> WidgetFamily.CALENDAR_MONTH_AGENDA

        WidgetType.CALENDAR_ADD_EVENT_1X1,
        WidgetType.CALENDAR_ADD_EVENT_1X2 -> WidgetFamily.CALENDAR_ADD_EVENT

        WidgetType.WARDROBE_COUNT_1X1,
        WidgetType.WARDROBE_COUNT_1X2,
        WidgetType.WARDROBE_COUNT_2X2,
        WidgetType.WARDROBE_COUNT_2X4 -> WidgetFamily.WARDROBE_COUNT

        WidgetType.WARDROBE_LIST_1X4,
        WidgetType.WARDROBE_LIST_2X2,
        WidgetType.WARDROBE_LIST_2X4 -> WidgetFamily.WARDROBE_LIST

        WidgetType.WARDROBE_SEARCH_1X1,
        WidgetType.WARDROBE_SEARCH_1X2 -> WidgetFamily.WARDROBE_SEARCH

        WidgetType.WARDROBE_ADD_ITEM_1X1,
        WidgetType.WARDROBE_ADD_ITEM_1X2,
        WidgetType.WARDROBE_ADD_ITEM_2X2 -> WidgetFamily.WARDROBE_ADD_ITEM

        WidgetType.FUNCTION_CALENDAR_1X1 -> WidgetFamily.FUNCTION_CALENDAR
        WidgetType.FUNCTION_TOOLS_1X1 -> WidgetFamily.FUNCTION_TOOLS

        WidgetType.FUNCTION_QUICK_ACTIONS_1X4,
        WidgetType.FUNCTION_QUICK_ACTIONS_2X2 -> WidgetFamily.FUNCTION_QUICK_ACTIONS

        WidgetType.FUNCTION_PHOTO_1X1,
        WidgetType.FUNCTION_PHOTO_2X2,
        WidgetType.FUNCTION_PHOTO_2X4 -> WidgetFamily.FUNCTION_PHOTO

        WidgetType.FUNCTION_MOVE_1X1,
        WidgetType.FUNCTION_MOVE_1X2,
        WidgetType.FUNCTION_MOVE_2X2 -> WidgetFamily.FUNCTION_MOVE

        WidgetType.FUNCTION_LAUNDRY_1X1,
        WidgetType.FUNCTION_LAUNDRY_1X2,
        WidgetType.FUNCTION_LAUNDRY_2X2,
        WidgetType.FUNCTION_LAUNDRY_2X4 -> WidgetFamily.FUNCTION_LAUNDRY

        WidgetType.FUNCTION_TRIP_1X1,
        WidgetType.FUNCTION_TRIP_1X2,
        WidgetType.FUNCTION_TRIP_2X2,
        WidgetType.FUNCTION_TRIP_2X4 -> WidgetFamily.FUNCTION_TRIP

        WidgetType.FUNCTION_TRANSFERS_1X1,
        WidgetType.FUNCTION_TRANSFERS_1X2,
        WidgetType.FUNCTION_TRANSFERS_2X2 -> WidgetFamily.FUNCTION_TRANSFERS

        WidgetType.UTILITY_TORCH_1X1,
        WidgetType.UTILITY_TORCH_1X2,
        WidgetType.UTILITY_TORCH_2X2 -> WidgetFamily.UTILITY_TORCH

        WidgetType.UTILITY_COMPASS_1X1,
        WidgetType.UTILITY_COMPASS_1X2,
        WidgetType.UTILITY_COMPASS_2X2,
        WidgetType.UTILITY_COMPASS_2X4 -> WidgetFamily.UTILITY_COMPASS

        WidgetType.UTILITY_LEVEL_1X1,
        WidgetType.UTILITY_LEVEL_1X2,
        WidgetType.UTILITY_LEVEL_2X2,
        WidgetType.UTILITY_LEVEL_2X4 -> WidgetFamily.UTILITY_LEVEL

        WidgetType.CLOCK_DIGITAL_1X2,
        WidgetType.CLOCK_DIGITAL_2X2,
        WidgetType.CLOCK_DIGITAL_2X4 -> WidgetFamily.CLOCK_DIGITAL

        WidgetType.CLOCK_ANALOG_1X1,
        WidgetType.CLOCK_ANALOG_2X2,
        WidgetType.CLOCK_ANALOG_2X4 -> WidgetFamily.CLOCK_ANALOG
    }

private val variantsByFamily: Map<WidgetFamily, List<WidgetType>> by lazy {
    WidgetType.entries
        .groupBy { it.family }
        .mapValues { (_, list) ->
            list.sortedWith(compareBy<WidgetType> { it.rowSpan * it.colSpan }.thenBy { it.colSpan })
        }
}

/**
 * Restituisce tutte le varianti dimensionali disponibili per la famiglia di questo widget.
 */
fun WidgetType.getAvailableVariants(): List<WidgetType> {
    return variantsByFamily[this.family] ?: listOf(this)
}

/**
 * Restituisce la variante successiva (utile per il tocco diretto sul cursore di ridimensionamento).
 */
fun WidgetType.getNextSizeVariant(): WidgetType {
    val variants = getAvailableVariants()
    if (variants.size <= 1) return this
    val currentIndex = variants.indexOf(this)
    val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % variants.size else 0
    return variants[nextIndex]
}

/**
 * Trova la variante migliore in base alle dimensioni trascinate (colonne x righe).
 */
fun WidgetType.findBestVariantForSize(targetCols: Int, targetRows: Int): WidgetType {
    val variants = getAvailableVariants()
    if (variants.size <= 1) return this
    return variants.minByOrNull { variant ->
        val dc = variant.colSpan - targetCols
        val dr = variant.rowSpan - targetRows
        (dc * dc) + (dr * dr * 2)
    } ?: this
}

/**
 * Trova la variante migliore durante il trascinamento di una maniglia della cornice di ridimensionamento,
 * privilegiando l'asse principale trascinato (orizzontale per maniglie sinistra/destra, verticale per alto/basso).
 */
fun WidgetType.findBestVariantForResize(
    desiredCols: Int,
    desiredRows: Int,
    primaryHorizontal: Boolean? = null
): WidgetType {
    val variants = getAvailableVariants()
    if (variants.size <= 1) return this
    return variants.minByOrNull { variant ->
        val dc = variant.colSpan - desiredCols
        val dr = variant.rowSpan - desiredRows
        when (primaryHorizontal) {
            true -> (dc * dc * 5) + (dr * dr)
            false -> (dr * dr * 6) + (dc * dc)
            null -> (dc * dc * 2) + (dr * dr * 3)
        }
    } ?: this
}

enum class ResizeHandleType {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

@Immutable
data class PlacedWidget(
    val instanceId: String = java.util.UUID.randomUUID().toString(),
    val type: WidgetType,
    val targetCloset: String? = null,
    val gridRow: Int = -1,
    val gridCol: Int = -1
)

@Immutable
data class WidgetPlacement(
    val widget: PlacedWidget,
    val row: Int,
    val col: Int,
    val rowSpan: Int,
    val colSpan: Int
)

@Immutable
data class GridResolutionResult(
    val isValid: Boolean,
    val placements: List<WidgetPlacement>,
    val targetPlacement: WidgetPlacement?
)

// =========================================================================
// MOTORE A MATRICE 2D SAMSUNG ONE UI 9.0
// =========================================================================

fun canFitInMatrix(
    occupied: Set<Pair<Int, Int>>,
    row: Int,
    col: Int,
    rowSpan: Int,
    colSpan: Int,
    maxRowsPerPage: Int = 0
): Boolean {
    if (row < 0 || col < 0 || col + colSpan > 4) return false
    if (maxRowsPerPage > 0) {
        val startPage = row / maxRowsPerPage
        val endPage = (row + rowSpan - 1) / maxRowsPerPage
        if (startPage != endPage) return false
    }
    for (dr in 0 until rowSpan) {
        for (dc in 0 until colSpan) {
            if (occupied.contains((row + dr) to (col + dc))) return false
        }
    }
    return true
}

fun markMatrixOccupied(
    occupied: MutableSet<Pair<Int, Int>>,
    row: Int,
    col: Int,
    rowSpan: Int,
    colSpan: Int
) {
    for (dr in 0 until rowSpan) {
        for (dc in 0 until colSpan) {
            occupied.add((row + dr) to (col + dc))
        }
    }
}

private fun rectsOverlap(
    r1: Int, c1: Int, rs1: Int, cs1: Int,
    r2: Int, c2: Int, rs2: Int, cs2: Int
): Boolean {
    return r1 < r2 + rs2 && r1 + rs1 > r2 && c1 < c2 + cs2 && c1 + cs1 > c2
}

/**
 * Normalizza il posizionamento dei widget garantendo che non vi siano collisioni né sovrapposizioni.
 * Preserva esattamente le coordinate riga/colonna definite dall'utente senza compattare indebitamente gli spazi vuoti.
 */
fun normalizeGridPlacements(widgets: List<PlacedWidget>, maxRowsPerPage: Int = 0): List<WidgetPlacement> {
    if (widgets.isEmpty()) return emptyList()
    val placements = mutableListOf<WidgetPlacement>()
    val occupied = mutableSetOf<Pair<Int, Int>>()

    for (w in widgets) {
        val rSpan = w.type.rowSpan
        val cSpan = w.type.colSpan.coerceIn(1, 4)
        val prefRow = w.gridRow
        val prefCol = w.gridCol.coerceIn(0, 4 - cSpan)

        if (prefRow >= 0 && w.gridCol >= 0 && canFitInMatrix(occupied, prefRow, prefCol, rSpan, cSpan, maxRowsPerPage)) {
            markMatrixOccupied(occupied, prefRow, prefCol, rSpan, cSpan)
            placements.add(
                WidgetPlacement(
                    widget = w.copy(gridRow = prefRow, gridCol = prefCol),
                    row = prefRow,
                    col = prefCol,
                    rowSpan = rSpan,
                    colSpan = cSpan
                )
            )
        } else {
            var r = prefRow.coerceAtLeast(0)
            var foundRow = 0
            var foundCol = 0
            var found = false

            while (!found) {
                if (maxRowsPerPage > 0 && (r % maxRowsPerPage) + rSpan > maxRowsPerPage) {
                    r = ((r / maxRowsPerPage) + 1) * maxRowsPerPage
                }
                val candidateCols = if (w.gridCol >= 0) {
                    listOf(prefCol) + (0..(4 - cSpan)).filter { it != prefCol }
                } else {
                    (0..(4 - cSpan)).toList()
                }
                for (c in candidateCols) {
                    if (canFitInMatrix(occupied, r, c, rSpan, cSpan, maxRowsPerPage)) {
                        found = true
                        foundRow = r
                        foundCol = c
                        break
                    }
                }
                if (!found) r++
            }

            markMatrixOccupied(occupied, foundRow, foundCol, rSpan, cSpan)
            placements.add(
                WidgetPlacement(
                    widget = w.copy(gridRow = foundRow, gridCol = foundCol),
                    row = foundRow,
                    col = foundCol,
                    rowSpan = rSpan,
                    colSpan = cSpan
                )
            )
        }
    }

    return placements
}

fun calculateGridPlacements(widgets: List<PlacedWidget>): List<WidgetPlacement> =
    normalizeGridPlacements(widgets)

/**
 * Risolve lo spostamento o il ridimensionamento di un widget rigorosamente all'interno della stessa pagina
 * (o della griglia continua se maxRowsPerPage <= 0).
 *
 * - Se lo spazio target è libero, posiziona il widget senza muovere gli altri.
 * - Se collide con altri widget nella pagina, prova prima lo scorrimento locale / scambio reciproco (swap)
 *   e poi una ricollocazione a minimo spostamento di pagina.
 * - Se nella pagina corrente (0 until maxRowsPerPage) non c'è spazio sufficiente per far stare tutti i widget
 *   senza espellerne alcuno fuori pagina, restituisce `isValid = false` (che mostra il contorno rosso di blocco).
 */
fun resolvePageGridOperation(
    basePlacements: List<WidgetPlacement>,
    activeWidget: PlacedWidget,
    targetRow: Int,
    targetCol: Int,
    maxRowsPerPage: Int = 0
): GridResolutionResult {
    val dRowSpan = activeWidget.type.rowSpan
    val dColSpan = activeWidget.type.colSpan.coerceIn(1, 4)

    if (maxRowsPerPage > 0 && dRowSpan > maxRowsPerPage) {
        return GridResolutionResult(
            isValid = false,
            placements = basePlacements,
            targetPlacement = null
        )
    }

    val safeCol = targetCol.coerceIn(0, 4 - dColSpan)
    val safeRow = if (maxRowsPerPage > 0) {
        targetRow.coerceIn(0, maxRowsPerPage - dRowSpan)
    } else {
        targetRow.coerceAtLeast(0)
    }

    val targetPlacement = WidgetPlacement(
        widget = activeWidget.copy(gridRow = safeRow, gridCol = safeCol),
        row = safeRow,
        col = safeCol,
        rowSpan = dRowSpan,
        colSpan = dColSpan
    )

    val existingSelf = basePlacements.find { it.widget.instanceId == activeWidget.instanceId }
    val others = basePlacements.filter { it.widget.instanceId != activeWidget.instanceId }

    val colliding = others.filter {
        rectsOverlap(safeRow, safeCol, dRowSpan, dColSpan, it.row, it.col, it.rowSpan, it.colSpan)
    }

    // Nessuna collisione: tutti gli altri widget restano immobili
    if (colliding.isEmpty()) {
        val result = (others + targetPlacement)
            .sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col })
        return GridResolutionResult(
            isValid = true,
            placements = result,
            targetPlacement = targetPlacement
        )
    }

    // Verifica rapida della capienza totale in celle della pagina
    val totalCellsNeeded = (dRowSpan * dColSpan) + others.sumOf { it.rowSpan * it.colSpan }
    if (maxRowsPerPage > 0 && totalCellsNeeded > maxRowsPerPage * 4) {
        return GridResolutionResult(
            isValid = false,
            placements = basePlacements,
            targetPlacement = targetPlacement
        )
    }

    val limitRows = if (maxRowsPerPage > 0) {
        maxRowsPerPage
    } else {
        maxOf(
            safeRow + dRowSpan + 6,
            (others.maxOfOrNull { it.row + it.rowSpan } ?: 0) + 6,
            24
        )
    }

    val nonColliding = others.filter { o -> colliding.none { it.widget.instanceId == o.widget.instanceId } }

    // 1. Caso singolo widget collidente: prova scorrimento sulla stessa riga o swap diretto
    if (colliding.size == 1) {
        val other = colliding.first()
        val occupiedByFixed = mutableSetOf<Pair<Int, Int>>()
        nonColliding.forEach {
            markMatrixOccupied(occupiedByFixed, it.row, it.col, it.rowSpan, it.colSpan)
        }
        markMatrixOccupied(occupiedByFixed, safeRow, safeCol, dRowSpan, dColSpan)

        // 1a. Scorrimento orizzontale sulla stessa riga
        val alternateCols = (0..(4 - other.colSpan))
            .filter { it != other.col }
            .sortedBy { kotlin.math.abs(it - other.col) }
        for (altCol in alternateCols) {
            if (canFitInMatrix(occupiedByFixed, other.row, altCol, other.rowSpan, other.colSpan, maxRowsPerPage)) {
                val movedOther = other.copy(
                    col = altCol,
                    widget = other.widget.copy(gridRow = other.row, gridCol = altCol)
                )
                val result = (nonColliding + listOf(movedOther, targetPlacement))
                    .sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col })
                return GridResolutionResult(
                    isValid = true,
                    placements = result,
                    targetPlacement = targetPlacement
                )
            }
        }

        // 1b. Scambio (swap) con la posizione originaria del widget attivo (se era nella stessa pagina)
        if (existingSelf != null) {
            val swapRow = existingSelf.row.coerceIn(0, (limitRows - other.rowSpan).coerceAtLeast(0))
            val swapCol = existingSelf.col.coerceIn(0, 4 - other.colSpan)
            if (canFitInMatrix(occupiedByFixed, swapRow, swapCol, other.rowSpan, other.colSpan, maxRowsPerPage)) {
                val swappedOther = other.copy(
                    row = swapRow,
                    col = swapCol,
                    widget = other.widget.copy(gridRow = swapRow, gridCol = swapCol)
                )
                val result = (nonColliding + listOf(swappedOther, targetPlacement))
                    .sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col })
                return GridResolutionResult(
                    isValid = true,
                    placements = result,
                    targetPlacement = targetPlacement
                )
            }
        }
    }

    // Helper per tentare il posizionamento di una lista ordinata di widget nella pagina
    fun tryPackOthers(orderedOthers: List<WidgetPlacement>, keepNonCollidingFixedFirst: Boolean): Pair<List<WidgetPlacement>, Int>? {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        val placed = mutableListOf<WidgetPlacement>()
        var displacementCost = 0

        markMatrixOccupied(occupied, safeRow, safeCol, dRowSpan, dColSpan)
        placed.add(targetPlacement)

        val toRelocate = mutableListOf<WidgetPlacement>()

        if (keepNonCollidingFixedFirst) {
            for (item in orderedOthers) {
                val isCollidingItem = colliding.any { it.widget.instanceId == item.widget.instanceId }
                if (!isCollidingItem && canFitInMatrix(occupied, item.row, item.col, item.rowSpan, item.colSpan, maxRowsPerPage)) {
                    markMatrixOccupied(occupied, item.row, item.col, item.rowSpan, item.colSpan)
                    placed.add(item)
                } else {
                    toRelocate.add(item)
                }
            }
        } else {
            toRelocate.addAll(orderedOthers)
        }

        for (item in toRelocate) {
            if (canFitInMatrix(occupied, item.row, item.col, item.rowSpan, item.colSpan, maxRowsPerPage)) {
                markMatrixOccupied(occupied, item.row, item.col, item.rowSpan, item.colSpan)
                placed.add(item)
                continue
            }

            val maxCandidateRow = (limitRows - item.rowSpan).coerceAtLeast(0)
            val maxCandidateCol = (4 - item.colSpan).coerceAtLeast(0)

            val candidateCells = mutableListOf<Pair<Int, Int>>()
            for (r in 0..maxCandidateRow) {
                for (c in 0..maxCandidateCol) {
                    candidateCells.add(r to c)
                }
            }

            candidateCells.sortWith(
                compareBy<Pair<Int, Int>> { (r, c) ->
                    val distOwn = kotlin.math.abs(r - item.row) * 2 + kotlin.math.abs(c - item.col)
                    val distSwap = if (existingSelf != null) {
                        kotlin.math.abs(r - existingSelf.row) * 2 + kotlin.math.abs(c - existingSelf.col) + 1
                    } else {
                        Int.MAX_VALUE
                    }
                    minOf(distOwn, distSwap)
                }.thenBy { (r, _) -> kotlin.math.abs(r - item.row) }
                    .thenBy { (_, c) -> kotlin.math.abs(c - item.col) }
                    .thenBy { (r, _) -> r }
                    .thenBy { (_, c) -> c }
            )

            var foundCell: Pair<Int, Int>? = null
            for (cell in candidateCells) {
                if (canFitInMatrix(occupied, cell.first, cell.second, item.rowSpan, item.colSpan, maxRowsPerPage)) {
                    foundCell = cell
                    break
                }
            }

            if (foundCell == null) {
                return null
            }

            markMatrixOccupied(occupied, foundCell.first, foundCell.second, item.rowSpan, item.colSpan)
            displacementCost += kotlin.math.abs(foundCell.first - item.row) * 3 +
                    kotlin.math.abs(foundCell.second - item.col) * 2 + 4
            placed.add(
                item.copy(
                    row = foundCell.first,
                    col = foundCell.second,
                    widget = item.widget.copy(gridRow = foundCell.first, gridCol = foundCell.second)
                )
            )
        }

        return placed to displacementCost
    }

    val candidateOrderings = listOf(
        // Strategia A: mantieni fissi i non-collidenti e sposta solo i collidenti (dal più grande al più piccolo)
        others.sortedWith(compareByDescending<WidgetPlacement> { it.rowSpan * it.colSpan }.thenBy { it.row }.thenBy { it.col }) to true,
        // Strategia B: collidenti per primi (così prendono il posto adiacente) e i non-collidenti scalano se necessario
        (colliding.sortedByDescending { it.rowSpan * it.colSpan } + nonColliding.sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col })) to false,
        // Strategia C: dal più grande al più piccolo (risolve incastri tra 2x2 e 1x1)
        others.sortedWith(compareByDescending<WidgetPlacement> { it.rowSpan * it.colSpan }.thenBy { it.row }.thenBy { it.col }) to false,
        // Strategia D: ordine naturale di lettura (riga, colonna)
        others.sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col }) to false
    )

    var bestSolution: Pair<List<WidgetPlacement>, Int>? = null
    for ((ordering, fixNonColliding) in candidateOrderings) {
        val attempt = tryPackOthers(ordering, fixNonColliding)
        if (attempt != null) {
            if (bestSolution == null || attempt.second < bestSolution.second) {
                bestSolution = attempt
                if (attempt.second <= 6) break
            }
        }
    }

    if (bestSolution != null) {
        return GridResolutionResult(
            isValid = true,
            placements = bestSolution.first.sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col }),
            targetPlacement = targetPlacement
        )
    }

    return GridResolutionResult(
        isValid = false,
        placements = basePlacements,
        targetPlacement = targetPlacement
    )
}

fun resolveMatrixDragLayout(
    basePlacements: List<WidgetPlacement>,
    draggedId: String,
    targetRow: Int,
    targetCol: Int,
    maxRowsPerPage: Int = 0
): List<WidgetPlacement> {
    val pDrag = basePlacements.find { it.widget.instanceId == draggedId } ?: return basePlacements
    val res = resolvePageGridOperation(
        basePlacements = basePlacements,
        activeWidget = pDrag.widget,
        targetRow = targetRow,
        targetCol = targetCol,
        maxRowsPerPage = maxRowsPerPage
    )
    return if (res.isValid) res.placements else basePlacements
}
