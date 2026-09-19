package com.example.medicalreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.medicalreminder.model.Medicine
import com.example.medicalreminder.model.ScheduleFrequency
import com.example.medicalreminder.model.WeekDay
import com.example.medicalreminder.receiver.ReminderAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_MEDICINE_DOSE = "extra_medicine_dose"
        const val EXTRA_MEDICINE_TYPE = "extra_medicine_type"
        const val EXTRA_MEDICINE_TIME = "extra_medicine_time"
        const val EXTRA_WEEKDAY = "extra_weekday"
    }

    fun rescheduleAll(medicines: List<Medicine>) {
        Log.d(TAG, "Rescheduling alarms for ${medicines.size} medicines")
        medicines.forEach { medicine ->
            scheduleMedicine(medicine)
        }
    }

    fun scheduleMedicine(medicine: Medicine) {
        if (alarmManager == null) return

        val reminderTimes = medicine.allReminders

        reminderTimes.forEachIndexed { timeIndex, timeStr ->
            val parts = timeStr.split(":", " ")
            if (parts.size < 2) return@forEachIndexed

            val hour = parts[0].toIntOrNull() ?: 8
            val minute = parts[1].toIntOrNull() ?: 0

            when (medicine.frequency) {
                ScheduleFrequency.EVERYDAY -> {
                    val triggerTime = getNextDailyTriggerTime(hour, minute)
                    val requestCode = getRequestCode(medicine.id, timeIndex, 0)
                    setAlarm(medicine, timeStr, triggerTime, requestCode = requestCode)
                }
                ScheduleFrequency.SPECIFIC_DAYS -> {
                    medicine.reminderDays.forEach { dayStr ->
                        val weekDay = WeekDay.fromShortName(dayStr) ?: return@forEach
                        val triggerTime = getNextWeekDayTriggerTime(weekDay.calendarDay, hour, minute)
                        val requestCode = getRequestCode(medicine.id, timeIndex, weekDay.calendarDay)
                        setAlarm(medicine, timeStr, triggerTime, requestCode, dayStr)
                    }
                }
                ScheduleFrequency.SPECIFIC_DATES -> {
                    val now = System.currentTimeMillis()
                    medicine.specificDates.forEach { dateStr ->
                        val triggerTime = getSpecificDateTriggerTime(dateStr, hour, minute)
                        if (triggerTime > now + 30000L) {
                            val requestCode = getRequestCodeForDate(medicine.id, timeIndex, dateStr)
                            setAlarm(medicine, timeStr, triggerTime, requestCode, dateStr)
                        }
                    }
                }
                ScheduleFrequency.ALTERNATIVE_DAYS -> {
                    val triggerTime = getNextIntervalTriggerTime(medicine, intervalDays = 2, hour, minute)
                    val requestCode = getRequestCode(medicine.id, timeIndex, 99)
                    setAlarm(medicine, timeStr, triggerTime, requestCode = requestCode)
                }
                ScheduleFrequency.EVERY_N_DAYS -> {
                    val interval = if (medicine.intervalDays > 0) medicine.intervalDays else 1
                    val triggerTime = getNextIntervalTriggerTime(medicine, interval, hour, minute)
                    val requestCode = getRequestCode(medicine.id, timeIndex, 100)
                    setAlarm(medicine, timeStr, triggerTime, requestCode = requestCode)
                }
            }
        }
    }

    private fun setAlarm(
        medicine: Medicine,
        timeStr: String,
        triggerTime: Long,
        requestCode: Int,
        dayStr: String = ""
    ) {
        val mgr = alarmManager ?: return

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.example.medicalreminder.ACTION_ALARM"
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_MEDICINE_DOSE, "${medicine.amount} ${medicine.type.label}(s), ${medicine.dose}")
            putExtra(EXTRA_MEDICINE_TYPE, medicine.type.name)
            putExtra(EXTRA_MEDICINE_TIME, timeStr)
            putExtra(EXTRA_WEEKDAY, dayStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_MEDICINE_DOSE, "${medicine.amount} ${medicine.type.label}(s), ${medicine.dose}")
            putExtra(EXTRA_MEDICINE_TYPE, medicine.type.name)
            putExtra(EXTRA_MEDICINE_TIME, timeStr)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            (requestCode xor 7777),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)

        try {
            mgr.setAlarmClock(clockInfo, pendingIntent)
            Log.d(TAG, "Scheduled alarm clock for ${medicine.name} ($timeStr) at timestamp $triggerTime (rc: $requestCode)")
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (mgr.canScheduleExactAlarms()) {
                        mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else {
                    mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Log.d(TAG, "Scheduled exact fallback alarm for ${medicine.name} at timestamp $triggerTime")
            } catch (e2: Exception) {
                mgr.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.e(TAG, "Fallback to inexact alarm", e2)
            }
        }
    }

    fun cancelMedicine(medicineId: String) {
        if (alarmManager == null) return

        // Cancel across potential dose indices (0..10) and days/intervals
        for (timeIndex in 0..10) {
            // Cancel everyday/special code
            cancelPending(getRequestCode(medicineId, timeIndex, 0))
            cancelPending(getRequestCode(medicineId, timeIndex, 99))
            cancelPending(getRequestCode(medicineId, timeIndex, 100))

            // Cancel specific day alarms
            WeekDay.entries.forEach { weekDay ->
                val requestCode = getRequestCode(medicineId, timeIndex, weekDay.calendarDay)
                cancelPending(requestCode)
            }
        }

        // Cancel legacy single requestCode alarm
        cancelPending(medicineId.hashCode())
        Log.d(TAG, "Cancelled alarms for medicine $medicineId")
    }

    private fun cancelPending(requestCode: Int) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.example.medicalreminder.ACTION_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun getNextDailyTriggerTime(targetHour: Int, targetMinute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        // If target time is past or within 30 seconds of now, schedule for tomorrow
        if (calendar.timeInMillis <= now + 30000L) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun getNextWeekDayTriggerTime(targetDayOfWeek: Int, targetHour: Int, targetMinute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var daysUntilTarget = (targetDayOfWeek - currentDayOfWeek + 7) % 7

        val now = System.currentTimeMillis()
        if (daysUntilTarget == 0 && calendar.timeInMillis <= now + 30000L) {
            daysUntilTarget = 7
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysUntilTarget)
        return calendar.timeInMillis
    }

    private fun getSpecificDateTriggerTime(dateStr: String, targetHour: Int, targetMinute: Int): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try {
            sdf.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getNextIntervalTriggerTime(medicine: Medicine, intervalDays: Int, targetHour: Int, targetMinute: Int): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = try {
            sdf.parse(medicine.startDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val cal = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        while (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, intervalDays)
        }
        return cal.timeInMillis
    }

    private fun getRequestCode(medicineId: String, timeIndex: Int, dayCode: Int): Int {
        val hash = medicineId.hashCode() xor (timeIndex shl 8) xor dayCode
        return if (hash < 0) -hash else hash
    }

    private fun getRequestCodeForDate(medicineId: String, timeIndex: Int, dateStr: String): Int {
        val hash = medicineId.hashCode() xor (timeIndex shl 8) xor dateStr.hashCode()
        return if (hash < 0) -hash else hash
    }
}
