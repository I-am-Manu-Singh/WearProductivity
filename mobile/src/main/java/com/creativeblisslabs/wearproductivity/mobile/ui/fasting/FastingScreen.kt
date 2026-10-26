package com.creativeblisslabs.wearproductivity.mobile.ui.fasting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import kotlinx.coroutines.launch

// Design Tokens
private val FastingGradientStart = Color(0xFFB3C5FF)
private val FastingGradientEnd = Color(0xFF80F8CD)
private val KetosisCoral = Color(0xFFF17F5B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingScreen(
    state: LifeState,
    repository: LifeRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val fast = state.fast

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fasting") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                FastingDial(
                    progress = fast.progress,
                    label = fast.plan,
                    isActive = fast.active
                )
            }

            item {
                KetosisBadge()
            }

            item {
                TelemetryGrid(
                    startedAt = fast.startedAtLabel.ifBlank { "--:--" },
                    phase = if (fast.progress > 0.8f) "Ketosis" else "Anabolic",
                    avgHr = "${state.wellness.currentHeartRateBpm.toInt()} BPM"
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                repository.toggleFast()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (fast.active) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (fast.active) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            if (fast.active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (fast.active) "End Fast" else "Start Fast")
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.extendFast(60)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = fast.active
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("+1 Hour")
                    }
                }
            }
        }
    }
}

@Composable
fun FastingDial(
    progress: Float,
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background track
            drawArc(
                color = Color.LightGray.copy(alpha = 0.2f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            // Progress track
            if (isActive) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to FastingGradientStart,
                        progress to FastingGradientEnd,
                        progress to Color.Transparent
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * progress,
                    useCenter = false,
                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isActive) "${(progress * 100).toInt()}%" else "Off",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun KetosisBadge() {
    Surface(
        color = KetosisCoral.copy(alpha = 0.1f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KetosisCoral.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(KetosisCoral, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Ketosis Zone",
                style = MaterialTheme.typography.labelLarge,
                color = KetosisCoral,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TelemetryGrid(
    startedAt: String,
    phase: String,
    avgHr: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TelemetryItem("Started", startedAt, Modifier.weight(1f))
        TelemetryItem("Phase", phase, Modifier.weight(1f))
        TelemetryItem("Avg HR", avgHr, Modifier.weight(1f))
    }
}

@Composable
fun TelemetryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
