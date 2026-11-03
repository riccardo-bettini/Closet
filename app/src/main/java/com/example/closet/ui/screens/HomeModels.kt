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
    FUNCTION_SHORTCUT("Scorciatoia"),
    FUNCTION_QUICK_ACTIONS("Pannello Funzioni"),
    FUNCTION_PHOTO("Foto Armadio"),
    FUNCTION_MOVE("Spostamento Capi"),
    FUNCTION_LAUNDRY("Panni"),
    FUNCTION_TRIP("Viaggio"),
    FUNCTION_TRANSFERS("Registro Trasferimenti"),
    UTILITY_TORCH("Torcia"),
    UTILITY_COMPASS("Bussola"),
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

        WidgetType.FUNCTION_CALENDAR_1X1,
        WidgetType.FUNCTION_TOOLS_1X1 -> WidgetFamily.FUNCTION_SHORTCUT

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

        WidgetType.CLOCK_DIGITAL_1X2,
        WidgetType.CLOCK_DIGITAL_2X2,
        WidgetType.CLOCK_DIGITAL_2X4 -> WidgetFamily.CLOCK_DIGITAL

        WidgetType.CLOCK_ANALOG_1X1,
        WidgetType.CLOCK_ANALOG_2X2,
        WidgetType.CLOCK_ANALOG_2X4 -> WidgetFamily.CLOCK_ANALOG
    }

/**
 * Restituisce tutte le varianti dimensionali disponibili per la famiglia di questo widget.
 */
fun WidgetType.getAvailableVariants(): List<WidgetType> {
    val targetFamily = this.family
    return WidgetType.values().filter { it.family == targetFamily }
        .sortedWith(compareBy<WidgetType> { it.rowSpan * it.colSpan }.thenBy { it.colSpan })
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

fun compactEmptyMatrixRows(placements: List<WidgetPlacement>, maxRowsPerPage: Int = 0): List<WidgetPlacement> {
    if (placements.isEmpty()) return emptyList()
    if (maxRowsPerPage <= 0) {
        val maxRow = placements.maxOf { it.row + it.rowSpan }
        val rowHasContent = BooleanArray(maxRow + 1)
        for (p in placements) {
            for (r in p.row until (p.row + p.rowSpan)) {
                if (r in rowHasContent.indices) rowHasContent[r] = true
            }
        }

        val rowMapping = IntArray(maxRow + 1)
        var currentCompactRow = 0
        for (r in 0..maxRow) {
            rowMapping[r] = currentCompactRow
            if (rowHasContent[r]) {
                currentCompactRow++
            }
        }

        return placements.map { p ->
            val newRow = rowMapping[p.row]
            p.copy(
                row = newRow,
                widget = p.widget.copy(gridRow = newRow, gridCol = p.col)
            )
        }
    } else {
        // Compatta riga per riga all'interno della stessa pagina senza mai mischiare o unire pagine
        val pages = placements.groupBy { it.row / maxRowsPerPage }
        val result = mutableListOf<WidgetPlacement>()
        val sortedPageKeys = pages.keys.sorted()
        sortedPageKeys.forEachIndexed { newPageIndex, oldPageIndex ->
            val pagePlacements = pages[oldPageIndex] ?: emptyList()
            val pageStartRow = newPageIndex * maxRowsPerPage
            val oldPageStartRow = oldPageIndex * maxRowsPerPage
            val localHasContent = BooleanArray(maxRowsPerPage)
            for (p in pagePlacements) {
                val localStart = (p.row - oldPageStartRow).coerceIn(0, maxRowsPerPage - 1)
                for (r in localStart until minOf(maxRowsPerPage, localStart + p.rowSpan)) {
                    localHasContent[r] = true
                }
            }
            val localMapping = IntArray(maxRowsPerPage)
            var currentCompactLocal = 0
            for (r in 0 until maxRowsPerPage) {
                localMapping[r] = currentCompactLocal
                if (localHasContent[r]) {
                    currentCompactLocal++
                }
            }
            pagePlacements.forEach { p ->
                val localStart = (p.row - oldPageStartRow).coerceIn(0, maxRowsPerPage - 1)
                val newLocalRow = localMapping[localStart]
                val newGlobalRow = pageStartRow + newLocalRow
                result.add(
                    p.copy(
                        row = newGlobalRow,
                        widget = p.widget.copy(gridRow = newGlobalRow, gridCol = p.col)
                    )
                )
            }
        }
        return result
    }
}

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

    return compactEmptyMatrixRows(placements, maxRowsPerPage)
}

fun calculateGridPlacements(widgets: List<PlacedWidget>): List<WidgetPlacement> =
    normalizeGridPlacements(widgets)

private fun rectsOverlap(
    r1: Int, c1: Int, rs1: Int, cs1: Int,
    r2: Int, c2: Int, rs2: Int, cs2: Int
): Boolean {
    return r1 < r2 + rs2 && r1 + rs1 > r2 && c1 < c2 + cs2 && c1 + cs1 > c2
}

