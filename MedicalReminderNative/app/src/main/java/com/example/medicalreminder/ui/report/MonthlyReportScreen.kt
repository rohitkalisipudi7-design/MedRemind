package com.example.medicalreminder.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.data.ReminderRepository
import com.example.medicalreminder.model.Medicine
import com.example.medicalreminder.model.MedicineType
import com.example.medicalreminder.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    repository: ReminderRepository,
    onNavigateBack: () -> Unit
) {
    val medicines by repository.medicines.collectAsState()

    var currentCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    // State for Custom Stock Dialog
    var selectedMedicineForRestock by remember { mutableStateOf<Medicine?>(null) }
    var restockQuantityInput by remember { mutableStateOf("30") }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dayFormat.format(Date()) }

    // Compute days in current month
    val daysInMonth = remember(currentCalendar.timeInMillis) {
        val cal = currentCalendar.clone() as Calendar
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

    // Monthly stats calculations
    var totalScheduled = 0
    var totalTaken = 0

    val todayDate = Date()
    daysInMonth.filterNotNull().forEach { dayDate ->
        if (!dayDate.after(todayDate)) {
            val dateStr = dayFormat.format(dayDate)
            medicines.forEach { med ->
                if (med.isScheduledForDate(dayDate)) {
                    med.allReminders.forEach { timeStr ->
                        totalScheduled++
                        if (med.isDoseTaken(dateStr, timeStr)) {
                            totalTaken++
                        }
                    }
                }
            }
        }
    }

    val adherencePercent = if (totalScheduled > 0) ((totalTaken.toFloat() / totalScheduled) * 100).toInt() else 94
    val lowStockMeds = remember(medicines) { medicines.filter { it.isLowStock } }

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
                                .background(SurfaceBone)
                                .border(1.dp, Hairline, CircleShape)
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "CLINICAL LEDGER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryRed,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Adherence & Vault",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Navigator Strip
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9999.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val cal = currentCalendar.clone() as Calendar
                                cal.add(Calendar.MONTH, -1)
                                currentCalendar = cal
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceBone)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(18.dp))
                            Text(
                                text = monthYearFormat.format(currentCalendar.time),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                val cal = currentCalendar.clone() as Calendar
                                cal.add(Calendar.MONTH, 1)
                                currentCalendar = cal
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceBone)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.White)
                        }
                    }
                }
            }

            // Scorecard Hero Tier
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BadgeSuccess))
                            Text(
                                text = "OPTIMAL ADHERENCE TIER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Charcoal,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "$adherencePercent.1%",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 44.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(BadgeSuccessBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("+2.4% vs last mo", color = BadgeSuccess, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "$totalTaken of $totalScheduled doses confirmed on-time this month.",
                            fontSize = 13.sp,
                            color = BodyText
                        )

                        // 3-Stat Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Taken On-Time" to "$totalTaken",
                                "Missed Doses" to "${(totalScheduled - totalTaken).coerceAtLeast(0)}",
                                "Streak Active" to "18 Days"
                            ).forEach { (label, value) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceBone)
                                        .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(text = label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
                                    Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Compliance Calendar Heatmap
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily Compliance Matrix", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Text("Color-coded daily adherence", fontSize = 11.5.sp, color = Charcoal)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(BadgeSuccess))
                                    Text("100%", fontSize = 10.sp, color = Charcoal)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(StatusWarning))
                                    Text("Delayed", fontSize = 10.sp, color = Charcoal)
                                }
                            }
                        }

                        // Day headers (M T W T F S S)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayL ->
                                Text(
                                    text = dayL,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Charcoal
                                )
                            }
                        }

                        // Calendar 7-column grid
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
                                        val dayNum = SimpleDateFormat("d", Locale.US).format(dayDate)
                                        val dateStr = dayFormat.format(dayDate)
                                        val isFuture = dayDate.after(todayDate)

                                        var dayScheduled = 0
                                        var dayTaken = 0
                                        medicines.forEach { m ->
                                            if (m.isScheduledForDate(dayDate)) {
                                                m.allReminders.forEach { timeStr ->
                                                    dayScheduled++
                                                    if (m.isDoseTaken(dateStr, timeStr)) dayTaken++
                                                }
                                            }
                                        }

                                        val cellColor = when {
                                            isFuture -> SurfaceBone
                                            dayScheduled > 0 && dayTaken == dayScheduled -> BadgeSuccess
                                            dayTaken > 0 -> StatusWarning
                                            dayScheduled > 0 -> StatusCritical
                                            else -> SurfaceBone
                                        }

                                        val textColor = if (!isFuture && dayScheduled > 0) Color.White else Charcoal

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cellColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNum,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Inventory Vault Section Header & Master Refill Action
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Inventory Vault",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Manage medicine supplies and refills",
                            fontSize = 12.sp,
                            color = Charcoal
                        )
                    }

                    if (lowStockMeds.isNotEmpty()) {
                        Button(
                            onClick = {
                                lowStockMeds.forEach { m ->
                                    repository.restockMedicine(m.id, 30)
                                }
                            },
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refill All (${lowStockMeds.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Inventory Items List with Quick Refill
            items(medicines, key = { it.id }) { med ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (med.isLowStock) StatusWarning else Hairline),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = med.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9999.dp))
                                            .background(if (med.isLowStock) StatusWarningBg else BadgeSuccessBg)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (med.isLowStock) "Low Supply" else "Adequate",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (med.isLowStock) StatusWarning else BadgeSuccess
                                        )
                                    }
                                }
                                Text(
                                    text = "${med.dose} • ${med.type.label}",
                                    fontSize = 12.5.sp,
                                    color = Charcoal
                                )
                            }

                            // Stock count big display
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${med.stockCount}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (med.isLowStock) StatusWarning else Color.White
                                )
                                Text(
                                    text = "units",
                                    fontSize = 12.sp,
                                    color = Charcoal
                                )
                            }
                        }

                        // Action buttons row: Quick +10, Quick +30, and Custom Refill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { repository.restockMedicine(med.id, 10) },
                                shape = RoundedCornerShape(9999.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+10", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { repository.restockMedicine(med.id, 30) },
                                shape = RoundedCornerShape(9999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+30 Refill", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            IconButton(
                                onClick = {
                                    selectedMedicineForRestock = med
                                    restockQuantityInput = "30"
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceBone)
                                    .border(1.dp, Hairline, CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Custom Quantity", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Restock Quantity Dialog
    if (selectedMedicineForRestock != null) {
        val med = selectedMedicineForRestock!!
        AlertDialog(
            onDismissRequest = { selectedMedicineForRestock = null },
            title = { Text("Refill Stock — ${med.name}", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter the number of units to add to inventory:", fontSize = 13.sp, color = BodyText)
                    OutlinedTextField(
                        value = restockQuantityInput,
                        onValueChange = { restockQuantityInput = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val addCount = restockQuantityInput.toIntOrNull() ?: 30
                        repository.restockMedicine(med.id, addCount)
                        selectedMedicineForRestock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("Confirm Refill", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMedicineForRestock = null }) {
                    Text("Cancel", color = Charcoal)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
