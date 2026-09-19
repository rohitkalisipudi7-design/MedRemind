package com.example.medicalreminder.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

@Serializable
enum class MedicineType(val label: String) {
    CAPSULE("Capsule"),
    DROP("Drop"),
    TABLET("Tablet"),
    SYRUP("Syrup"),
    INJECTION("Injection");

    val displayName: String get() = label

    companion object {
        fun fromString(type: String): MedicineType {
            return entries.find { it.label.equals(type, ignoreCase = true) || it.name.equals(type, ignoreCase = true) } ?: TABLET
        }
    }
}

@Serializable
enum class ScheduleFrequency(val displayName: String) {
    EVERYDAY("Everyday"),
    ALTERNATIVE_DAYS("Alternate Days"),
    SPECIFIC_DAYS("Specific Days"),
    SPECIFIC_DATES("Calendar Dates"),
    EVERY_N_DAYS("Every Few Days")
}

@Serializable
enum class WeekDay(val shortName: String, val fullName: String, val calendarDay: Int) {
    MON("Mon", "Monday", Calendar.MONDAY),
    TUE("Tue", "Tuesday", Calendar.TUESDAY),
    WED("Wed", "Wednesday", Calendar.WEDNESDAY),
    THU("Thu", "Thursday", Calendar.THURSDAY),
    FRI("Fri", "Friday", Calendar.FRIDAY),
    SAT("Sat", "Saturday", Calendar.SATURDAY),
    SUN("Sun", "Sunday", Calendar.SUNDAY);

    companion object {
        fun fromShortName(name: String): WeekDay? {
            return entries.find { it.shortName.equals(name, ignoreCase = true) }
        }
    }
}

@Serializable
data class Medicine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: MedicineType = MedicineType.TABLET,
    val dose: String = "500mg",
    val amount: String = "1",
    val reminder: String = "08:00", // "HH:mm" format (primary/fallback)
    val reminders: List<String> = listOf("08:00"), // Multi-dose support: list of "HH:mm"
    val frequency: ScheduleFrequency = ScheduleFrequency.EVERYDAY,
    val intervalDays: Int = 2, // For EVERY_N_DAYS
    val startDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val reminderDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val specificDates: List<String> = emptyList(), // "YYYY-MM-DD" dates for SPECIFIC_DATES
    val takenOn: List<String> = emptyList(), // "YYYY-MM-DD" or "YYYY-MM-DD_HH:mm" formatted dates
    val stockCount: Int = 30, // Current inventory count
    val lowStockThreshold: Int = 3
) {
    val isLowStock: Boolean
        get() = stockCount <= lowStockThreshold

    val allReminders: List<String>
        get() = if (reminders.isNotEmpty()) reminders else listOf(reminder)

    fun isDoseTaken(dateStr: String, timeStr: String): Boolean {
        return takenOn.contains("${dateStr}_${timeStr}") || (allReminders.size == 1 && takenOn.contains(dateStr))
    }

    fun isScheduledForDate(date: Date): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val targetDateStr = sdf.format(date)
        val targetDate = sdf.parse(targetDateStr) ?: date
        val start = try {
            sdf.parse(startDate) ?: targetDate
        } catch (e: Exception) {
            targetDate
        }

        if (frequency == ScheduleFrequency.SPECIFIC_DATES) {
            return specificDates.contains(targetDateStr)
        }

        // For non-SPECIFIC_DATES frequencies, do not show if before start date
        if (targetDate.before(start)) {
            return false
        }

        return when (frequency) {
            ScheduleFrequency.EVERYDAY -> true
            ScheduleFrequency.SPECIFIC_DAYS -> {
                val dayName = SimpleDateFormat("EEE", Locale.US).format(date)
                reminderDays.any { it.equals(dayName, ignoreCase = true) }
            }
            ScheduleFrequency.SPECIFIC_DATES -> {
                specificDates.contains(targetDateStr)
            }
            ScheduleFrequency.ALTERNATIVE_DAYS -> {
                val diffMs = targetDate.time - start.time
                val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
                diffDays >= 0 && diffDays % 2 == 0
            }
            ScheduleFrequency.EVERY_N_DAYS -> {
                val step = if (intervalDays > 0) intervalDays else 1
                val diffMs = targetDate.time - start.time
                val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
                diffDays >= 0 && diffDays % step == 0
            }
        }
    }
}
