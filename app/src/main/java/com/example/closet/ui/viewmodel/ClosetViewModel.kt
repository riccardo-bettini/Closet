package com.example.closet.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.closet.ui.screens.AppTheme
import com.example.closet.ui.screens.CalendarEvent
import com.example.closet.ui.screens.ClothingItem
import com.example.closet.ui.screens.FavoriteActionType
import com.example.closet.ui.screens.IslandTabType
import com.example.closet.ui.screens.PlacedFavoriteShortcut
import com.example.closet.ui.screens.PlacedWidget
import com.example.closet.ui.screens.WidgetType
import com.example.closet.ui.screens.compactFavoriteRows
import com.example.closet.ui.screens.normalizeEmoji
import com.example.closet.ui.screens.normalizeGridPlacements
import com.example.closet.ui.screens.normalizeIslandTabs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ClothingSortOrder(val label: String) {
    CUSTOM("Personalizzato"),
    ALPHABETIC("Alfabetico (A-Z)"),
    REVERSE("Inverso (Z-A)")
}

data class ClosetFolder(
    val closetName: String,
    val folderName: String,
    val coverImageUri: Uri? = null,
    val coverEmoji: String? = null
)

data class MovedItemDetail(
    val itemId: String,
    val itemName: String,
    val quantity: Int
)

data class TransferHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val fromCloset: String,
    val toCloset: String,
    val itemsMoved: List<MovedItemDetail>
)

data class ActiveLaundryTransfer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fromCloset: String,
    val toCloset: String,
    val destinationItemIds: List<String>,
    val itemSelections: Map<String, Int> = emptyMap(),
    val endTimeMillis: Long,
    val isVisibleDimmed: Boolean,
    var isConsolidated: Boolean = false,
    val calendarEventId: String? = null
)

enum class TripEndAction(val label: String) {
    RETURN_TO_ORIGIN("Ritorna alla posizione iniziale"),
    GO_TO_CLOSET("Sposta in un armadio specifico"),
    GO_TO_LAUNDRY("Invia alla funzione Panni")
}

data class TripItemOrigin(
    val itemId: String,
    val itemName: String,
    val originalCloset: String,
    val originalFolder: String?,
    val isCountable: Boolean,
    val quantity: Int,
    val imageUri: Uri? = null,
    val emoji: String? = null,
    val tags: List<String> = emptyList()
)

data class ActiveTrip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Valigia Viaggio",
    val items: List<TripItemOrigin>,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long?,
    val endAction: TripEndAction = TripEndAction.RETURN_TO_ORIGIN,
    val targetCloset: String? = null,
    val laundryTargetCloset: String? = null,
    val laundryDurationHours: Int = 2,
    val laundryDurationMinutes: Int = 0,
    val laundryShowDimmed: Boolean = true,
    val calendarEventId: String? = null
)

class ClosetViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("closet_app_prefs", Context.MODE_PRIVATE)

    var currentTheme by mutableStateOf(AppTheme.DARK)
        private set

    var customAccentColor by mutableStateOf<Color?>(null)
        private set

    var useDistinctFunctionColors by mutableStateOf(true)
        private set

    var homeWidgets = mutableStateListOf<PlacedWidget>()
        private set

    // =========================================================================
    // SCHEDE DELL'ISOLA INFERIORE (CON PERSISTENZA AUTOMATICA)
    // =========================================================================

    val islandTabs = mutableStateListOf<IslandTabType>()

    /**
     * Restituisce automaticamente solo le funzioni che NON sono attualmente
     * presenti come schede nell'isola inferiore.
     */
    val availableFunctionsForScreen: List<String>
        get() {
            val activeIslandFunctionKeys = islandTabs.mapNotNull { it.functionKey }.toSet()
            return functionOrder.filter { it !in activeIslandFunctionKeys }
        }

    private fun getDefaultIslandTabs(): List<IslandTabType> = listOf(
        IslandTabType.FUNCTIONS,
        IslandTabType.WARDROBE,
        IslandTabType.HOME
    )

    fun updateIslandTabs(newTabs: List<IslandTabType>) {
        val normalized = normalizeIslandTabs(newTabs)
        islandTabs.clear()
        islandTabs.addAll(normalized)
        saveIslandTabsToPrefs(normalized)
    }

    // Coda di salvataggio ordinata in background (evita conflitti e corruzioni)
    private val persistScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)
    )

    private fun saveIslandTabsToPrefs(tabs: List<IslandTabType>) {
        val snapshot = tabs.toList()
        persistScope.launch {
            val arr = JSONArray()
            snapshot.forEach { arr.put(it.name) }
            prefs.edit().putString("pref_island_tabs_v1", arr.toString()).apply()
        }
    }

    private fun loadIslandTabsFromPrefs(): List<IslandTabType> {
        val raw = prefs.getString("pref_island_tabs_v1", null)
            ?: return getDefaultIslandTabs()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<IslandTabType>()
            for (i in 0 until arr.length()) {
                val tab = runCatching { IslandTabType.valueOf(arr.getString(i)) }.getOrNull()
                if (tab != null) list.add(tab)
            }
            if (list.isEmpty()) getDefaultIslandTabs() else normalizeIslandTabs(list)
        } catch (_: Exception) {
            getDefaultIslandTabs()
        }
    }

    // =========================================================================
    // STATO FUNZIONI PREFERITE (TASTO STELLA)
    // =========================================================================

    val favoriteShortcuts = mutableStateListOf<PlacedFavoriteShortcut>()

    private fun getDefaultFavoriteShortcuts(): List<PlacedFavoriteShortcut> = listOf(
        PlacedFavoriteShortcut(action = FavoriteActionType.ADD_CLOTHING, row = 0, col = 0),
        PlacedFavoriteShortcut(action = FavoriteActionType.SEARCH_WARDROBE, row = 0, col = 1),
        PlacedFavoriteShortcut(action = FavoriteActionType.LAUNDRY, row = 0, col = 2),
        PlacedFavoriteShortcut(action = FavoriteActionType.TORCH, row = 0, col = 3)
    )

    fun updateFavoriteShortcuts(newShortcuts: List<PlacedFavoriteShortcut>) {
        val compacted = compactFavoriteRows(newShortcuts)
        favoriteShortcuts.clear()
        favoriteShortcuts.addAll(compacted)
        saveFavoriteShortcutsToPrefs(compacted)
    }

    private fun saveFavoriteShortcutsToPrefs(list: List<PlacedFavoriteShortcut>) {
        val snapshot = list.toList()
        persistScope.launch {
            val jsonArray = JSONArray()
            snapshot.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("action", item.action.name)
                    put("row", item.row)
                    put("col", item.col)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("pref_favorite_shortcuts_v1", jsonArray.toString()).apply()
        }
    }

    private fun loadFavoriteShortcutsFromPrefs(): List<PlacedFavoriteShortcut> {
        val rawJson = prefs.getString("pref_favorite_shortcuts_v1", null)
            ?: return getDefaultFavoriteShortcuts()
        return try {
            val arr = JSONArray(rawJson)
            val loaded = mutableListOf<PlacedFavoriteShortcut>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val actionType = runCatching {
                    FavoriteActionType.valueOf(obj.optString("action", ""))
                }.getOrNull()
                if (actionType != null) {
                    loaded.add(
                        PlacedFavoriteShortcut(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            action = actionType,
                            row = obj.optInt("row", 0).coerceAtLeast(0),
                            col = obj.optInt("col", 0).coerceIn(0, 3)
                        )
                    )
                }
            }
            compactFavoriteRows(loaded)
        } catch (_: Exception) {
            getDefaultFavoriteShortcuts()
        }
    }

    val availableClosets = mutableStateListOf<String>()
    val closetFolders = mutableStateListOf<ClosetFolder>()
    val availableTags = mutableStateListOf<String>()
    val clothingItems = mutableStateListOf<ClothingItem>()

    val closetPhotos = mutableStateMapOf<String, Uri?>()
    val functionOrder = mutableStateListOf<String>()

    var suitcaseSourceCloset by mutableStateOf("")
        private set
    var suitcaseDestCloset by mutableStateOf("")
        private set

    val savedMoveSelections = mutableStateMapOf<String, Int>()
    val savedLaundrySelections = mutableStateMapOf<String, Int>()
    val savedTripSelections = mutableStateMapOf<String, Int>()

    val transferHistory = mutableStateListOf<TransferHistoryItem>()

    var laundryDurationEnabled by mutableStateOf(false)
    var laundryDurationHours by mutableStateOf(1)
    var laundryDurationMinutes by mutableStateOf(30)
    var laundryUseExactEndTime by mutableStateOf(false)
    var laundryExactDate by mutableStateOf(LocalDate.now())
    var laundryExactHour by mutableStateOf(LocalDateTime.now().plusHours(2).hour)
    var laundryExactMinute by mutableStateOf(0)
    var laundryShowDimmedInDestination by mutableStateOf(true)

    val activeLaundryTransfers = mutableStateListOf<ActiveLaundryTransfer>()

    var activeTrip by mutableStateOf<ActiveTrip?>(null)
        private set

    var tripDurationEnabled by mutableStateOf(true)
    var tripDurationHours by mutableStateOf(48)
    var tripDurationMinutes by mutableStateOf(0)
    var tripUseExactEndTime by mutableStateOf(false)
    var tripExactDate by mutableStateOf(LocalDate.now().plusDays(2))
    var tripExactHour by mutableStateOf(18)
    var tripExactMinute by mutableStateOf(0)
    var tripEndAction by mutableStateOf(TripEndAction.RETURN_TO_ORIGIN)
    var tripTargetCloset by mutableStateOf<String?>(null)
    var tripLaundryTargetCloset by mutableStateOf<String?>(null)
    var tripLaundryDurationHours by mutableStateOf(2)
    var tripLaundryDurationMinutes by mutableStateOf(0)
    var tripLaundryShowDimmed by mutableStateOf(true)

    var sortOrder by mutableStateOf(ClothingSortOrder.CUSTOM)
        private set

    var eventsMap by mutableStateOf<Map<LocalDate, List<CalendarEvent>>>(emptyMap())
        private set

    val trashEvents = mutableStateListOf<CalendarEvent>()

    init {
        loadTheme()
        loadAccentColor()
        loadFunctionColorsSetting()
        loadWidgets()
        loadClosets()
        loadFolders()
        loadTags()
        loadSortOrder()
        loadClothingItems()
        loadClosetPhotos()
        loadFunctionOrder()
        loadSuitcasePreferences()
        loadTransferHistory()
        loadActiveLaundryTransfers()
        loadActiveTrip()
        loadCalendarEvents()
        loadTrashEvents()
        startPeriodicCheckLoop()
        islandTabs.clear()
        islandTabs.addAll(loadIslandTabsFromPrefs())
        favoriteShortcuts.clear()
        favoriteShortcuts.addAll(loadFavoriteShortcutsFromPrefs())
    }

    private fun startPeriodicCheckLoop() {
        viewModelScope.launch {
            while (true) {
                if (activeLaundryTransfers.isNotEmpty() || activeTrip != null) {
                    checkAndCompleteLaundryTransfers()
                    checkAndCompleteActiveTrips()
                    delay(1000L)
                } else {
                    delay(5000L)
                }
            }
        }
    }

    private fun checkAndCompleteLaundryTransfers() {
        val now = System.currentTimeMillis()
        var stateChanged = false

        activeLaundryTransfers.forEach { transfer ->
            if (now >= transfer.endTimeMillis && !transfer.isConsolidated) {
                transfer.isConsolidated = true
                consolidateCountableItemsInCloset(transfer.toCloset)
                stateChanged = true
            }
        }

        val expired = activeLaundryTransfers.filter { now >= (it.endTimeMillis + 300_000L) }
        if (expired.isNotEmpty()) {
            expired.forEach { tr ->
                tr.calendarEventId?.let { evId ->
                    deleteCalendarEventById(evId)
                }
            }
            activeLaundryTransfers.removeAll(expired)
            stateChanged = true
        }

        if (stateChanged) {
            saveActiveLaundryTransfers()
        }
    }

    private fun checkAndCompleteActiveTrips() {
        val trip = activeTrip ?: return
        val end = trip.endTimeMillis ?: return
        val now = System.currentTimeMillis()

        if (now >= end) {
            finishTrip(trip)
        }
    }

    fun startTrip(
        selectedQuantities: Map<String, Int>,
        tripName: String = "Valigia Viaggio"
    ) {
        if (selectedQuantities.isEmpty()) return

        val tripItems = mutableListOf<TripItemOrigin>()
        val itemsToRemove = mutableListOf<ClothingItem>()

        clothingItems.forEach { item ->
            val qtyToPack = selectedQuantities[item.id]
            if (qtyToPack != null && qtyToPack > 0) {
                tripItems.add(
                    TripItemOrigin(
                        itemId = item.id,
                        itemName = item.name,
                        originalCloset = item.closetName,
                        originalFolder = item.folderName,
                        isCountable = item.isCountable,
                        quantity = qtyToPack,
                        imageUri = item.imageUri,
                        emoji = item.emoji,
                        tags = item.tags
                    )
                )
                if (item.isCountable) {
                    val remaining = item.quantity - qtyToPack
                    if (remaining > 0) {
                        val idx = clothingItems.indexOf(item)
                        clothingItems[idx] = item.copy(quantity = remaining)
                    } else {
                        itemsToRemove.add(item)
                    }
                } else {
                    itemsToRemove.add(item)
                }
            }
        }
        clothingItems.removeAll(itemsToRemove)
        saveClothingItems()

        var endTimeMillis: Long? = null
        var calendarEvId: String? = null

        if (tripDurationEnabled) {
            val finishDateTime = if (tripUseExactEndTime) {
                LocalDateTime.of(tripExactDate, java.time.LocalTime.of(tripExactHour, tripExactMinute))
            } else {
                LocalDateTime.now().plusHours(tripDurationHours.toLong()).plusMinutes(tripDurationMinutes.toLong())
            }
            endTimeMillis = finishDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val calEvent = CalendarEvent(
                id = "trip_event_${System.currentTimeMillis()}",
                title = "Fine Viaggio ($tripName)",
                startDate = finishDateTime.toLocalDate(),
                endDate = finishDateTime.toLocalDate(),
                startTime = finishDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                endTime = finishDateTime.plusMinutes(15).format(DateTimeFormatter.ofPattern("HH:mm")),
                color = Color(0xFF53B5A2)
            )
            saveCalendarEvent(calEvent)
            calendarEvId = calEvent.id
        }

        activeTrip = ActiveTrip(
            name = tripName,
            items = tripItems,
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = endTimeMillis,
            endAction = tripEndAction,
            targetCloset = tripTargetCloset ?: availableClosets.firstOrNull(),
            laundryTargetCloset = tripLaundryTargetCloset ?: availableClosets.firstOrNull(),
            laundryDurationHours = tripLaundryDurationHours,
            laundryDurationMinutes = tripLaundryDurationMinutes,
            laundryShowDimmed = tripLaundryShowDimmed,
            calendarEventId = calendarEvId
        )
        saveActiveTrip()
        savedTripSelections.clear()
    }

    fun finishTrip(trip: ActiveTrip, overrideAction: TripEndAction? = null) {
        val action = overrideAction ?: trip.endAction
        when (action) {
            TripEndAction.RETURN_TO_ORIGIN -> {
                trip.items.forEach { tripItem ->
                    if (tripItem.isCountable) {
                        val existing = clothingItems.firstOrNull {
                            it.name.equals(tripItem.itemName, ignoreCase = true) &&
                                    it.closetName == tripItem.originalCloset &&
                                    it.folderName == tripItem.originalFolder &&
                                    it.isCountable
                        }
                        if (existing != null) {
                            val idx = clothingItems.indexOf(existing)
                            clothingItems[idx] = existing.copy(quantity = existing.quantity + tripItem.quantity)
                        } else {
                            clothingItems.add(
                                ClothingItem(
                                    id = tripItem.itemId,
                                    name = tripItem.itemName,
                                    tags = tripItem.tags,
                                    closetName = tripItem.originalCloset,
                                    folderName = tripItem.originalFolder,
                                    isCountable = true,
                                    quantity = tripItem.quantity,
                                    imageUri = tripItem.imageUri,
                                    emoji = tripItem.emoji
                                )
                            )
                        }
                    } else {
                        clothingItems.add(
                            ClothingItem(
                                id = tripItem.itemId,
                                name = tripItem.itemName,
                                tags = tripItem.tags,
                                closetName = tripItem.originalCloset,
                                folderName = tripItem.originalFolder,
                                isCountable = false,
                                quantity = 1,
                                imageUri = tripItem.imageUri,
                                emoji = tripItem.emoji
                            )
                        )
                    }
                }
                saveClothingItems()
            }
            TripEndAction.GO_TO_CLOSET -> {
                val dest = trip.targetCloset ?: availableClosets.firstOrNull() ?: "Armadio Principale"
                val targetFolders = getFoldersForCloset(dest)
                trip.items.forEach { tripItem ->
                    val fName = tripItem.originalFolder
                    if (fName != null && !targetFolders.any { it.folderName.equals(fName, ignoreCase = true) }) {
                        val origFolder = closetFolders.firstOrNull { it.closetName == tripItem.originalCloset && it.folderName.equals(fName, ignoreCase = true) }
                        addFolder(
                            closetName = dest,
                            folderName = fName,
                            coverImageUri = origFolder?.coverImageUri,
                            coverEmoji = origFolder?.coverEmoji
                        )
                    }
                    if (tripItem.isCountable) {
                        val existing = clothingItems.firstOrNull {
                            it.name.equals(tripItem.itemName, ignoreCase = true) &&
                                    it.closetName == dest &&
                                    it.folderName == fName &&
                                    it.isCountable
                        }
                        if (existing != null) {
                            val idx = clothingItems.indexOf(existing)
                            clothingItems[idx] = existing.copy(quantity = existing.quantity + tripItem.quantity)
                        } else {
                            clothingItems.add(
                                ClothingItem(
                                    id = tripItem.itemId,
                                    name = tripItem.itemName,
                                    tags = tripItem.tags,
                                    closetName = dest,
                                    folderName = fName,
                                    isCountable = true,
                                    quantity = tripItem.quantity,
                                    imageUri = tripItem.imageUri,
                                    emoji = tripItem.emoji
                                )
                            )
                        }
                    } else {
                        clothingItems.add(
                            ClothingItem(
                                id = tripItem.itemId,
                                name = tripItem.itemName,
                                tags = tripItem.tags,
                                closetName = dest,
                                folderName = fName,
                                isCountable = false,
                                quantity = 1,
                                imageUri = tripItem.imageUri,
                                emoji = tripItem.emoji
                            )
                        )
                    }
                }
                saveClothingItems()
                consolidateCountableItemsInCloset(dest)
            }
            TripEndAction.GO_TO_LAUNDRY -> {
                val laundryDest = trip.laundryTargetCloset ?: trip.targetCloset ?: availableClosets.firstOrNull() ?: "Armadio Principale"
                val targetFolders = getFoldersForCloset(laundryDest)
                val addedIds = mutableListOf<String>()
                val itemsMap = mutableMapOf<String, Int>()

                trip.items.forEach { tripItem ->
                    val fName = tripItem.originalFolder
                    if (fName != null && !targetFolders.any { it.folderName.equals(fName, ignoreCase = true) }) {
                        val origFolder = closetFolders.firstOrNull { it.closetName == tripItem.originalCloset && it.folderName.equals(fName, ignoreCase = true) }
                        addFolder(
                            closetName = laundryDest,
                            folderName = fName,
                            coverImageUri = origFolder?.coverImageUri,
                            coverEmoji = origFolder?.coverEmoji
                        )
                    }
                    val newItem = ClothingItem(
                        id = tripItem.itemId,
                        name = tripItem.itemName,
                        tags = tripItem.tags,
                        closetName = laundryDest,
                        folderName = fName,
                        isCountable = tripItem.isCountable,
                        quantity = tripItem.quantity,
                        imageUri = tripItem.imageUri,
                        emoji = tripItem.emoji
                    )
                    clothingItems.add(newItem)
                    addedIds.add(newItem.id)
                    itemsMap[newItem.id] = tripItem.quantity
                }
                saveClothingItems()

                val finishLaundryTime = LocalDateTime.now().plusHours(trip.laundryDurationHours.toLong()).plusMinutes(trip.laundryDurationMinutes.toLong())
                val finishMillis = finishLaundryTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val calendarEvent = CalendarEvent(
                    id = "laundry_from_trip_${System.currentTimeMillis()}",
                    title = "Fine Panni ($laundryDest)",
                    startDate = finishLaundryTime.toLocalDate(),
                    endDate = finishLaundryTime.toLocalDate(),
                    startTime = finishLaundryTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    endTime = finishLaundryTime.plusMinutes(15).format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = Color(0xFF388AF6)
                )
                saveCalendarEvent(calendarEvent)

                val lTransfer = ActiveLaundryTransfer(
                    fromCloset = "Viaggio",
                    toCloset = laundryDest,
                    destinationItemIds = addedIds,
                    itemSelections = itemsMap,
                    endTimeMillis = finishMillis,
                    isVisibleDimmed = trip.laundryShowDimmed,
                    isConsolidated = false,
                    calendarEventId = calendarEvent.id
                )
                activeLaundryTransfers.add(lTransfer)
                saveActiveLaundryTransfers()
            }
        }
        trip.calendarEventId?.let { deleteCalendarEventById(it) }
        activeTrip = null
        saveActiveTrip()
    }

    fun cancelTrip() {
        activeTrip?.let { finishTrip(it, overrideAction = TripEndAction.RETURN_TO_ORIGIN) }
    }

    private fun saveActiveTrip() {
        val trip = activeTrip
        viewModelScope.launch(Dispatchers.IO) {
            if (trip == null) {
                prefs.edit().remove("active_trip_data").apply()
                return@launch
            }
            val obj = JSONObject().apply {
                put("id", trip.id)
                put("name", trip.name)
                put("startTimeMillis", trip.startTimeMillis)
                put("endTimeMillis", trip.endTimeMillis ?: -1L)
                put("endAction", trip.endAction.name)
                put("targetCloset", trip.targetCloset ?: "")
                put("laundryTargetCloset", trip.laundryTargetCloset ?: "")
                put("laundryDurationHours", trip.laundryDurationHours)
                put("laundryDurationMinutes", trip.laundryDurationMinutes)
                put("laundryShowDimmed", trip.laundryShowDimmed)
                put("calendarEventId", trip.calendarEventId ?: "")
                val itemsArr = JSONArray()
                trip.items.forEach { item ->
                    val iObj = JSONObject().apply {
                        put("itemId", item.itemId)
                        put("itemName", item.itemName)
                        put("originalCloset", item.originalCloset)
                        put("originalFolder", item.originalFolder ?: "")
                        put("isCountable", item.isCountable)
                        put("quantity", item.quantity)
                        put("imageUri", item.imageUri?.toString() ?: "")
                        put("emoji", item.emoji ?: "")
                        val tagsArr = JSONArray()
                        item.tags.forEach { tagsArr.put(it) }
                        put("tags", tagsArr)
                    }
                    itemsArr.put(iObj)
                }
                put("items", itemsArr)
            }
            prefs.edit().putString("active_trip_data", obj.toString()).apply()
        }
    }

    private fun loadActiveTrip() {
        val raw = prefs.getString("active_trip_data", null) ?: return
        try {
            val obj = JSONObject(raw)
            val endMillis = obj.getLong("endTimeMillis")
            val itemsList = mutableListOf<TripItemOrigin>()
            val itemsArr = obj.getJSONArray("items")
            for (i in 0 until itemsArr.length()) {
                val iObj = itemsArr.getJSONObject(i)
                val imgStr = iObj.optString("imageUri", "")
                val tagsList = mutableListOf<String>()
                val tagsArr = iObj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) tagsList.add(tagsArr.getString(j))
                }
                itemsList.add(
                    TripItemOrigin(
                        itemId = iObj.getString("itemId"),
                        itemName = iObj.getString("itemName"),
                        originalCloset = iObj.getString("originalCloset"),
                        originalFolder = iObj.optString("originalFolder", "").ifBlank { null },
                        isCountable = iObj.getBoolean("isCountable"),
                        quantity = iObj.getInt("quantity"),
                        imageUri = if (imgStr.isNotBlank()) Uri.parse(imgStr) else null,
                        emoji = normalizeEmoji(iObj.optString("emoji", "")),
                        tags = tagsList
                    )
                )
            }
            activeTrip = ActiveTrip(
                id = obj.getString("id"),
                name = obj.optString("name", "Valigia Viaggio"),
                items = itemsList,
                startTimeMillis = obj.optLong("startTimeMillis", System.currentTimeMillis()),
                endTimeMillis = if (endMillis > 0) endMillis else null,
                endAction = TripEndAction.valueOf(obj.optString("endAction", TripEndAction.RETURN_TO_ORIGIN.name)),
                targetCloset = obj.optString("targetCloset", "").ifBlank { null },
                laundryTargetCloset = obj.optString("laundryTargetCloset", "").ifBlank { null },
                laundryDurationHours = obj.optInt("laundryDurationHours", 2),
                laundryDurationMinutes = obj.optInt("laundryDurationMinutes", 0),
                laundryShowDimmed = obj.optBoolean("laundryShowDimmed", true),
                calendarEventId = obj.optString("calendarEventId", "").ifBlank { null }
            )
        } catch (_: Exception) {
            activeTrip = null
        }
    }

    fun completeLaundryTransferNow(transferId: String) {
        val tr = activeLaundryTransfers.firstOrNull { it.id == transferId } ?: return
        tr.isConsolidated = true
        consolidateCountableItemsInCloset(tr.toCloset)
        tr.calendarEventId?.let { deleteCalendarEventById(it) }
        activeLaundryTransfers.removeAll { it.id == transferId }
        saveActiveLaundryTransfers()
    }

    fun cancelLaundryTransfer(transferId: String) {
        val tr = activeLaundryTransfers.firstOrNull { it.id == transferId } ?: return
        val itemsMap = mutableMapOf<String, Int>()
        tr.destinationItemIds.forEach { id ->
            val itm = clothingItems.firstOrNull { it.id == id }
            if (itm != null) {
                itemsMap[id] = if (itm.isCountable) itm.quantity else 1
            }
        }
        transferSuitcaseItems(itemsMap, tr.toCloset, tr.fromCloset, recordHistory = false, isLaundry = false)
        tr.calendarEventId?.let { deleteCalendarEventById(it) }
        activeLaundryTransfers.removeAll { it.id == transferId }
        saveActiveLaundryTransfers()
    }

    fun consolidateCountableItemsInCloset(closetName: String) {
        val inTransitIds = activeLaundryTransfers
            .filter { it.endTimeMillis > System.currentTimeMillis() }
            .flatMap { it.destinationItemIds }
            .toSet()

        val itemsInCloset = clothingItems.filter { it.closetName == closetName && !inTransitIds.contains(it.id) }
        val groups = itemsInCloset.groupBy {
            Triple(
                it.name.trim().lowercase(),
                it.folderName?.trim() ?: "",
                it.tags.map { t -> t.trim().lowercase() }.sorted().joinToString(",")
            )
        }

        val itemsToRemove = mutableListOf<ClothingItem>()
        groups.forEach { (_, list) ->
            if (list.size > 1) {
                val primaryItem = list.firstOrNull { it.imageUri != null }
                    ?: list.firstOrNull { !it.emoji.isNullOrBlank() }
                    ?: list.first()
                val totalQuantity = list.sumOf { maxOf(1, it.quantity) }
                val idx = clothingItems.indexOfFirst { it.id == primaryItem.id }
                if (idx != -1) {
                    clothingItems[idx] = primaryItem.copy(
                        isCountable = true,
                        quantity = totalQuantity
                    )
                }
                itemsToRemove.addAll(list.filter { it.id != primaryItem.id })
            }
        }

        if (itemsToRemove.isNotEmpty()) {
            clothingItems.removeAll(itemsToRemove)
            saveClothingItems()
        }
    }

    fun setTheme(theme: AppTheme) {
        currentTheme = theme
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    private fun loadTheme() {
        val themeName = prefs.getString("app_theme", AppTheme.DARK.name) ?: AppTheme.DARK.name
        currentTheme = try {
            AppTheme.valueOf(themeName)
        } catch (_: Exception) {
            AppTheme.DARK
        }
    }

    fun setCustomAccentColor(color: Color) {
        customAccentColor = color
        prefs.edit().putInt("custom_accent_color", color.toArgb()).apply()
    }

    fun resetCustomAccentColor() {
        customAccentColor = null
        prefs.edit().remove("custom_accent_color").apply()
    }

    private fun loadAccentColor() {
        if (prefs.contains("custom_accent_color")) {
            val argb = prefs.getInt("custom_accent_color", 0)
            customAccentColor = Color(argb)
        } else {
            customAccentColor = null
        }
    }

    fun setDistinctFunctionColors(enabled: Boolean) {
        useDistinctFunctionColors = enabled
        prefs.edit().putBoolean("use_distinct_function_colors", enabled).apply()
    }

    private fun loadFunctionColorsSetting() {
        useDistinctFunctionColors = prefs.getBoolean("use_distinct_function_colors", true)
    }

    fun setClosetPhoto(closetName: String, uri: Uri?) {
        if (uri != null) {
            closetPhotos[closetName] = uri
        } else {
            closetPhotos.remove(closetName)
        }
        saveClosetPhotos()
    }

    private fun saveClosetPhotos() {
        val snapshot = closetPhotos.toMap()
        viewModelScope.launch(Dispatchers.IO) {
            val obj = JSONObject()
            snapshot.forEach { (closet, uri) ->
                if (uri != null) {
                    obj.put(closet, uri.toString())
                }
            }
            prefs.edit().putString("closet_panoramic_photos", obj.toString()).apply()
        }
    }

    private fun loadClosetPhotos() {
        closetPhotos.clear()
        val raw = prefs.getString("closet_panoramic_photos", null) ?: return
        try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val uriStr = obj.getString(key)
                if (uriStr.isNotBlank()) {
                    closetPhotos[key] = Uri.parse(uriStr)
                }
            }
        } catch (_: Exception) {}
    }

    fun moveFunction(fromIndex: Int, toIndex: Int) {
        if (fromIndex in functionOrder.indices && toIndex in functionOrder.indices && fromIndex != toIndex) {
            val item = functionOrder.removeAt(fromIndex)
            functionOrder.add(toIndex, item)
            saveFunctionOrder()
        }
    }

    fun moveFunction(fromKey: String, toKey: String) {
        val fromIndex = functionOrder.indexOf(fromKey)
        val toIndex = functionOrder.indexOf(toKey)
        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
            val item = functionOrder.removeAt(fromIndex)
            functionOrder.add(toIndex, item)
            saveFunctionOrder()
        }
    }

    fun updateFunctionOrder(newOrder: List<String>) {
        val distinct = newOrder.distinct()
        if (functionOrder.toList() != distinct) {
            functionOrder.clear()
            functionOrder.addAll(distinct)
            saveFunctionOrder()
        }
    }

    private fun saveFunctionOrder() {
        val snapshot = functionOrder.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { array.put(it) }
            prefs.edit().putString("functions_order", array.toString()).apply()
        }
    }

    private fun loadFunctionOrder() {
        functionOrder.clear()
        val raw = prefs.getString("functions_order", null)
        val defaultList = listOf(
            "WARDROBE",
            "CALENDAR",
            "CLOSET_PHOTOS",
            "MOVE",
            "LAUNDRY",
            "TRIP",
            "TRANSFERS_ACTIVITY",
            "TOOLS"
        )
        if (raw.isNullOrBlank()) {
            functionOrder.addAll(defaultList)
        } else {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val rawName = array.getString(i)
                    val normalized = if (rawName == "SUITCASE") "MOVE" else rawName
                    if (normalized != "OUTFIT_SUGGESTIONS" && normalized != "STATISTICS" && !functionOrder.contains(normalized)) {
                        functionOrder.add(normalized)
                    }
                }
                if (!functionOrder.contains("WARDROBE")) {
                    functionOrder.add(0, "WARDROBE")
                }
                defaultList.forEach { def ->
                    if (!functionOrder.contains(def)) functionOrder.add(def)
                }
            } catch (_: Exception) {
                functionOrder.addAll(defaultList)
            }
        }
    }

    fun setSuitcaseClosets(source: String, dest: String) {
        suitcaseSourceCloset = source
        suitcaseDestCloset = dest
        prefs.edit()
            .putString("suitcase_source_closet", source)
            .putString("suitcase_dest_closet", dest)
            .apply()
    }

    private fun loadSuitcasePreferences() {
        val savedSource = prefs.getString("suitcase_source_closet", null)
        val savedDest = prefs.getString("suitcase_dest_closet", null)
        val firstCloset = availableClosets.firstOrNull() ?: "Armadio Principale"
        val secondCloset = availableClosets.getOrNull(1) ?: firstCloset

        suitcaseSourceCloset = if (savedSource != null && availableClosets.contains(savedSource)) savedSource else firstCloset
        suitcaseDestCloset = if (savedDest != null && availableClosets.contains(savedDest)) savedDest else secondCloset
    }

    fun transferSuitcaseItems(
        itemSelections: Map<String, Int>,
        fromCloset: String,
        toCloset: String,
        recordHistory: Boolean = true,
        isLaundry: Boolean = false
    ): List<String> {
        if (fromCloset == toCloset || itemSelections.isEmpty()) return emptyList()

        val targetFolders = getFoldersForCloset(toCloset)
        val itemsToProcess = clothingItems.filter { itemSelections.containsKey(it.id) && it.closetName == fromCloset }

        itemsToProcess.forEach { item ->
            val fName = item.folderName
            if (fName != null && !targetFolders.any { it.folderName.equals(fName, ignoreCase = true) }) {
                val originalFolder = closetFolders.firstOrNull { it.closetName == fromCloset && it.folderName.equals(fName, ignoreCase = true) }
                addFolder(
                    closetName = toCloset,
                    folderName = fName,
                    coverImageUri = originalFolder?.coverImageUri,
                    coverEmoji = originalFolder?.coverEmoji
                )
            }
        }

        val newClothingList = mutableListOf<ClothingItem>()
        val movedDetails = mutableListOf<MovedItemDetail>()
        val destinationItemIds = mutableListOf<String>()

        clothingItems.forEach { currentItem ->
            val qtyToMove = itemSelections[currentItem.id]
            if (qtyToMove != null && currentItem.closetName == fromCloset) {
                if (currentItem.isCountable) {
                    val remaining = currentItem.quantity - qtyToMove
                    val newGeneratedId = java.util.UUID.randomUUID().toString()
                    if (remaining > 0) {
                        newClothingList.add(currentItem.copy(quantity = remaining))
                        newClothingList.add(
                            ClothingItem(
                                id = newGeneratedId,
                                name = currentItem.name,
                                tags = currentItem.tags,
                                closetName = toCloset,
                                folderName = currentItem.folderName,
                                isCountable = true,
                                quantity = qtyToMove,
                                color = currentItem.color,
                                imageUri = currentItem.imageUri,
                                emoji = currentItem.emoji
                            )
                        )
                        destinationItemIds.add(newGeneratedId)
                        movedDetails.add(MovedItemDetail(newGeneratedId, currentItem.name, qtyToMove))
                    } else {
                        newClothingList.add(currentItem.copy(closetName = toCloset))
                        destinationItemIds.add(currentItem.id)
                        movedDetails.add(MovedItemDetail(currentItem.id, currentItem.name, qtyToMove))
                    }
                } else {
                    newClothingList.add(currentItem.copy(closetName = toCloset))
                    destinationItemIds.add(currentItem.id)
                    movedDetails.add(MovedItemDetail(currentItem.id, currentItem.name, 1))
                }
            } else {
                newClothingList.add(currentItem)
            }
        }

        clothingItems.clear()
        clothingItems.addAll(newClothingList)
        saveClothingItems()

        setSuitcaseClosets(fromCloset, toCloset)

        if (!isLaundry) {
            consolidateCountableItemsInCloset(toCloset)
        }

        savedMoveSelections.clear()
        savedLaundrySelections.clear()

        if (recordHistory && movedDetails.isNotEmpty()) {
            val record = TransferHistoryItem(
                timestamp = LocalDateTime.now(),
                fromCloset = fromCloset,
                toCloset = toCloset,
                itemsMoved = movedDetails
            )
            transferHistory.add(0, record)
            while (transferHistory.size > 15) {
                transferHistory.removeAt(transferHistory.lastIndex)
            }
            saveTransferHistory()
        }

        return destinationItemIds
    }

    private fun saveTransferHistory() {
        val snapshot = transferHistory.take(15)
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    put("fromCloset", item.fromCloset)
                    put("toCloset", item.toCloset)
                    val detailsArr = JSONArray()
                    item.itemsMoved.forEach { detail ->
                        val dObj = JSONObject().apply {
                            put("itemId", detail.itemId)
                            put("itemName", detail.itemName)
                            put("quantity", detail.quantity)
                        }
                        detailsArr.put(dObj)
                    }
                    put("itemsMoved", detailsArr)
                }
                array.put(obj)
            }
            prefs.edit().putString("transfer_history_records_v2", array.toString()).apply()
        }
    }

    private fun loadTransferHistory() {
        transferHistory.clear()
        val raw = prefs.getString("transfer_history_records_v2", null) ?: return
        try {
            val array = JSONArray(raw)
            val limit = minOf(array.length(), 15)
            for (i in 0 until limit) {
                val obj = array.getJSONObject(i)
                val detailsList = mutableListOf<MovedItemDetail>()
                val detailsArr = obj.getJSONArray("itemsMoved")
                for (j in 0 until detailsArr.length()) {
                    val dObj = detailsArr.getJSONObject(j)
                    detailsList.add(
                        MovedItemDetail(
                            itemId = dObj.getString("itemId"),
                            itemName = dObj.getString("itemName"),
                            quantity = dObj.getInt("quantity")
                        )
                    )
                }
                transferHistory.add(
                    TransferHistoryItem(
                        id = obj.getString("id"),
                        timestamp = LocalDateTime.parse(obj.getString("timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        fromCloset = obj.getString("fromCloset"),
                        toCloset = obj.getString("toCloset"),
                        itemsMoved = detailsList
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun startLaundryTransfer(
        itemSelections: Map<String, Int>,
        fromCloset: String,
        toCloset: String,
        isLaundry: Boolean = true
    ) {
        val finishDateTime = if (!laundryDurationEnabled) {
            LocalDateTime.now().plusHours(1)
        } else if (laundryUseExactEndTime) {
            LocalDateTime.of(laundryExactDate, java.time.LocalTime.of(laundryExactHour, laundryExactMinute))
        } else {
            LocalDateTime.now().plusHours(laundryDurationHours.toLong()).plusMinutes(laundryDurationMinutes.toLong())
        }

        val endTimeMillis = finishDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val destIds = transferSuitcaseItems(itemSelections, fromCloset, toCloset, recordHistory = true, isLaundry = true)

        val titlePrefix = if (isLaundry) "Fine Panni" else "Fine Spostamento"
        val calendarEvent = CalendarEvent(
            id = "transfer_event_${System.currentTimeMillis()}",
            title = "$titlePrefix ($toCloset)",
            startDate = finishDateTime.toLocalDate(),
            endDate = finishDateTime.toLocalDate(),
            startTime = finishDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            endTime = finishDateTime.plusMinutes(15).format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color(0xFF388AF6)
        )
        saveCalendarEvent(calendarEvent)

        val transfer = ActiveLaundryTransfer(
            fromCloset = fromCloset,
            toCloset = toCloset,
            destinationItemIds = destIds,
            itemSelections = itemSelections,
            endTimeMillis = endTimeMillis,
            isVisibleDimmed = laundryShowDimmedInDestination,
            isConsolidated = false,
            calendarEventId = calendarEvent.id
        )
        activeLaundryTransfers.add(transfer)
        saveActiveLaundryTransfers()
    }

    private fun saveActiveLaundryTransfers() {
        val snapshot = activeLaundryTransfers.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { tr ->
                val obj = JSONObject().apply {
                    put("id", tr.id)
                    put("fromCloset", tr.fromCloset)
                    put("toCloset", tr.toCloset)
                    put("endTimeMillis", tr.endTimeMillis)
                    put("isVisibleDimmed", tr.isVisibleDimmed)
                    put("isConsolidated", tr.isConsolidated)
                    put("calendarEventId", tr.calendarEventId ?: "")
                    val idsArr = JSONArray()
                    tr.destinationItemIds.forEach { idsArr.put(it) }
                    put("destinationItemIds", idsArr)
                    val selObj = JSONObject()
                    tr.itemSelections.forEach { (k, v) -> selObj.put(k, v) }
                    put("itemSelections", selObj)
                }
                array.put(obj)
            }
            prefs.edit().putString("active_laundry_transfers", array.toString()).apply()
        }
    }

    private fun loadActiveLaundryTransfers() {
        activeLaundryTransfers.clear()
        val raw = prefs.getString("active_laundry_transfers", null) ?: return
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val idsList = mutableListOf<String>()
                val idsArr = obj.getJSONArray("destinationItemIds")
                for (j in 0 until idsArr.length()) {
                    idsList.add(idsArr.getString(j))
                }
                val selMap = mutableMapOf<String, Int>()
                val selObj = obj.optJSONObject("itemSelections")
                selObj?.keys()?.forEach { k ->
                    selMap[k] = selObj.getInt(k)
                }

                activeLaundryTransfers.add(
                    ActiveLaundryTransfer(
                        id = obj.getString("id"),
                        fromCloset = obj.getString("fromCloset"),
                        toCloset = obj.getString("toCloset"),
                        destinationItemIds = idsList,
                        itemSelections = selMap,
                        endTimeMillis = obj.getLong("endTimeMillis"),
                        isVisibleDimmed = obj.getBoolean("isVisibleDimmed"),
                        isConsolidated = obj.optBoolean("isConsolidated", false),
                        calendarEventId = obj.optString("calendarEventId", "").ifBlank { null }
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun saveCalendarEvent(event: CalendarEvent) {
        val updated = eventsMap.mapValues { (_, list) ->
            list.filterNot { it.id == event.id }
        }.toMutableMap()

        var cur = event.startDate
        while (!cur.isAfter(event.endDate)) {
            val list = updated[cur]?.toMutableList() ?: mutableListOf()
            list.add(event)
            updated[cur] = list
            cur = cur.plusDays(1)
        }
        eventsMap = updated.filterValues { it.isNotEmpty() }
        saveCalendarEvents()
    }

    fun deleteCalendarEventById(eventId: String) {
        val updated = eventsMap.mapValues { (_, list) ->
            list.filterNot { it.id == eventId }
        }.filterValues { it.isNotEmpty() }
        eventsMap = updated
        saveCalendarEvents()
    }

    fun deleteCalendarEvent(event: CalendarEvent) {
        deleteCalendarEventById(event.id)
        val deletedEvent = event.copy(deletedTimestamp = System.currentTimeMillis())
        trashEvents.add(0, deletedEvent)
        pruneExpiredTrashEvents()
        saveCalendarEvents()
        saveTrashEvents()
    }

    fun restoreTrashEvent(event: CalendarEvent) {
        trashEvents.removeAll { it.id == event.id }
        saveTrashEvents()
        saveCalendarEvent(event.copy(deletedTimestamp = null))
    }

    fun clearTrash() {
        trashEvents.clear()
        saveTrashEvents()
    }

    private fun pruneExpiredTrashEvents() {
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        trashEvents.removeAll { ev ->
            val delTs = ev.deletedTimestamp ?: 0L
            delTs > 0L && (now - delTs) > thirtyDaysMillis
        }
    }

    private fun saveCalendarEvents() {
        val distinctEvents = eventsMap.values.flatten().distinctBy { it.id }
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            distinctEvents.forEach { ev ->
                val obj = JSONObject().apply {
                    put("id", ev.id)
                    put("title", ev.title)
                    put("isAllDay", ev.isAllDay)
                    put("startDate", ev.startDate.toString())
                    put("endDate", ev.endDate.toString())
                    put("startTime", ev.startTime)
                    put("endTime", ev.endTime)
                    put("color", ev.color.toArgb())
                }
                array.put(obj)
            }
            prefs.edit().putString("calendar_events", array.toString()).apply()
        }
    }

    private fun loadCalendarEvents() {
        val raw = prefs.getString("calendar_events", null)
        val today = LocalDate.now()
        if (raw.isNullOrBlank()) {
            val initialList = listOf(
                CalendarEvent(
                    title = "Outfit Giornata",
                    startDate = today,
                    endDate = today,
                    startTime = "09:00",
                    endTime = "11:00",
                    color = Color(0xFF388AF6)
                )
            )
            eventsMap = mapOf(today to initialList)
        } else {
            try {
                val array = JSONArray(raw)
                val map = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val ev = CalendarEvent(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        isAllDay = obj.getBoolean("isAllDay"),
                        startDate = LocalDate.parse(obj.getString("startDate")),
                        endDate = LocalDate.parse(obj.getString("endDate")),
                        startTime = obj.optString("startTime", ""),
                        endTime = obj.optString("endTime", ""),
                        color = Color(obj.getInt("color"))
                    )
                    var cur = ev.startDate
                    while (!cur.isAfter(ev.endDate)) {
                        val list = map.getOrPut(cur) { mutableListOf() }
                        list.add(ev)
                        cur = cur.plusDays(1)
                    }
                }
                eventsMap = map
            } catch (_: Exception) {
                eventsMap = emptyMap()
            }
        }
    }

    private fun saveTrashEvents() {
        val snapshot = trashEvents.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { ev ->
                val obj = JSONObject().apply {
                    put("id", ev.id)
                    put("title", ev.title)
                    put("isAllDay", ev.isAllDay)
                    put("startDate", ev.startDate.toString())
                    put("endDate", ev.endDate.toString())
                    put("startTime", ev.startTime)
                    put("endTime", ev.endTime)
                    put("color", ev.color.toArgb())
                    put("deletedTimestamp", ev.deletedTimestamp ?: -1L)
                }
                array.put(obj)
            }
            prefs.edit().putString("trash_events", array.toString()).apply()
        }
    }

    private fun loadTrashEvents() {
        trashEvents.clear()
        val raw = prefs.getString("trash_events", null) ?: return
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val delTs = obj.optLong("deletedTimestamp", -1L)
                trashEvents.add(
                    CalendarEvent(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        isAllDay = obj.getBoolean("isAllDay"),
                        startDate = LocalDate.parse(obj.getString("startDate")),
                        endDate = LocalDate.parse(obj.getString("endDate")),
                        startTime = obj.optString("startTime", ""),
                        endTime = obj.optString("endTime", ""),
                        color = Color(obj.getInt("color")),
                        deletedTimestamp = if (delTs > 0L) delTs else null
                    )
                )
            }
            pruneExpiredTrashEvents()
        } catch (_: Exception) {}
    }

    fun clearAllData() {
        clothingItems.clear()
        closetFolders.clear()
        availableTags.clear()
        availableTags.addAll(listOf("Casual", "Sport", "Invernale", "Elegante", "Lavoro", "Estate"))
        availableClosets.clear()
        availableClosets.add("Armadio Principale")
        closetPhotos.clear()
        transferHistory.clear()
        activeLaundryTransfers.clear()
        activeTrip = null
        eventsMap = emptyMap()
        trashEvents.clear()
        savedMoveSelections.clear()
        savedLaundrySelections.clear()
        savedTripSelections.clear()

        val context = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val imagesDir = File(context.filesDir, "clothing_images")
            if (imagesDir.exists()) {
                imagesDir.deleteRecursively()
            }
            prefs.edit()
                .putString("clothing_items", "[]")
                .putString("closet_folders", "[]")
                .putString("available_closets", JSONArray().apply { put("Armadio Principale") }.toString())
                .remove("closet_panoramic_photos")
                .remove("transfer_history_records_v2")
                .remove("active_laundry_transfers")
                .remove("active_trip_data")
                .remove("calendar_events")
                .remove("trash_events")
                .putString("suitcase_source_closet", "Armadio Principale")
                .putString("suitcase_dest_closet", "Armadio Principale")
                .putString("wardrobe_selected_closet", "Armadio Principale")
                .apply()
        }

        saveTags()
        setSuitcaseClosets("Armadio Principale", "Armadio Principale")
    }

    fun resetSettingsToDefault() {
        currentTheme = AppTheme.DARK
        customAccentColor = null
        useDistinctFunctionColors = true
        functionOrder.clear()
        functionOrder.addAll(
            listOf(
                "WARDROBE",
                "CALENDAR",
                "CLOSET_PHOTOS",
                "MOVE",
                "LAUNDRY",
                "TRIP",
                "TRANSFERS_ACTIVITY",
                "TOOLS"
            )
        )

        laundryDurationEnabled = false
        laundryDurationHours = 1
        laundryDurationMinutes = 30
        laundryUseExactEndTime = false
        laundryExactDate = LocalDate.now()
        laundryExactHour = LocalDateTime.now().plusHours(2).hour
        laundryExactMinute = 0
        laundryShowDimmedInDestination = true

        tripDurationEnabled = true
        tripDurationHours = 48
        tripDurationMinutes = 0
        tripUseExactEndTime = false
        tripExactDate = LocalDate.now().plusDays(2)
        tripExactHour = 18
        tripExactMinute = 0
        tripEndAction = TripEndAction.RETURN_TO_ORIGIN
        tripTargetCloset = null
        tripLaundryTargetCloset = null
        tripLaundryDurationHours = 2
        tripLaundryDurationMinutes = 0
        tripLaundryShowDimmed = true

        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit()
                .remove("app_theme")
                .remove("custom_accent_color")
                .remove("use_distinct_function_colors")
                .remove("functions_order")
                .remove("pref_island_tabs_v1")
                .apply()
        }

        updateIslandTabs(getDefaultIslandTabs())
        updateFavoriteShortcuts(getDefaultFavoriteShortcuts())
    }

    fun addWidget(type: WidgetType, targetCloset: String? = null) {
        val currentNormalized = normalizeGridPlacements(homeWidgets)
        val occupied = mutableSetOf<Pair<Int, Int>>()
        currentNormalized.forEach { p ->
            for (dr in 0 until p.rowSpan) {
                for (dc in 0 until p.colSpan) {
                    occupied.add((p.row + dr) to (p.col + dc))
                }
            }
        }

        val rSpan = type.rowSpan
        val cSpan = type.colSpan.coerceIn(1, 4)
        var r = 0
        var foundRow = 0
        var foundCol = 0
        var found = false

        while (!found) {
            for (c in 0..(4 - cSpan)) {
                var canFit = true
                for (dr in 0 until rSpan) {
                    for (dc in 0 until cSpan) {
                        if (occupied.contains((r + dr) to (c + dc))) {
                            canFit = false
                            break
                        }
                    }
                    if (!canFit) break
                }
                if (canFit) {
                    found = true
                    foundRow = r
                    foundCol = c
                    break
                }
            }
            if (!found) r++
        }

        val updatedExisting = currentNormalized.map {
            it.widget.copy(gridRow = it.row, gridCol = it.col)
        }
        homeWidgets.clear()
        homeWidgets.addAll(updatedExisting)
        homeWidgets.add(
            PlacedWidget(
                type = type,
                targetCloset = targetCloset,
                gridRow = foundRow,
                gridCol = foundCol
            )
        )
        saveWidgets()
    }

    fun removeWidget(instanceId: String) {
        homeWidgets.removeAll { it.instanceId == instanceId }
        saveWidgets()
    }

    fun moveWidget(fromIndex: Int, toIndex: Int) {
        if (fromIndex in homeWidgets.indices && toIndex in homeWidgets.indices) {
            val item = homeWidgets.removeAt(fromIndex)
            homeWidgets.add(toIndex, item)
            saveWidgets()
        }
    }

    fun updateWidgetsLayout(newWidgets: List<PlacedWidget>) {
        homeWidgets.clear()
        homeWidgets.addAll(newWidgets)
        saveWidgets()
    }

    private fun saveWidgets() {
        val snapshot = homeWidgets.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { widget ->
                val obj = JSONObject().apply {
                    put("instanceId", widget.instanceId)
                    put("type", widget.type.name)
                    put("targetCloset", widget.targetCloset ?: "")
                    put("gridRow", widget.gridRow)
                    put("gridCol", widget.gridCol)
                }
                array.put(obj)
            }
            prefs.edit().putString("home_widgets", array.toString()).apply()
        }
    }

    private fun loadWidgets() {
        homeWidgets.clear()
        val raw = prefs.getString("home_widgets", null)
        val loaded = mutableListOf<PlacedWidget>()
        if (raw.isNullOrBlank()) {
            loaded.addAll(
                listOf(
                    PlacedWidget(type = WidgetType.CALENDAR_TODAY_2X2, gridRow = 0, gridCol = 0),
                    PlacedWidget(type = WidgetType.CALENDAR_MONTH_2X2, gridRow = 0, gridCol = 2),
                    PlacedWidget(type = WidgetType.CALENDAR_AGENDA_4X2, gridRow = 2, gridCol = 0),
                    PlacedWidget(type = WidgetType.CALENDAR_TODAY_2X1, gridRow = 4, gridCol = 0)
                )
            )
        } else {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("instanceId")
                    val type = WidgetType.valueOf(obj.getString("type"))
                    val targetCloset = obj.optString("targetCloset", "").ifBlank { null }
                    val gRow = obj.optInt("gridRow", -1)
                    val gCol = obj.optInt("gridCol", -1)
                    loaded.add(
                        PlacedWidget(
                            instanceId = id,
                            type = type,
                            targetCloset = targetCloset,
                            gridRow = gRow,
                            gridCol = gCol
                        )
                    )
                }
            } catch (_: Exception) {
                loaded.addAll(
                    listOf(
                        PlacedWidget(type = WidgetType.CALENDAR_TODAY_2X2, gridRow = 0, gridCol = 0),
                        PlacedWidget(type = WidgetType.CALENDAR_MONTH_2X2, gridRow = 0, gridCol = 2),
                        PlacedWidget(type = WidgetType.CALENDAR_AGENDA_4X2, gridRow = 2, gridCol = 0),
                        PlacedWidget(type = WidgetType.CALENDAR_TODAY_2X1, gridRow = 4, gridCol = 0)
                    )
                )
            }
        }
        val normalized = normalizeGridPlacements(loaded).map {
            it.widget.copy(gridRow = it.row, gridCol = it.col)
        }
        homeWidgets.addAll(normalized)
    }

    fun addCloset(name: String) {
        if (name.isNotBlank() && !availableClosets.contains(name.trim())) {
            availableClosets.add(name.trim())
            saveClosets()
        }
    }

    fun renameCloset(oldName: String, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank() || clean == oldName) return
        if (availableClosets.any { it.equals(clean, ignoreCase = true) }) return

        val index = availableClosets.indexOf(oldName)
        if (index != -1) {
            availableClosets[index] = clean
            saveClosets()

            val updatedItems = clothingItems.map {
                if (it.closetName == oldName) it.copy(closetName = clean) else it
            }
            clothingItems.clear()
            clothingItems.addAll(updatedItems)
            saveClothingItems()

            val updatedFolders = closetFolders.map {
                if (it.closetName == oldName) it.copy(closetName = clean) else it
            }
            closetFolders.clear()
            closetFolders.addAll(updatedFolders)
            saveFolders()

            if (closetPhotos.containsKey(oldName)) {
                val photoUri = closetPhotos.remove(oldName)
                if (photoUri != null) closetPhotos[clean] = photoUri
                saveClosetPhotos()
            }

            if (suitcaseSourceCloset == oldName) suitcaseSourceCloset = clean
            if (suitcaseDestCloset == oldName) suitcaseDestCloset = clean
            val savedWardrobeCloset = prefs.getString("wardrobe_selected_closet", null)
            viewModelScope.launch(Dispatchers.IO) {
                val editor = prefs.edit()
                    .putString("suitcase_source_closet", suitcaseSourceCloset)
                    .putString("suitcase_dest_closet", suitcaseDestCloset)
                if (savedWardrobeCloset == oldName) {
                    editor.putString("wardrobe_selected_closet", clean)
                }
                editor.apply()
            }

            val updatedWidgets = homeWidgets.map {
                if (it.targetCloset == oldName) it.copy(targetCloset = clean) else it
            }
            homeWidgets.clear()
            homeWidgets.addAll(updatedWidgets)
            saveWidgets()
        }
    }

    fun setClosetsList(newList: List<String>) {
        availableClosets.clear()
        availableClosets.addAll(newList)
        saveClosets()
    }

    fun deleteCloset(name: String) {
        if (availableClosets.size > 1) {
            availableClosets.remove(name)
            saveClosets()
            clothingItems.removeAll { it.closetName == name }
            saveClothingItems()
            closetFolders.removeAll { it.closetName == name }
            saveFolders()
            closetPhotos.remove(name)
            saveClosetPhotos()
            loadSuitcasePreferences()
        }
    }

    fun moveCloset(fromIndex: Int, toIndex: Int) {
        if (fromIndex in availableClosets.indices && toIndex in availableClosets.indices) {
            val item = availableClosets.removeAt(fromIndex)
            availableClosets.add(toIndex, item)
            saveClosets()
        }
    }

    private fun saveClosets() {
        val snapshot = availableClosets.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { array.put(it) }
            prefs.edit().putString("available_closets", array.toString()).apply()
        }
    }

    private fun loadClosets() {
        availableClosets.clear()
        val raw = prefs.getString("available_closets", null)
        if (raw.isNullOrBlank()) {
            availableClosets.add("Armadio Principale")
        } else {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    availableClosets.add(array.getString(i))
                }
                if (availableClosets.isEmpty()) {
                    availableClosets.add("Armadio Principale")
                }
            } catch (_: Exception) {
                availableClosets.add("Armadio Principale")
            }
        }
    }

    fun getFoldersForCloset(closetName: String): List<ClosetFolder> {
        return closetFolders.filter { it.closetName == closetName }
    }

    fun addFolder(closetName: String, folderName: String, coverImageUri: Uri? = null, coverEmoji: String? = null) {
        val clean = folderName.trim()
        if (clean.isNotBlank() && !closetFolders.any { it.closetName == closetName && it.folderName.equals(clean, ignoreCase = true) }) {
            closetFolders.add(ClosetFolder(closetName, clean, coverImageUri, normalizeEmoji(coverEmoji)))
            saveFolders()
        }
    }

    fun deleteFolder(closetName: String, folderName: String) {
        closetFolders.removeAll { it.closetName == closetName && it.folderName == folderName }
        saveFolders()
        val updated = clothingItems.map { item ->
            if (item.closetName == closetName && item.folderName == folderName) {
                item.copy(folderName = null)
            } else item
        }
        clothingItems.clear()
        clothingItems.addAll(updated)
        consolidateCountableItemsInCloset(closetName)
        saveClothingItems()
    }

    private fun saveFolders() {
        val snapshot = closetFolders.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { folder ->
                val obj = JSONObject().apply {
                    put("closet", folder.closetName)
                    put("folder", folder.folderName)
                    put("cover", folder.coverImageUri?.toString() ?: "")
                    put("emoji", folder.coverEmoji ?: "")
                }
                array.put(obj)
            }
            prefs.edit().putString("closet_folders", array.toString()).apply()
        }
    }

    private fun loadFolders() {
        closetFolders.clear()
        val raw = prefs.getString("closet_folders", null)
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val coverStr = obj.optString("cover", "")
                    val emojiStr = obj.optString("emoji", "")
                    closetFolders.add(
                        ClosetFolder(
                            closetName = obj.getString("closet"),
                            folderName = obj.getString("folder"),
                            coverImageUri = if (coverStr.isNotBlank()) Uri.parse(coverStr) else null,
                            coverEmoji = normalizeEmoji(emojiStr)
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun addTag(tagName: String) {
        val clean = tagName.trim()
        if (clean.isNotBlank() && !availableTags.any { it.equals(clean, ignoreCase = true) }) {
            availableTags.add(clean)
            saveTags()
        }
    }

    fun updateTag(oldName: String, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        val index = availableTags.indexOfFirst { it.equals(oldName, ignoreCase = true) }
        if (index != -1) {
            availableTags[index] = clean
            saveTags()
            val updated = clothingItems.map { item ->
                if (item.tags.contains(oldName)) {
                    val newTags = item.tags.map { if (it == oldName) clean else it }
                    item.copy(tags = newTags)
                } else item
            }
            clothingItems.clear()
            clothingItems.addAll(updated)
            saveClothingItems()
        }
    }

    fun setTagsList(newList: List<String>) {
        availableTags.clear()
        availableTags.addAll(newList)
        saveTags()
    }

    fun deleteTag(tagName: String) {
        availableTags.removeAll { it.equals(tagName, ignoreCase = true) }
        saveTags()
        val updated = clothingItems.map { item ->
            if (item.tags.contains(tagName)) {
                item.copy(tags = item.tags.filterNot { it == tagName })
            } else item
        }
        clothingItems.clear()
        clothingItems.addAll(updated)
        saveClothingItems()
    }

    private fun saveTags() {
        val snapshot = availableTags.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { array.put(it) }
            prefs.edit().putString("available_tags", array.toString()).apply()
        }
    }

    private fun loadTags() {
        availableTags.clear()
        val raw = prefs.getString("available_tags", null)
        if (raw.isNullOrBlank()) {
            availableTags.addAll(listOf("Casual", "Sport", "Invernale", "Elegante", "Lavoro", "Estate"))
        } else {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    availableTags.add(array.getString(i))
                }
            } catch (_: Exception) {
                availableTags.addAll(listOf("Casual", "Sport", "Invernale", "Elegante", "Lavoro", "Estate"))
            }
        }
    }

    fun setSortingOrder(order: ClothingSortOrder, currentCloset: String) {
        sortOrder = order
        prefs.edit().putString("clothing_sort_order", order.name).apply()

        if (order == ClothingSortOrder.ALPHABETIC) {
            val currentList = clothingItems.filter { it.closetName == currentCloset }
                .sortedBy { it.name.lowercase() }
            val otherList = clothingItems.filterNot { it.closetName == currentCloset }
            clothingItems.clear()
            clothingItems.addAll(currentList + otherList)
            saveClothingItems()
        } else if (order == ClothingSortOrder.REVERSE) {
            val currentList = clothingItems.filter { it.closetName == currentCloset }
                .sortedByDescending { it.name.lowercase() }
            val otherList = clothingItems.filterNot { it.closetName == currentCloset }
            clothingItems.clear()
            clothingItems.addAll(currentList + otherList)
            saveClothingItems()
        }
    }

    private fun loadSortOrder() {
        val raw = prefs.getString("clothing_sort_order", ClothingSortOrder.CUSTOM.name)
        sortOrder = try {
            ClothingSortOrder.valueOf(raw ?: ClothingSortOrder.CUSTOM.name)
        } catch (_: Exception) {
            ClothingSortOrder.CUSTOM
        }
    }

    fun moveClothingItem(fromId: String, toId: String) {
        if (fromId == toId) return
        val fromIndex = clothingItems.indexOfFirst { it.id == fromId }
        val toIndex = clothingItems.indexOfFirst { it.id == toId }
        if (fromIndex != -1 && toIndex != -1) {
            val item = clothingItems.removeAt(fromIndex)
            clothingItems.add(toIndex, item)
            sortOrder = ClothingSortOrder.CUSTOM
            prefs.edit().putString("clothing_sort_order", ClothingSortOrder.CUSTOM.name).apply()
            saveClothingItems()
        }
    }

    fun updateClothingOrder(orderedIds: List<String>) {
        if (orderedIds.isEmpty()) return
        val subIndices = clothingItems.mapIndexedNotNull { index, item ->
            if (orderedIds.contains(item.id)) index else null
        }
        if (subIndices.size == orderedIds.size) {
            val itemMap = clothingItems.associateBy { it.id }
            val reordered = orderedIds.mapNotNull { itemMap[it] }
            if (reordered.size == orderedIds.size) {
                subIndices.forEachIndexed { i, originalIdx ->
                    clothingItems[originalIdx] = reordered[i]
                }
                sortOrder = ClothingSortOrder.CUSTOM
                prefs.edit().putString("clothing_sort_order", ClothingSortOrder.CUSTOM.name).apply()
                saveClothingItems()
            }
        }
    }

    fun moveItemsToTarget(
        itemIds: Set<String>,
        targetCloset: String,
        targetFolder: String?,
        itemQuantities: Map<String, Int> = emptyMap()
    ) {
        val newClothingList = mutableListOf<ClothingItem>()
        clothingItems.forEach { currentItem ->
            if (itemIds.contains(currentItem.id)) {
                if (currentItem.isCountable && itemQuantities.containsKey(currentItem.id)) {
                    val qtyToMove = (itemQuantities[currentItem.id] ?: currentItem.quantity).coerceIn(1, currentItem.quantity)
                    val remaining = currentItem.quantity - qtyToMove
                    if (remaining > 0) {
                        newClothingList.add(currentItem.copy(quantity = remaining))
                        newClothingList.add(
                            currentItem.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                closetName = targetCloset,
                                folderName = targetFolder,
                                quantity = qtyToMove
                            )
                        )
                    } else {
                        newClothingList.add(currentItem.copy(closetName = targetCloset, folderName = targetFolder))
                    }
                } else {
                    newClothingList.add(currentItem.copy(closetName = targetCloset, folderName = targetFolder))
                }
            } else {
                newClothingList.add(currentItem)
            }
        }
        clothingItems.clear()
        clothingItems.addAll(newClothingList)
        consolidateCountableItemsInCloset(targetCloset)
        saveClothingItems()
    }

    fun incrementQuantity(itemId: String) {
        val index = clothingItems.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val current = clothingItems[index]
            clothingItems[index] = current.copy(quantity = current.quantity + 1)
            saveClothingItems()
        }
    }

    fun decrementQuantity(itemId: String) {
        val index = clothingItems.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val current = clothingItems[index]
            if (current.quantity > 1) {
                clothingItems[index] = current.copy(quantity = current.quantity - 1)
                saveClothingItems()
            }
        }
    }

    suspend fun savePermanentImage(tempUri: Uri?): Uri? {
        if (tempUri == null) return null
        val context = getApplication<Application>().applicationContext
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                // 1. Leggi solo le dimensioni dell'immagine senza caricarla tutta in RAM
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(tempUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, boundsOptions)
                }

                val origWidth = boundsOptions.outWidth
                val origHeight = boundsOptions.outHeight
                if (origWidth <= 0 || origHeight <= 0) return@withContext tempUri

                val maxEdge = 1080
                var sampleSize = 1
                while ((origWidth / sampleSize) > maxEdge * 2 || (origHeight / sampleSize) > maxEdge * 2) {
                    sampleSize *= 2
                }

                // 2. Decodifica l'immagine giÃ  sottocampionata a basso consumo di memoria
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val sampledBitmap = context.contentResolver.openInputStream(tempUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                } ?: return@withContext tempUri

                // 3. Scalatura finale precisa a 1080px
                val scaledBitmap = if (sampledBitmap.width > maxEdge || sampledBitmap.height > maxEdge) {
                    val ratio = minOf(maxEdge.toFloat() / sampledBitmap.width, maxEdge.toFloat() / sampledBitmap.height)
                    val w = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
                    val h = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
                    val finalBmp = Bitmap.createScaledBitmap(sampledBitmap, w, h, true)
                    if (finalBmp != sampledBitmap) {
                        sampledBitmap.recycle()
                    }
                    finalBmp
                } else {
                    sampledBitmap
                }

                val dir = File(context.filesDir, "clothing_images")
                if (!dir.exists()) dir.mkdirs()

                val permFile = File(dir, "item_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg")
                FileOutputStream(permFile).use { outputStream ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    outputStream.flush()
                }
                scaledBitmap.recycle()

                Uri.fromFile(permFile)
            } catch (_: Exception) {
                tempUri
            }
        }
    }

    fun addClothingItem(item: ClothingItem) {
        clothingItems.add(item.copy(emoji = normalizeEmoji(item.emoji)))
        saveClothingItems()
    }

    fun updateClothingItem(item: ClothingItem) {
        val index = clothingItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            clothingItems[index] = item.copy(emoji = normalizeEmoji(item.emoji))
            saveClothingItems()
        }
    }

    fun deleteClothingItem(item: ClothingItem) {
        clothingItems.removeAll { it.id == item.id }
        saveClothingItems()
        viewModelScope.launch(Dispatchers.IO) {
            item.imageUri?.path?.let { File(it).delete() }
        }
    }

    private fun saveClothingItems() {
        val snapshot = clothingItems.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            snapshot.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("closetName", item.closetName)
                    put("folderName", item.folderName ?: "")
                    put("color", Color.Transparent.toArgb())
                    put("imageUri", item.imageUri?.toString() ?: "")
                    put("emoji", item.emoji ?: "")
                    put("isCountable", item.isCountable)
                    put("quantity", item.quantity)
                    val tagsArr = JSONArray()
                    item.tags.forEach { tagsArr.put(it) }
                    put("tags", tagsArr)
                }
                array.put(obj)
            }
            prefs.edit().putString("clothing_items", array.toString()).apply()
        }
    }

    private fun loadClothingItems() {
        clothingItems.clear()
        val raw = prefs.getString("clothing_items", null)
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val imgStr = obj.optString("imageUri", "")
                    val emojiStr = obj.optString("emoji", "")
                    val fName = obj.optString("folderName", "")
                    val tagsList = mutableListOf<String>()
                    val tagsArr = obj.optJSONArray("tags")
                    if (tagsArr != null) {
                        for (t in 0 until tagsArr.length()) {
                            tagsList.add(tagsArr.getString(t))
                        }
                    }

                    clothingItems.add(
                        ClothingItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            tags = tagsList,
                            closetName = obj.getString("closetName"),
                            folderName = if (fName.isNotBlank()) fName else null,
                            color = Color.Transparent,
                            imageUri = if (imgStr.isNotBlank()) Uri.parse(imgStr) else null,
                            emoji = normalizeEmoji(emojiStr),
                            isCountable = obj.optBoolean("isCountable", false),
                            quantity = obj.optInt("quantity", 1)
                        )
                    )
                }
            } catch (_: Exception) {
                clothingItems.clear()
            }
        }
    }
}