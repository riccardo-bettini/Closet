package com.example.closet.ui.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler {

    const val ACTION_TRIGGER_REMINDER = "com.example.closet.ACTION_TRIGGER_REMINDER"
    const val ACTION_SNOOZE = "com.example.closet.ACTION_SNOOZE"
    const val ACTION_COMPLETE = "com.example.closet.ACTION_COMPLETE"
    const val ACTION_REMINDERS_UPDATED = "com.example.closet.ACTION_REMINDERS_UPDATED"

    const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
    const val EXTRA_IS_LEAD_TIME = "EXTRA_IS_LEAD_TIME"

    fun calculateNextTriggerMillis(item: ReminderItem): Long? {
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()

        if (!item.isRecurring) {
            val targetDate = item.date ?: run {
                val candidateToday = LocalDateTime.of(now.toLocalDate(), item.time)
                if (candidateToday.isAfter(now)) now.toLocalDate() else now.toLocalDate().plusDays(1)
            }
            val targetDateTime = LocalDateTime.of(targetDate, item.time)
            val millis = targetDateTime.atZone(zone).toInstant().toEpochMilli()
            return if (millis > System.currentTimeMillis()) millis else null
        } else {
            val validDays = if (item.recurringDays.isNotEmpty()) item.recurringDays else (1..7).toSet()
            for (offset in 0..7) {
                val checkDate = now.toLocalDate().plusDays(offset.toLong())
                if (validDays.contains(checkDate.dayOfWeek.value)) {
                    val candidateDateTime = LocalDateTime.of(checkDate, item.time)
                    if (candidateDateTime.isAfter(now)) {
                        return candidateDateTime.atZone(zone).toInstant().toEpochMilli()
                    }
                }
            }
            return null
        }
    }

    fun calculateLeadTriggerMillis(item: ReminderItem, mainTriggerMillis: Long): Long? {
        if (!item.hasLeadTime || item.leadTimeMinutes <= 0) return null
        val leadMillis = mainTriggerMillis - (item.leadTimeMinutes * 60 * 1000L)
        return if (leadMillis > System.currentTimeMillis()) leadMillis else null
    }

    fun scheduleReminder(context: Context, item: ReminderItem) {
        cancelReminder(context, item)
        if (!item.isEnabled) return

        val mainMillis = calculateNextTriggerMillis(item) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Schedula allarme principale
        val mainIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, item.id)
            putExtra(EXTRA_IS_LEAD_TIME, false)
        }
        val mainReqCode = Math.abs(item.id.hashCode())
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            mainReqCode,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmExact(alarmManager, mainMillis, mainPendingIntent)

        // 2. Schedula eventuale preavviso
        val leadMillis = calculateLeadTriggerMillis(item, mainMillis)
        if (leadMillis != null) {
            val leadIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_TRIGGER_REMINDER
                putExtra(EXTRA_REMINDER_ID, item.id)
                putExtra(EXTRA_IS_LEAD_TIME, true)
            }
            val leadReqCode = Math.abs(item.id.hashCode()) + 1_000_000
            val leadPendingIntent = PendingIntent.getBroadcast(
                context,
                leadReqCode,
                leadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarmExact(alarmManager, leadMillis, leadPendingIntent)
        }
    }

    fun cancelReminder(context: Context, item: ReminderItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancella allarme principale
        val mainIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, item.id)
            putExtra(EXTRA_IS_LEAD_TIME, false)
        }
        val mainReqCode = Math.abs(item.id.hashCode())
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            mainReqCode,
            mainIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (mainPendingIntent != null) {
            alarmManager.cancel(mainPendingIntent)
            mainPendingIntent.cancel()
        }

        // Cancella eventuale allarme di preavviso
        val leadIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, item.id)
            putExtra(EXTRA_IS_LEAD_TIME, true)
        }
        val leadReqCode = Math.abs(item.id.hashCode()) + 1_000_000
        val leadPendingIntent = PendingIntent.getBroadcast(
            context,
            leadReqCode,
            leadIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (leadPendingIntent != null) {
            alarmManager.cancel(leadPendingIntent)
            leadPendingIntent.cancel()
        }
    }

    fun scheduleSnooze(context: Context, reminderId: String, snoozeMinutes: Int = 15) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val mainIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_IS_LEAD_TIME, false)
        }
        val mainReqCode = Math.abs(reminderId.hashCode())
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            mainReqCode,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmExact(alarmManager, targetMillis, mainPendingIntent)
    }

    fun rescheduleAll(context: Context) {
        val reminders = ReminderStorage.loadReminders(context)
        reminders.filter { it.isEnabled }.forEach { scheduleReminder(context, it) }
    }

    private fun scheduleAlarmExact(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }
}
