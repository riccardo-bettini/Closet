package com.example.closet.ui.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.closet.MainActivity
import java.time.format.DateTimeFormatter

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            ReminderScheduler.ACTION_TRIGGER_REMINDER -> {
                val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
                val isLeadTime = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_LEAD_TIME, false)
                handleTriggerReminder(context, reminderId, isLeadTime)
            }

            ReminderScheduler.ACTION_SNOOZE -> {
                val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
                handleSnooze(context, reminderId)
            }

            ReminderScheduler.ACTION_COMPLETE -> {
                val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
                handleComplete(context, reminderId)
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                ReminderScheduler.rescheduleAll(context)
            }
        }
    }

    private fun handleTriggerReminder(context: Context, reminderId: String, isLeadTime: Boolean) {
        val reminders = ReminderStorage.loadReminders(context)
        val reminder = reminders.firstOrNull { it.id == reminderId } ?: return
        if (!reminder.isEnabled) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Verifica preferenze globali e preferenze di preavviso
        val globalSound = ReminderStorage.isSoundEnabled(context)
        val globalVibe = ReminderStorage.isVibrationEnabled(context)

        val soundEnabled = if (isLeadTime) (globalSound && reminder.leadTimeSound) else globalSound
        val vibrationEnabled = if (isLeadTime) (globalVibe && reminder.leadTimeVibration) else globalVibe

        val channelId = getOrCreateChannel(notificationManager, soundEnabled, vibrationEnabled)

        // Intento al tocco della notifica: apre l'app e passa l'eventuale funzione da aprire
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!reminder.associatedFunction.isNullOrBlank()) {
                putExtra("TARGET_FUNCTION", reminder.associatedFunction)
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            Math.abs(reminder.id.hashCode()),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Azione 1: Rinvia (+15 min)
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_SNOOZE
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            Math.abs(reminder.id.hashCode()) + 2_000_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Azione 2: Completa
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_COMPLETE
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            Math.abs(reminder.id.hashCode()) + 3_000_000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = reminder.time.format(DateTimeFormatter.ofPattern("HH:mm"))
        val titleText = if (isLeadTime) "Preavviso: ${reminder.title}" else reminder.title
        val functionName = getFunctionLabel(reminder.associatedFunction)
        val subtitleText = if (functionName != null) {
            "Orario: $timeStr • Apri: $functionName"
        } else {
            "Orario: $timeStr"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titleText)
            .setContentText(subtitleText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Rinvia 15m", snoozePendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Completa", completePendingIntent)

        if (soundEnabled) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(defaultSoundUri)
        } else {
            builder.setSound(null)
        }

        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 300, 150, 300))
        } else {
            builder.setVibrate(longArrayOf(0L))
        }

        // Lo stesso ID notifica sostituisce automaticamente il preavviso quando scatta la notifica principale!
        val notificationId = Math.abs(reminder.id.hashCode())
        notificationManager.notify(notificationId, builder.build())

        // Se scatta la notifica principale di un promemoria ricorrente, rischedula la prossima ricorrenza
        if (!isLeadTime && reminder.isRecurring) {
            ReminderScheduler.scheduleReminder(context, reminder)
        }
    }

    private fun handleSnooze(context: Context, reminderId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = Math.abs(reminderId.hashCode())
        notificationManager?.cancel(notificationId)

        ReminderScheduler.scheduleSnooze(context, reminderId, 15)
    }

    private fun handleComplete(context: Context, reminderId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = Math.abs(reminderId.hashCode())
        notificationManager?.cancel(notificationId)

        val currentReminders = ReminderStorage.loadReminders(context).toMutableList()
        val index = currentReminders.indexOfFirst { it.id == reminderId }
        if (index != -1) {
            val item = currentReminders[index]
            if (item.deleteOnCompletion) {
                currentReminders.removeAt(index)
                ReminderScheduler.cancelReminder(context, item)
            } else if (!item.isRecurring) {
                val updated = item.copy(isEnabled = false)
                currentReminders[index] = updated
                ReminderScheduler.cancelReminder(context, updated)
            } else {
                // Ricorrente: l'allarme per la prossima volta continua a essere programmato
                ReminderScheduler.scheduleReminder(context, item)
            }
            ReminderStorage.saveReminders(context, currentReminders)

            // Notifica all'app che i promemoria sono stati aggiornati
            val updateIntent = Intent(ReminderScheduler.ACTION_REMINDERS_UPDATED)
            context.sendBroadcast(updateIntent)
        }
    }

    private fun getOrCreateChannel(
        manager: NotificationManager,
        sound: Boolean,
        vibrate: Boolean
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return "closet_reminders_default"
        }

        val suffix = when {
            sound && vibrate -> "sound_vibe"
            sound && !vibrate -> "sound_only"
            !sound && vibrate -> "vibe_only"
            else -> "silent"
        }
        val channelId = "closet_reminders_$suffix"
        val existing = manager.getNotificationChannel(channelId)
        if (existing == null) {
            val name = "Promemoria ($suffix)"
            val importance = if (sound || vibrate) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "Notifiche dei promemoria con profilo $suffix"
                if (sound) {
                    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                    setSound(defaultSoundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
                enableVibration(vibrate)
                if (vibrate) {
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                }
            }
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    private fun getFunctionLabel(functionKey: String?): String? {
        return when (functionKey) {
            "WARDROBE" -> "Armadio"
            "CALENDAR" -> "Calendario"
            "CLOSET_PHOTOS" -> "Foto armadio"
            "MOVE" -> "Spostamento"
            "LAUNDRY" -> "Panni"
            "TRIP" -> "Viaggio"
            "TRANSFERS_ACTIVITY" -> "Trasferimenti"
            "TOOLS" -> "Strumenti"
            "SETTINGS" -> "Impostazioni"
            else -> null
        }
    }
}
