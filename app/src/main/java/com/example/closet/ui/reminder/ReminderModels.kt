package com.example.closet.ui.reminder

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val date: LocalDate? = null,
    val time: LocalTime = LocalTime.now().plusMinutes(5).withSecond(0).withNano(0),
    val recurringDays: Set<Int> = emptySet(), // 1 = Lunedì, ..., 7 = Domenica
    val hasLeadTime: Boolean = false,
    val leadTimeMinutes: Int = 15,
    val leadTimeSound: Boolean = true,
    val leadTimeVibration: Boolean = true,
    val associatedFunction: String? = null, // "WARDROBE", "CALENDAR", "CLOSET_PHOTOS", "MOVE", "LAUNDRY", "TRIP", "TRANSFERS_ACTIVITY", "TOOLS", "SETTINGS", null
    val deleteOnCompletion: Boolean = false
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("title", title)
        json.put("isEnabled", isEnabled)
        json.put("isRecurring", isRecurring)
        json.put("date", date?.format(DateTimeFormatter.ISO_LOCAL_DATE))
        json.put("time", time.format(DateTimeFormatter.ISO_LOCAL_TIME))
        
        val daysArray = JSONArray()
        recurringDays.forEach { daysArray.put(it) }
        json.put("recurringDays", daysArray)
        
        json.put("hasLeadTime", hasLeadTime)
        json.put("leadTimeMinutes", leadTimeMinutes)
        json.put("leadTimeSound", leadTimeSound)
        json.put("leadTimeVibration", leadTimeVibration)
        json.put("associatedFunction", associatedFunction ?: "")
        json.put("deleteOnCompletion", deleteOnCompletion)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ReminderItem? {
            return try {
                val id = json.optString("id", UUID.randomUUID().toString())
                val title = json.optString("title", "Promemoria")
                val isEnabled = json.optBoolean("isEnabled", true)
                val isRecurring = json.optBoolean("isRecurring", false)
                
                val dateStr = json.optString("date", "")
                val date = if (dateStr.isNotEmpty() && dateStr != "null") {
                    try { LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE) } catch (_: Exception) { null }
                } else null
                
                val timeStr = json.optString("time", "12:00:00")
                val time = try {
                    LocalTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_TIME)
                } catch (_: Exception) {
                    LocalTime.of(12, 0)
                }
                
                val recurringDays = mutableSetOf<Int>()
                val daysArray = json.optJSONArray("recurringDays")
                if (daysArray != null) {
                    for (i in 0 until daysArray.length()) {
                        recurringDays.add(daysArray.getInt(i))
                    }
                }
                
                val hasLeadTime = json.optBoolean("hasLeadTime", false)
                val leadTimeMinutes = json.optInt("leadTimeMinutes", 15)
                val leadTimeSound = json.optBoolean("leadTimeSound", true)
                val leadTimeVibration = json.optBoolean("leadTimeVibration", true)
                
                val functionStr = json.optString("associatedFunction", "")
                val associatedFunction = if (functionStr.isNotEmpty() && functionStr != "null" && functionStr != "NONE") functionStr else null
                
                val deleteOnCompletion = json.optBoolean("deleteOnCompletion", false)
                
                ReminderItem(
                    id = id,
                    title = title,
                    isEnabled = isEnabled,
                    isRecurring = isRecurring,
                    date = date,
                    time = time,
                    recurringDays = recurringDays,
                    hasLeadTime = hasLeadTime,
                    leadTimeMinutes = leadTimeMinutes,
                    leadTimeSound = leadTimeSound,
                    leadTimeVibration = leadTimeVibration,
                    associatedFunction = associatedFunction,
                    deleteOnCompletion = deleteOnCompletion
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
