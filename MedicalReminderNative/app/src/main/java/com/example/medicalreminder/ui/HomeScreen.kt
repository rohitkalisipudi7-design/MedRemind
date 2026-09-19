package com.example.medicalreminder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

data class DayItem(
    val date: Date,
    val dayName: String,     // "Mon", "Tue"
    val dayNumber: String,   // "16"
    val isToday: Boolean
)

data class ScheduledDoseItem(
    val medicine: Medicine,
    val doseTime: String,
    val isTaken: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ReminderRepository,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToReport: () -> Unit
) {
    val medicines by repository.medicines.collectAsState()
    val userName by repository.userName.collectAsState()

    var activeFilter by remember { mutableStateOf("all") } // "all", "pending", "taken"
    var isLowStockBannerDismissed by remember { mutableStateOf(false) }

    // Generate 30-day window (-3 days to +27 days from today) so user can see all treatment cycles
    val calendarDays = remember {
        val list = mutableListOf<DayItem>()
        val todayCal = Calendar.getInstance()
        val nameFormat = SimpleDateFormat("EEE", Locale.US)
        val numFormat = SimpleDateFormat("d", Locale.US)

        for (offset in -3..27) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
            list.add(
                DayItem(
                    date = cal.time,
                    dayName = nameFormat.format(cal.time),
                    dayNumber = numFormat.format(cal.time),
                    isToday = isToday
                )
            )
        }
        list
    }

    var selectedDayIndex by remember {
        val todayIdx = calendarDays.indexOfFirst { it.isToday }
        mutableStateOf(if (todayIdx >= 0) todayIdx else 3)
    }

    val selectedDay = calendarDays.getOrElse(selectedDayIndex) { calendarDays[0] }
    val selectedDateStr = remember(selectedDay) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDay.date)
    }
    val fullDateHeaderStr = remember(selectedDay) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(selectedDay.date)
    }

    // Dynamic greeting based on current local hour
    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    // Filter medicines & expand individual daily doses for selected day
    val scheduledDoses = remember(medicines, selectedDay, selectedDateStr) {
        val list = mutableListOf<ScheduledDoseItem>()
        medicines.filter { medicine ->
            medicine.isScheduledForDate(selectedDay.date)
        }.forEach { med ->
            med.allReminders.forEach { timeStr ->
                list.add(
                    ScheduledDoseItem(
                        medicine = med,
                        doseTime = timeStr,
                        isTaken = med.isDoseTaken(selectedDateStr, timeStr)
                    )
                )
            }
        }
        list.sortedBy { it.doseTime }
    }

    val lowStockMedicines = remember(medicines) {
        medicines.filter { it.isLowStock }
    }

    val totalForDay = scheduledDoses.size
    val takenCount = scheduledDoses.count { it.isTaken }
    val remainingCount = (totalForDay - takenCount).coerceAtLeast(0)

    val displayedDoses = remember(scheduledDoses, activeFilter) {
        when (activeFilter) {
            "pending" -> scheduledDoses.filter { !it.isTaken }
            "taken" -> scheduledDoses.filter { it.isTaken }
            else -> scheduledDoses
        }
    }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            // Editorial Header Bar
            Surface(
                color = Canvas.copy(alpha = 0.95f),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo & App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = "Logo",
                                tint = OnPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MEDREMINDER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Charcoal,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Daily Schedule",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                        }
                    }

                    // User Profile Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceBone)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userName.firstOrNull() ?: 'C').toString().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = userName.ifEmpty { "Clara" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = PrimaryRed,
                contentColor = OnPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-10).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medication",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 8.dp,
                modifier = Modifier.border(width = 1.dp, color = Hairline)
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Current view */ },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Schedule") },
                    label = { Text("Schedule", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryRed,
                        selectedTextColor = PrimaryRed,
                        indicatorColor = SurfaceBone
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToReport,
                    icon = {
                        BadgedBox(
                            badge = {
                                if (lowStockMedicines.isNotEmpty()) {
                                    Badge(containerColor = PrimaryRed) {
                                        Text("${lowStockMedicines.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = "Monthly Report")
                        }
                    },
                    label = { Text("Report & Vault", fontWeight = FontWeight.Medium, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Charcoal,
                        unselectedTextColor = Charcoal
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Editorial Greeting & Adherence Counter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullDateHeaderStr.uppercase(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$greetingText, ${userName.ifEmpty { "Patient" }}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (remainingCount == 0) "All scheduled doses completed today! 🎉" else "You have $remainingCount dose${if (remainingCount > 1) "s" else ""} scheduled for this date",
                            fontSize = 13.5.sp,
                            color = Charcoal
                        )
                    }

                    // Pulse counter pill
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotScale"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceBone)
                            .border(1.dp, Hairline, RoundedCornerShape(9999.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .scale(if (remainingCount > 0) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(if (remainingCount > 0) PrimaryRed else BadgeSuccess)
                        )
                        Text(
                            text = "$takenCount of $totalForDay Taken",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                    }
                }
            }

            // 2. Low Inventory Warning Banner
            if (lowStockMedicines.isNotEmpty() && !isLowStockBannerDismissed) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceBone,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(StatusWarningBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = StatusWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Low Inventory Warning",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Ink
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9999.dp))
                                                .background(StatusWarningBg)
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Urgent",
                                                color = StatusWarning,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${lowStockMedicines.joinToString(" & ") { "${it.name} (${it.stockCount} left)" }} remaining.",
                                        fontSize = 12.5.sp,
                                        color = BodyText
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { isLowStockBannerDismissed = true },
                                    shape = RoundedCornerShape(9999.dp)
                                ) {
                                    Text("Dismiss", color = Charcoal, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = onNavigateToReport,
                                    shape = RoundedCornerShape(9999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalPharmacy,
                                        contentDescription = "Restock",
                                        modifier = Modifier.size(15.dp),
                                        tint = OnPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restock now", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Horizontal Scrollable 30-Day Calendar Strip
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceBone.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Medication Calendar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                            val todayIndex = calendarDays.indexOfFirst { it.isToday }
                            if (todayIndex >= 0 && selectedDayIndex != todayIndex) {
                                Surface(
                                    onClick = { selectedDayIndex = todayIndex },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = PrimaryRed.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Jump to Today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Text("Swipe to see dates", fontSize = 11.5.sp, color = Charcoal)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(calendarDays) { index, day ->
                                val isSelected = index == selectedDayIndex
                                val animatedBg by animateColorAsState(
                                    if (isSelected) PrimaryRed else SurfaceCard,
                                    label = "dayBg"
                                )
                                val textColor = Color.White
                                val dayNameColor = if (isSelected) Color.White.copy(alpha = 0.9f) else Charcoal

                                Column(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(animatedBg)
                                        .border(1.dp, if (isSelected) Ink else Hairline, RoundedCornerShape(12.dp))
                                        .clickable { selectedDayIndex = index }
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = day.dayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dayNameColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = day.dayNumber,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (day.isToday) PrimaryRed
                                                else if (index < selectedDayIndex) BadgeSuccess
                                                else Stone
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Regimen Header & Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Regimen",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceBone)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("all" to "All", "pending" to "Pending", "taken" to "Done").forEach { (filterKey, label) ->
                            val isChipActive = activeFilter == filterKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (isChipActive) SurfaceCard else Color.Transparent)
                                    .clickable { activeFilter = filterKey }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    fontWeight = if (isChipActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChipActive) Ink else Charcoal
                                )
                            }
                        }
                    }
                }
            }

            // 5. Medication List Items
            if (displayedDoses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Stone,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No medications scheduled for this filter.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Charcoal
                            )
                        }
                    }
                }
            } else {
                items(displayedDoses, key = { "${it.medicine.id}_${it.doseTime}" }) { doseItem ->
                    MedicationCard(
                        doseItem = doseItem,
                        onToggleTaken = {
                            repository.toggleMedicineTaken(doseItem.medicine.id, selectedDay.date, doseItem.doseTime)
                        },
                        onEdit = { onNavigateToEdit(doseItem.medicine.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MedicationCard(
    doseItem: ScheduledDoseItem,
    onToggleTaken: () -> Unit,
    onEdit: () -> Unit
) {
    val medicine = doseItem.medicine
    val isTaken = doseItem.isTaken
    val doseTime = doseItem.doseTime

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        color = if (isTaken) SurfaceCard.copy(alpha = 0.7f) else SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
        shadowElevation = if (isTaken) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Form factor icon badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (!isTaken) PrimaryRed.copy(alpha = 0.12f) else SurfaceBone),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (medicine.type) {
                        MedicineType.CAPSULE -> Icons.Default.Vaccines
                        MedicineType.DROP -> Icons.Default.Opacity
                        MedicineType.SYRUP -> Icons.Default.WaterDrop
                        MedicineType.INJECTION -> Icons.Default.Vaccines
                        else -> Icons.Default.Medication
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = medicine.type.label,
                        tint = if (!isTaken) PrimaryRed else Ink,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Details column
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = medicine.name,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (medicine.isLowStock) StatusWarningBg else SurfaceBone)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${medicine.stockCount} left",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (medicine.isLowStock) StatusWarning else Charcoal
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Time chip
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryRed.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(11.dp))
                                Text(
                                    text = doseTime,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryRed
                                )
                            }
                        }

                        Text(
                            text = "• ${medicine.dose}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Charcoal,
                            maxLines = 1
                        )
                    }

                    // Schedule & Food Timing Badge
                    val foodOrCycleLabel = when {
                        medicine.name.contains("BORTERO", ignoreCase = true) -> "Day 8, 15, 22 Specific Dates"
                        medicine.name.contains("AURADEX", ignoreCase = true) -> "Day 8, 15, 22 (Once/Week) • After Food"
                        medicine.name.contains("ENDOXAN", ignoreCase = true) -> "Day 8 to 22 (15 Days) • After Food"
                        medicine.name.contains("SEPTRAN", ignoreCase = true) -> "Mon, Wed, Fri Only • After Food"
                        medicine.name.contains("PANTOP", ignoreCase = true) -> "Empty Stomach • Before Food"
                        medicine.name.contains("SUCRAL", ignoreCase = true) -> "Before Food"
                        medicine.dose.contains("Before Food", ignoreCase = true) -> "Before Food"
                        medicine.dose.contains("After Food", ignoreCase = true) -> "After Food"
                        else -> medicine.frequency.displayName
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceBone,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Hairline)
                    ) {
                        Text(
                            text = foodOrCycleLabel,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BodyText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Action: Take Button / Completed Badge
            if (isTaken) {
                Surface(
                    onClick = onToggleTaken,
                    shape = RoundedCornerShape(9999.dp),
                    color = BadgeSuccessBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = BadgeSuccess,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Taken",
                            color = BadgeSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onToggleTaken,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Take",
                        modifier = Modifier.size(15.dp),
                        tint = OnPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Take",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimary
                    )
                }
            }
        }
    }
}
