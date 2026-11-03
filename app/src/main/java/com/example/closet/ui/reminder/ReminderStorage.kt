package com.example.closet.ui.reminder

import android.content.Context
import org.json.JSONArray
import java.io.File

object ReminderStorage {
    private const val FILE_NAME = "reminders.json"
    private const val PREFS_NAME = "closet_notifications_prefs"
    private const val KEY_SOUND_ENABLED = "notifications_sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "notifications_vibration_enabled"

    fun loadReminders(context: Context): List<ReminderItem> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<ReminderItem>()
            for (i in 0 until jsonArray.length()) {
                val itemJson = jsonArray.optJSONObject(i)
                if (itemJson != null) {
                    ReminderItem.fromJson(itemJson)?.let { list.add(it) }
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReminders(context: Context, reminders: List<ReminderItem>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonArray = JSONArray()
            reminders.forEach { jsonArray.put(it.toJson()) }
            file.writeText(jsonArray.toString(2))
        } catch (_: Exception) {}
    }

    fun isSoundEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
}
