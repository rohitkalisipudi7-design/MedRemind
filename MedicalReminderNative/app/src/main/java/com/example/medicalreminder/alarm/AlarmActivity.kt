package com.example.medicalreminder.alarm

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.data.ReminderRepository
import com.example.medicalreminder.model.MedicineType
import com.example.medicalreminder.receiver.ReminderAlarmReceiver
import com.example.medicalreminder.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeAndUnlockScreen()
        startAlarmSoundAndVibration()

        val medicineId = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_ID) ?: ""
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Atorvastatin Calcium"
        val medicineDose = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_DOSE) ?: "1 Tablet • 40mg"
        val medicineType = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_TYPE) ?: "TABLET"
        val medicineTime = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_TIME) ?: ""

        val repository = ReminderRepository(applicationContext)

        setContent {
            AlarmFullScreenUi(
                medicineName = medicineName,
                medicineDose = medicineDose,
                medicineType = medicineType,
                scheduledTime = medicineTime,
                onTakeMedicine = {
                    stopAlarm()
                    if (medicineId.isNotEmpty()) {
                        repository.toggleMedicineTaken(medicineId, Date(), medicineTime.ifEmpty { null })
                    }
                    finish()
                },
                onSnooze = {
                    stopAlarm()
                    snoozeAlarm(medicineId, medicineName, medicineDose, medicineType)
                    finish()
                },
                onDismiss = {
                    stopAlarm()
                    finish()
                }
            )
        }
    }

    private fun wakeAndUnlockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startAlarmSoundAndVibration() {
        try {
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Start repeating vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 600, 400, 600, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            ringtone?.stop()
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun snoozeAlarm(medicineId: String, name: String, dose: String, type: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeDurationMs = 15 * 60 * 1000L // 15 minutes
        val snoozeTime = System.currentTimeMillis() + snoozeDurationMs

        val intent = Intent(this, ReminderAlarmReceiver::class.java).apply {
            action = "com.example.medicalreminder.ACTION_ALARM"
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, name)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_DOSE, dose)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_TYPE, type)
            putExtra("is_snooze", true)
        }

        val showIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, name)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_DOSE, dose)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_TYPE, type)
        }

        val showPendingIntent = PendingIntent.getActivity(
            this,
            (medicineId.hashCode() xor 8888),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            (medicineId.hashCode() xor 9999),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(snoozeTime, showPendingIntent)
        try {
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                }
            } catch (e2: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            }
        }

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val ringTimeStr = timeFormat.format(Date(snoozeTime))
        android.widget.Toast.makeText(this, "Alarm snoozed for 15 min ($ringTimeStr)", android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
fun AlarmFullScreenUi(
    medicineName: String,
    medicineDose: String,
    medicineType: String,
    scheduledTime: String = "",
    onTakeMedicine: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    val timeStr = remember { SimpleDateFormat("HH:mm", Locale.US).format(Date()) }
    val dateStr = remember { SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Active Badge & Lock Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(PrimaryRed)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Text(
                        text = "ALARM ACTIVE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Stone, modifier = Modifier.size(15.dp))
                    Text("Lock-Screen Wake", fontSize = 11.5.sp, color = Stone)
                }
            }

            // Time & Alarm Context
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = HeroGlow, modifier = Modifier.size(18.dp))
                    Text(
                        text = "SCHEDULED REGIMEN DUE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeroGlow,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = timeStr,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp,
                    lineHeight = 56.sp
                )

                Text(
                    text = "$dateStr • Pharmacopeia Alert",
                    fontSize = 13.sp,
                    color = Stone
                )
            }

            // Pulsating Center Pill Graphic
            Box(
                modifier = Modifier
                    .size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer expanding animated ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(PrimaryRed.copy(alpha = ringAlpha))
                )

                // Inner static glow ring
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(CircleShape)
                        .background(HeroGlow.copy(alpha = 0.25f))
                )

                // Center Black Disc
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(PrimaryRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Highlight Box: Medication Specs
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(HeroGlow.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("Afternoon Dispensation", color = HeroPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = medicineName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "$medicineDose • With Food",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Stone
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Stone, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take immediately with a full glass of water.", fontSize = 12.sp, color = OnDark)
                        }
                    }
                }
            }

            // Action Buttons (Take Now, Snooze, Dismiss)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSnooze,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Snooze 15m", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }

                    Button(
                        onClick = onTakeMedicine,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Dose Now", color = OnPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Dismiss Wake Screen", color = Stone, fontSize = 12.sp)
                }
            }
        }
    }
}
