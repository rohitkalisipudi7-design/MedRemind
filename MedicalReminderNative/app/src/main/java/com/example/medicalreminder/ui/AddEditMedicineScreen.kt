package com.example.medicalreminder.ui

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.data.ReminderRepository
import com.example.medicalreminder.model.Medicine
import com.example.medicalreminder.model.MedicineType
import com.example.medicalreminder.model.ScheduleFrequency
import com.example.medicalreminder.model.WeekDay
import com.example.medicalreminder.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    repository: ReminderRepository,
    medicineId: String? = null,
    onNavigateBack: () -> Unit
) {
    val existingMedicine = remember(medicineId) {
        if (medicineId != null) repository.getMedicineById(medicineId) else null
    }

    var name by remember { mutableStateOf(existingMedicine?.name ?: "") }
    var selectedType by remember { mutableStateOf(existingMedicine?.type ?: MedicineType.TABLET) }
    var dose by remember { mutableStateOf(existingMedicine?.dose ?: "500mg") }
    var amount by remember { mutableStateOf(existingMedicine?.amount ?: "1") }
    var stockCountText by remember { mutableStateOf(existingMedicine?.stockCount?.toString() ?: "30") }
    var lowStockThresholdText by remember { mutableStateOf(existingMedicine?.lowStockThreshold?.toString() ?: "5") }
    var selectedFrequency by remember { mutableStateOf(existingMedicine?.frequency ?: ScheduleFrequency.EVERYDAY) }
    var intervalDaysText by remember { mutableStateOf(existingMedicine?.intervalDays?.toString() ?: "2") }

    // Multi-dose list of reminder times
    var reminderTimes by remember {
        mutableStateOf(existingMedicine?.allReminders ?: listOf("08:00"))
    }
    var editingTimeIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingNewTime by remember { mutableStateOf(false) }

    // Specific weekdays for SPECIFIC_DAYS
    var selectedDays by remember {
        mutableStateOf(existingMedicine?.reminderDays ?: listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
    }

    // Specific calendar dates for SPECIFIC_DATES
    var selectedCalendarDates by remember {
        mutableStateOf(existingMedicine?.specificDates ?: emptyList())
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Time Picker handler
    fun openTimePicker(initialTime: String, isNew: Boolean, index: Int? = null) {
        val parts = initialTime.split(":")
        val h = if (parts.size >= 2) parts[0].toIntOrNull() ?: 8 else 8
        val m = if (parts.size >= 2) parts[1].toIntOrNull() ?: 0 else 0
        isAddingNewTime = isNew
        editingTimeIndex = index

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                if (isNew) {
                    if (!reminderTimes.contains(formatted)) {
                        reminderTimes = (reminderTimes + formatted).sorted()
                    }
                } else if (index != null && index in reminderTimes.indices) {
                    val updated = reminderTimes.toMutableList()
                    updated[index] = formatted
                    reminderTimes = updated.distinct().sorted()
                }
            },
            h,
            m,
            true
        ).show()
    }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            Surface(
                color = Canvas,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceCard)
                                .border(1.dp, Hairline, CircleShape)
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (existingMedicine != null) "EDIT REGIMEN" else "NEW REGIMEN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryRed,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (existingMedicine != null) existingMedicine.name else "Add Medication",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                        }
                    }

                    if (existingMedicine != null) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ErrorContainer)
                                .clickable { showDeleteConfirm = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Sticky Save Action Bar
            Surface(
                color = SurfaceCard,
                shadowElevation = 8.dp,
                modifier = Modifier.border(width = 1.dp, color = Hairline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Charcoal)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                validationError = "Medicine name is required."
                                return@Button
                            }
                            if (reminderTimes.isEmpty()) {
                                validationError = "At least one reminder time is required."
                                return@Button
                            }
                            if (selectedFrequency == ScheduleFrequency.SPECIFIC_DATES && selectedCalendarDates.isEmpty()) {
                                validationError = "Please select at least one date from the calendar."
                                return@Button
                            }

                            val stockInt = stockCountText.toIntOrNull() ?: 30
                            val lowThreshold = lowStockThresholdText.toIntOrNull() ?: 5
                            val intervalDays = intervalDaysText.toIntOrNull() ?: 2

                            val newMed = Medicine(
                                id = existingMedicine?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                type = selectedType,
                                dose = dose.trim().ifEmpty { "1 dose" },
                                amount = amount.trim().ifEmpty { "1" },
                                reminder = reminderTimes.firstOrNull() ?: "08:00",
                                reminders = reminderTimes,
                                frequency = selectedFrequency,
                                intervalDays = intervalDays,
                                reminderDays = if (selectedFrequency == ScheduleFrequency.SPECIFIC_DAYS) selectedDays else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                                specificDates = if (selectedFrequency == ScheduleFrequency.SPECIFIC_DATES) selectedCalendarDates else emptyList(),
                                takenOn = existingMedicine?.takenOn ?: emptyList(),
                                stockCount = stockInt,
                                lowStockThreshold = lowThreshold
                            )
                            if (existingMedicine != null) {
                                repository.updateMedicine(newMed)
                            } else {
                                repository.addMedicine(newMed)
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = OnPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (existingMedicine != null) "Update Regimen" else "Save Medication",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OnPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Validation Error Alert
            if (validationError != null) {
                Surface(
                    color = ErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                        Text(text = validationError ?: "", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // SECTION 1: Identity & Delivery Form Factor
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Medicine Identity", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)

                    // Medicine Name Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("MEDICINE NAME", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Charcoal)
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                validationError = null
                            },
                            placeholder = { Text("e.g. Atorvastatin Calcium", color = Ash) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = PrimaryRed)
                            },
                            trailingIcon = {
                                if (name.isNotEmpty()) {
                                    IconButton(onClick = { name = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Charcoal)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(9999.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryRed,
                                unfocusedBorderColor = Hairline,
                                focusedContainerColor = SurfaceBone,
                                unfocusedContainerColor = SurfaceCard,
                                cursorColor = PrimaryRed
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Form Factor Strip
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("FORM FACTOR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Charcoal)
                            Text("${selectedType.label} selected", fontSize = 12.sp, color = Mute)
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(MedicineType.entries) { type ->
                                val isSelected = selectedType == type
                                val chipIcon = when (type) {
                                    MedicineType.TABLET -> Icons.Default.Medication
                                    MedicineType.CAPSULE -> Icons.Default.Vaccines
                                    MedicineType.DROP -> Icons.Default.Opacity
                                    MedicineType.SYRUP -> Icons.Default.WaterDrop
                                    MedicineType.INJECTION -> Icons.Default.Vaccines
                                }

                                Column(
                                    modifier = Modifier
                                        .width(76.dp)
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) PrimaryRed else SurfaceBone)
                                        .border(1.dp, if (isSelected) PrimaryRed else Hairline, RoundedCornerShape(14.dp))
                                        .clickable { selectedType = type }
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = chipIcon,
                                        contentDescription = type.label,
                                        tint = if (isSelected) OnPrimary else Ink,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = type.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) OnPrimary else Charcoal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: Dosage Strength & Amount
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Dosage & Intake Strength", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Strength / Dosage
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("DOSAGE STRENGTH", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Charcoal)
                            OutlinedTextField(
                                value = dose,
                                onValueChange = { dose = it },
                                placeholder = { Text("40mg", color = Ash) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                shape = RoundedCornerShape(9999.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PrimaryRed,
                                    unfocusedBorderColor = Hairline,
                                    focusedContainerColor = SurfaceBone,
                                    unfocusedContainerColor = SurfaceCard,
                                    cursorColor = PrimaryRed
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Amount Stepper
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("DOSE AMOUNT", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Charcoal)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(SurfaceBone)
                                    .border(1.dp, Hairline, RoundedCornerShape(9999.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = {
                                    val cur = amount.toDoubleOrNull() ?: 1.0
                                    if (cur > 0.5) amount = (cur - 0.5).toString().removeSuffix(".0")
                                }) {
                                    Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(
                                    text = amount,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(onClick = {
                                    val cur = amount.toDoubleOrNull() ?: 1.0
                                    amount = (cur + 0.5).toString().removeSuffix(".0")
                                }) {
                                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: Multiple Daily Doses & Alarm Times
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Dose Timing", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                            Text("Schedule multiple doses per day", fontSize = 11.5.sp, color = Charcoal)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceBone)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${reminderTimes.size} dose${if (reminderTimes.size > 1) "s" else ""}/day",
                                color = PrimaryRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Dose Presets for Fast Setup
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("QUICK DOSE PRESETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Charcoal)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "1x" to listOf("08:00"),
                                "2x" to listOf("08:00", "20:00"),
                                "3x" to listOf("08:00", "14:00", "20:00"),
                                "4x" to listOf("08:00", "12:00", "16:00", "20:00")
                            ).forEach { (label, presetTimes) ->
                                val isSelected = reminderTimes == presetTimes
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) PrimaryRed else SurfaceBone)
                                        .border(1.dp, if (isSelected) PrimaryRed else Hairline, RoundedCornerShape(10.dp))
                                        .clickable { reminderTimes = presetTimes }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) OnPrimary else Charcoal
                                    )
                                }
                            }
                        }
                    }

                    // Dose times chips list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SCHEDULED TIMES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Charcoal)

                        reminderTimes.forEachIndexed { index, timeStr ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceBone,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.clickable {
                                            openTimePicker(timeStr, isNew = false, index = index)
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = PrimaryRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Dose #${index + 1}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Charcoal
                                            )
                                            Text(
                                                text = timeStr,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { openTimePicker(timeStr, isNew = false, index = index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Time", tint = Charcoal, modifier = Modifier.size(16.dp))
                                        }

                                        if (reminderTimes.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    reminderTimes = reminderTimes.filterIndexed { i, _ -> i != index }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove Dose Time", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add Another Dose Time Action
                        OutlinedButton(
                            onClick = {
                                openTimePicker("12:00", isNew = true)
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(9999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add Another Dose Time", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }

            // SECTION 4: Schedule Frequency & Calendar Selection
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Regimen Frequency", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)

                    // 4-Way Frequency Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            ScheduleFrequency.EVERYDAY to "Everyday",
                            ScheduleFrequency.ALTERNATIVE_DAYS to "Alt Days",
                            ScheduleFrequency.SPECIFIC_DAYS to "Weekdays",
                            ScheduleFrequency.SPECIFIC_DATES to "Calendar"
                        ).forEach { (freq, label) ->
                            val isSelected = selectedFrequency == freq
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (isSelected) PrimaryRed else SurfaceBone)
                                    .border(1.dp, if (isSelected) PrimaryRed else Hairline, RoundedCornerShape(9999.dp))
                                    .clickable { selectedFrequency = freq }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) OnPrimary else Charcoal,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Option A: Specific Weekdays
                    AnimatedVisibility(visible = selectedFrequency == ScheduleFrequency.SPECIFIC_DAYS) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("SELECT ACTIVE DAYS OF THE WEEK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Charcoal)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                WeekDay.entries.forEach { day ->
                                    val isChecked = selectedDays.contains(day.shortName)
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isChecked) PrimaryRed else SurfaceBone)
                                            .border(1.dp, if (isChecked) PrimaryRed else Hairline, CircleShape)
                                            .clickable {
                                                selectedDays = if (isChecked) {
                                                    selectedDays - day.shortName
                                                } else {
                                                    selectedDays + day.shortName
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.shortName.first().toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChecked) OnPrimary else Charcoal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Option B: Interactive Calendar Dates Picker
                    AnimatedVisibility(visible = selectedFrequency == ScheduleFrequency.SPECIFIC_DATES) {
                        CalendarDatePickerView(
                            selectedDates = selectedCalendarDates,
                            onToggleDate = { dateStr ->
                                selectedCalendarDates = if (selectedCalendarDates.contains(dateStr)) {
                                    selectedCalendarDates - dateStr
                                } else {
                                    (selectedCalendarDates + dateStr).sorted()
                                }
                            },
                            onSelectPreset = { presetList ->
                                selectedCalendarDates = presetList.sorted()
                            },
                            onClearAll = {
                                selectedCalendarDates = emptyList()
                            }
                        )
                    }
                }
            }

            // SECTION 5: Inventory Tracking
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Inventory Vault Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("CURRENT STOCK", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Charcoal)
                            OutlinedTextField(
                                value = stockCountText,
                                onValueChange = { stockCountText = it.filter { ch -> ch.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                shape = RoundedCornerShape(9999.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PrimaryRed,
                                    unfocusedBorderColor = Hairline,
                                    focusedContainerColor = SurfaceBone,
                                    unfocusedContainerColor = SurfaceCard,
                                    cursorColor = PrimaryRed
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("LOW ALERT LEVEL", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Charcoal)
                            OutlinedTextField(
                                value = lowStockThresholdText,
                                onValueChange = { lowStockThresholdText = it.filter { ch -> ch.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                shape = RoundedCornerShape(9999.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PrimaryRed,
                                    unfocusedBorderColor = Hairline,
                                    focusedContainerColor = SurfaceBone,
                                    unfocusedContainerColor = SurfaceCard,
                                    cursorColor = PrimaryRed
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && existingMedicine != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Regimen?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Are you sure you want to remove ${existingMedicine.name} from your schedule?", color = BodyText) },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteMedicine(existingMedicine.id)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("Delete", color = OnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Charcoal)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Interactive Month-by-Month Multi-Date Calendar Picker
 */
@Composable
fun CalendarDatePickerView(
    selectedDates: List<String>,
    onToggleDate: (String) -> Unit,
    onSelectPreset: (List<String>) -> Unit,
    onClearAll: () -> Unit
) {
    var calendarMonth by remember {
        mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) })
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dayFormat.format(Date()) }

    // Build grid cells for the active month
    val daysInMonth = remember(calendarMonth.timeInMillis) {
        val cal = calendarMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        if (firstDayOfWeek < 0) firstDayOfWeek += 7

        val list = mutableListOf<Date?>()
        for (i in 0 until firstDayOfWeek) {
            list.add(null)
        }
        for (d in 1..maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            list.add(cal.time)
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceBone)
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val cal = calendarMonth.clone() as Calendar
                    cal.add(Calendar.MONTH, -1)
                    calendarMonth = cal
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color.White)
            }

            Text(
                text = monthYearFormat.format(calendarMonth.time),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )

            IconButton(
                onClick = {
                    val cal = calendarMonth.clone() as Calendar
                    cal.add(Calendar.MONTH, 1)
                    calendarMonth = cal
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.White)
            }
        }

        // Day of week labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal
                )
            }
        }

        // Calendar Grid
        val dayRows = daysInMonth.chunked(7)
        dayRows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until 7) {
                    val dayDate = week.getOrNull(i)
                    if (dayDate == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val dateStr = dayFormat.format(dayDate)
                        val dayNum = SimpleDateFormat("d", Locale.US).format(dayDate)
                        val isSelected = selectedDates.contains(dateStr)
                        val isToday = dateStr == todayStr

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryRed else SurfaceCard)
                                .border(
                                    width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.dp,
                                    color = if (isSelected) PrimaryRed else if (isToday) PrimaryRed.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onToggleDate(dateStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OnPrimary else if (isToday) Color.White else Charcoal
                            )
                        }
                    }
                }
            }
        }

        // Quick Presets Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val list = mutableListOf<String>()
                    val cal = Calendar.getInstance()
                    for (i in 0 until 7) {
                        list.add(dayFormat.format(cal.time))
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    onSelectPreset(list)
                },
                modifier = Modifier.weight(1f).height(34.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
            ) {
                Text("Next 7d", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = {
                    val list = mutableListOf<String>()
                    val cal = Calendar.getInstance()
                    for (i in 0 until 14) {
                        list.add(dayFormat.format(cal.time))
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    onSelectPreset(list)
                },
                modifier = Modifier.weight(1f).height(34.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
            ) {
                Text("Next 14d", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.weight(0.9f).height(34.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
            ) {
                Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
            }
        }

        // Summary selection badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(15.dp))
                Text(
                    text = "${selectedDates.size} date${if (selectedDates.size == 1) "" else "s"} selected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (selectedDates.isNotEmpty()) {
                Text(
                    text = "Custom schedule active",
                    fontSize = 11.sp,
                    color = BadgeSuccess,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