fun resolveMatrixDragLayout(
    basePlacements: List<WidgetPlacement>,
    draggedId: String,
    targetRow: Int,
    targetCol: Int,
    maxRowsPerPage: Int = 0
): List<WidgetPlacement> {
    val pDrag = basePlacements.find { it.widget.instanceId == draggedId } ?: return basePlacements
    val dRowSpan = pDrag.rowSpan
    val dColSpan = pDrag.colSpan
    val safeCol = targetCol.coerceIn(0, 4 - dColSpan)
    var safeRow = targetRow.coerceAtLeast(0)

    if (maxRowsPerPage > 0) {
        val page = safeRow / maxRowsPerPage
        val localRow = safeRow % maxRowsPerPage
        if (localRow + dRowSpan > maxRowsPerPage) {
            safeRow = page * maxRowsPerPage + (maxRowsPerPage - dRowSpan).coerceAtLeast(0)
        }
    }

    if (safeRow == pDrag.row && safeCol == pDrag.col) {
        return basePlacements
    }

    val others = basePlacements.filter { it.widget.instanceId != draggedId }
    val colliding = others.filter {
        rectsOverlap(safeRow, safeCol, dRowSpan, dColSpan, it.row, it.col, it.rowSpan, it.colSpan)
    }

    if (colliding.isEmpty()) {
        val result = others.toMutableList()
        result.add(
            pDrag.copy(
                row = safeRow,
                col = safeCol,
                widget = pDrag.widget.copy(gridRow = safeRow, gridCol = safeCol)
            )
        )
        return compactEmptyMatrixRows(result, maxRowsPerPage)
    }

    if (colliding.size == 1) {
        val other = colliding.first()
        val nonColliding = others.filter { it.widget.instanceId != other.widget.instanceId }
        val occupiedByRest = mutableSetOf<Pair<Int, Int>>()
        nonColliding.forEach {
            markMatrixOccupied(occupiedByRest, it.row, it.col, it.rowSpan, it.colSpan)
        }
        markMatrixOccupied(occupiedByRest, safeRow, safeCol, dRowSpan, dColSpan)

        val alternateColsOnSameRow = (0..(4 - other.colSpan)).filter { it != other.col }
        for (altCol in alternateColsOnSameRow) {
            if (canFitInMatrix(occupiedByRest, other.row, altCol, other.rowSpan, other.colSpan, maxRowsPerPage)) {
                val result = nonColliding.toMutableList()
                result.add(
                    other.copy(
                        col = altCol,
                        widget = other.widget.copy(gridRow = other.row, gridCol = altCol)
                    )
                )
                result.add(
                    pDrag.copy(
                        row = safeRow,
                        col = safeCol,
                        widget = pDrag.widget.copy(gridRow = safeRow, gridCol = safeCol)
                    )
                )
                return compactEmptyMatrixRows(result, maxRowsPerPage)
            }
        }

        val swapCol = pDrag.col.coerceIn(0, 4 - other.colSpan)
        val swapRow = pDrag.row
        if (canFitInMatrix(occupiedByRest, swapRow, swapCol, other.rowSpan, other.colSpan, maxRowsPerPage)) {
            val result = nonColliding.toMutableList()
            result.add(
                other.copy(
                    row = swapRow,
                    col = swapCol,
                    widget = other.widget.copy(gridRow = swapRow, gridCol = swapCol)
                )
            )
            result.add(
                pDrag.copy(
                    row = safeRow,
                    col = safeCol,
                    widget = pDrag.widget.copy(gridRow = safeRow, gridCol = safeCol)
                )
            )
            return compactEmptyMatrixRows(result, maxRowsPerPage)
        }
    }

    val occupied = mutableSetOf<Pair<Int, Int>>()
    val result = mutableListOf<WidgetPlacement>()

    markMatrixOccupied(occupied, safeRow, safeCol, dRowSpan, dColSpan)
    result.add(
        pDrag.copy(
            row = safeRow,
            col = safeCol,
            widget = pDrag.widget.copy(gridRow = safeRow, gridCol = safeCol)
        )
    )

    val sortedOthers = others.sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col })
    for (item in sortedOthers) {
        if (canFitInMatrix(occupied, item.row, item.col, item.rowSpan, item.colSpan, maxRowsPerPage)) {
            markMatrixOccupied(occupied, item.row, item.col, item.rowSpan, item.colSpan)
            result.add(item)
        } else {
            var r = item.row
            var foundRow = r
            var foundCol = item.col
            var found = false

            while (!found) {
                if (maxRowsPerPage > 0 && (r % maxRowsPerPage) + item.rowSpan > maxRowsPerPage) {
                    r = ((r / maxRowsPerPage) + 1) * maxRowsPerPage
                }
                val candidateCols = listOf(item.col) + (0..(4 - item.colSpan)).filter { it != item.col }
                for (c in candidateCols) {
                    if (canFitInMatrix(occupied, r, c, item.rowSpan, item.colSpan, maxRowsPerPage)) {
                        found = true
                        foundRow = r
                        foundCol = c
                        break
                    }
                }
                if (!found) r++
            }

            markMatrixOccupied(occupied, foundRow, foundCol, item.rowSpan, item.colSpan)
            result.add(
                item.copy(
                    row = foundRow,
                    col = foundCol,
                    widget = item.widget.copy(gridRow = foundRow, gridCol = foundCol)
                )
            )
        }
    }

    return compactEmptyMatrixRows(result, maxRowsPerPage)
}
