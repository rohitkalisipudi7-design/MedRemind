package com.example.medicalreminder.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.medicalreminder.MainActivity
import com.example.medicalreminder.MedicalReminderApp
import com.example.medicalreminder.alarm.AlarmActivity
import com.example.medicalreminder.alarm.AlarmScheduler

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ""
        
        // Handle system broadcasts and midnight maintenance that invalidate or refresh AlarmManager state
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "com.example.medicalreminder.ACTION_MAINTENANCE"
        ) {
            val repository = com.example.medicalreminder.data.ReminderRepository(context)
            val scheduler = AlarmScheduler(context)
            scheduler.rescheduleAll(repository.medicines.value)
            return
        }

        val medicineId = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Your Medicine"
        val medicineDose = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_DOSE) ?: "Time for scheduled intake"
        val medicineType = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_TYPE) ?: "TABLET"
        val medicineTime = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_TIME) ?: ""

        // Acquire temporary wake lock to ensure device is awake
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MedicalReminder:AlarmWakeLock"
        )
        wakeLock?.acquire(30000L) // Hold for 30 seconds

        val notifId = (medicineId + medicineTime).hashCode()

        // Intent for full-screen alarm activity (shown on lock screen and heads up)
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_DOSE, medicineDose)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_TYPE, medicineType)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_TIME, medicineTime)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Regular content intent (falls back to MainActivity)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (notifId xor 1234),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val notification = NotificationCompat.Builder(context, MedicalReminderApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Medicine Alert: $medicineName")
            .setContentText("Time: $medicineTime — $medicineDose")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Time to take your scheduled dose: $medicineName\n$medicineDose ($medicineTime)"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 300, 500, 300, 500))
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notifId, notification)

        // Automatically start full screen alarm activity
        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Reschedule recurring occurrence for this specific medicine only, not snoozes
        val isSnooze = intent.getBooleanExtra("is_snooze", false)
        if (!isSnooze) {
            val scheduler = AlarmScheduler(context)
            val dataStore = com.example.medicalreminder.data.ReminderRepository(context)
            val currentMedicine = dataStore.getMedicineById(medicineId)
            if (currentMedicine != null) {
                scheduler.scheduleMedicine(currentMedicine)
            }
        }
    }
}
