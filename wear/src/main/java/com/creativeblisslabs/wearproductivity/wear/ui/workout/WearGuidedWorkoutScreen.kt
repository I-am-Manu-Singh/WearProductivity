package com.creativeblisslabs.wearproductivity.wear.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.creativeblisslabs.wearproductivity.wear.sensors.RepDetectionEngine
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository
import kotlinx.coroutines.delay

@Composable
fun WearGuidedWorkoutScreen(
    repository: ExerciseRepository,
    routineIds: List<String>,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val repEngine = remember { RepDetectionEngine(context) }
    val hardwareReps by repEngine.repCount.collectAsState()
    
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var showInstructions by remember { mutableStateOf(false) }

    val activeExercise = remember(currentExerciseIndex, routineIds) {
        if (currentExerciseIndex < routineIds.size) {
            repository.allExercises.find { it.id == routineIds[currentExerciseIndex] }
        } else null
    }

    LaunchedEffect(activeExercise) {
        repEngine.startTracking()
        while (activeExercise != null && activeExercise.images.isNotEmpty()) {
            delay(800L)
            currentImageIndex = (currentImageIndex + 1) % activeExercise.images.size
        }
    }

    DisposableEffect(Unit) {
        onDispose { repEngine.stopTracking() }
    }

    if (activeExercise == null) {
        onComplete()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Exercise Form Animation
        if (activeExercise.images.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/repdb/images/classic/${activeExercise.images[currentImageIndex]}")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .padding(32.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Top Overlay: Exercise Name
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = activeExercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${currentExerciseIndex + 1} / ${routineIds.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Center Overlay: Rep Counter
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                .padding(8.dp)
        ) {
            Text(
                text = "$hardwareReps",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showInstructions = !showInstructions },
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Instructions")
            }

            Button(
                onClick = {
                    repEngine.reset()
                    currentExerciseIndex += 1
                },
                modifier = Modifier.size(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Complete Set")
            }

            Button(
                onClick = { currentExerciseIndex += 1 },
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Skip")
            }
        }

        // Instructions Overlay
        AnimatedVisibility(
            visible = showInstructions,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("INSTRUCTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    activeExercise.instructions.forEach { instr ->
                        Text("• $instr", style = MaterialTheme.typography.bodySmall, color = Color.White, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showInstructions = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
