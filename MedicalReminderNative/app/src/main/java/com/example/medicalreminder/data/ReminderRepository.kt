package com.example.medicalreminder.data

import android.content.Context
import android.content.SharedPreferences
import com.example.medicalreminder.alarm.AlarmScheduler
import com.example.medicalreminder.model.Medicine
import com.example.medicalreminder.model.MedicineType
import com.example.medicalreminder.model.ScheduleFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("medical_reminder_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val scheduler = AlarmScheduler(context)

    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    companion object {
        const val REGIMEN_VERSION = "v3_user_driven"
    }

    init {
        loadData()
    }

    private fun loadData() {
        val name = prefs.getString("user_name", "Patient") ?: "Patient"
        _userName.value = name

        val currentVersion = prefs.getString("regimen_version", null)
        val jsonStr = prefs.getString("medicines_json", null)

        if (jsonStr != null && currentVersion == REGIMEN_VERSION) {
            try {
                val list = json.decodeFromString<List<Medicine>>(jsonStr)
                _medicines.value = list
                // Reschedule all active saved medicines to ensure AlarmManager queue is intact
                scheduler.rescheduleAll(list)
            } catch (e: Exception) {
                initDefaultMedicines()
            }
        } else {
            initDefaultMedicines()
        }
    }

    private fun initDefaultMedicines() {
        val defaultList = emptyList<Medicine>()
        saveMedicines(defaultList)
        prefs.edit().putString("regimen_version", REGIMEN_VERSION).apply()
    }

    private fun saveMedicines(list: List<Medicine>) {
        _medicines.value = list
        val jsonStr = json.encodeToString(list)
        prefs.edit().putString("medicines_json", jsonStr).apply()
    }

    fun saveMedicine(medicine: Medicine) {
        val exists = _medicines.value.any { it.id == medicine.id }
        if (exists) {
            updateMedicine(medicine)
        } else {
            addMedicine(medicine)
        }
    }

    fun addMedicine(medicine: Medicine) {
        val updated = _medicines.value + medicine
        saveMedicines(updated)
        scheduler.scheduleMedicine(medicine)
    }

    fun updateMedicine(medicine: Medicine) {
        scheduler.cancelMedicine(medicine.id)
        val updated = _medicines.value.map { if (it.id == medicine.id) medicine else it }
        saveMedicines(updated)
        scheduler.scheduleMedicine(medicine)
    }

    fun deleteMedicine(medicineId: String) {
        scheduler.cancelMedicine(medicineId)
        val updated = _medicines.value.filter { it.id != medicineId }
        saveMedicines(updated)
    }

    fun toggleMedicineTaken(medicineId: String, date: Date, doseTime: String? = null) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        val updated = _medicines.value.map { medicine ->
            if (medicine.id == medicineId) {
                val doseAmount = medicine.amount.toIntOrNull() ?: 1
                if (doseTime != null) {
                    val intakeKey = "${dateStr}_$doseTime"
                    val isAlreadyTaken = medicine.takenOn.contains(intakeKey) || (medicine.allReminders.size == 1 && medicine.takenOn.contains(dateStr))
                    if (isAlreadyTaken) {
                        val newTaken = medicine.takenOn.filterNot { it == intakeKey || it == dateStr }
                        val newStock = medicine.stockCount + doseAmount
                        medicine.copy(takenOn = newTaken, stockCount = newStock)
                    } else {
                        val newTaken = medicine.takenOn + intakeKey
                        val newStock = (medicine.stockCount - doseAmount).coerceAtLeast(0)
                        medicine.copy(takenOn = newTaken, stockCount = newStock)
                    }
                } else {
                    val isAlreadyTaken = medicine.takenOn.contains(dateStr) || medicine.takenOn.any { it.startsWith("${dateStr}_") }
                    if (isAlreadyTaken) {
                        val newTaken = medicine.takenOn.filterNot { it == dateStr || it.startsWith("${dateStr}_") }
                        val newStock = medicine.stockCount + doseAmount
                        medicine.copy(takenOn = newTaken, stockCount = newStock)
                    } else {
                        val newTaken = medicine.takenOn + dateStr
                        val newStock = (medicine.stockCount - doseAmount).coerceAtLeast(0)
                        medicine.copy(takenOn = newTaken, stockCount = newStock)
                    }
                }
            } else {
                medicine
            }
        }
        saveMedicines(updated)
    }

    fun restockMedicine(medicineId: String, addQuantity: Int = 30) {
        val updated = _medicines.value.map { medicine ->
            if (medicine.id == medicineId) {
                medicine.copy(stockCount = medicine.stockCount + addQuantity)
            } else {
                medicine
            }
        }
        saveMedicines(updated)
    }

    fun setStock(medicineId: String, newStock: Int) {
        val updated = _medicines.value.map { medicine ->
            if (medicine.id == medicineId) {
                medicine.copy(stockCount = newStock.coerceAtLeast(0))
            } else {
                medicine
            }
        }
        saveMedicines(updated)
    }

    fun getMedicineById(id: String): Medicine? {
        return _medicines.value.find { it.id == id }
    }

    fun setUserName(name: String) {
        _userName.value = name
        prefs.edit().putString("user_name", name).apply()
    }
}
