package com.creativeblisslabs.wearproductivity.wear.ui.workout

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.creativeblisslabs.wearproductivity.wear.sensors.RepDetectionEngine
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseDef
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository
import kotlinx.coroutines.delay

private const val EXERCISE_IMAGE_TAG = "ExerciseImages"

private enum class WorkoutOverlay {
    NONE,
    INSTRUCTIONS,
    TIPS
}

private fun resolveExerciseImageFrames(
    repository: ExerciseRepository,
    exercise: ExerciseDef
): List<String> {
    val animationFrames = repository.getExerciseAnimationFrames(exercise)

    return if (animationFrames != null) {
        listOf(animationFrames.first, animationFrames.second)
            .distinct()
            .filter(String::isNotBlank)
    } else {
        listOfNotNull(repository.getExerciseImagePath(exercise))
            .filter(String::isNotBlank)
    }
}

@Composable
fun WearGuidedWorkoutScreen(
    repository: ExerciseRepository,
    routineIds: List<String>,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val repEngine = remember(context) {
        RepDetectionEngine(context)
    }
    val hardwareReps by repEngine.repCount.collectAsState()

    // Reset the position when the selected routine changes.
    val routineKey = remember(routineIds) {
        routineIds.joinToString("|")
    }

    var currentExerciseIndex by rememberSaveable(routineKey) {
        mutableIntStateOf(0)
    }
    var currentImageIndex by remember {
        mutableIntStateOf(0)
    }
    var overlay by remember {
        mutableStateOf(WorkoutOverlay.NONE)
    }
    var completionSent by remember(routineKey) {
        mutableStateOf(false)
    }

    val exerciseMap = remember(repository.allExercises) {
        repository.allExercises.associateBy { it.id }
    }

    // Resolve invalid IDs rather than crashing or rendering a blank screen.
    val validRoutine = remember(routineIds, exerciseMap) {
        routineIds.mapNotNull { id -> exerciseMap[id] }
    }

    val activeExercise = validRoutine.getOrNull(currentExerciseIndex)

    val imageFrames = remember(repository, activeExercise) {
        activeExercise?.let {
            resolveExerciseImageFrames(repository, it)
        }.orEmpty()
    }

    val navigationBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    DisposableEffect(repEngine) {
        onDispose {
            repEngine.stopTracking()
        }
    }

    // Ensure completion is emitted only once, including empty/invalid routines.
    LaunchedEffect(routineKey, currentExerciseIndex, validRoutine.size) {
        if (
            !completionSent &&
            (validRoutine.isEmpty() || currentExerciseIndex >= validRoutine.size)
        ) {
            completionSent = true
            repEngine.stopTracking()
            onComplete()
        }
    }

    // Restart rep tracking and the animation loop for each exercise.
    LaunchedEffect(activeExercise?.id, imageFrames) {
        currentImageIndex = 0
        repEngine.stopTracking()

        if (activeExercise != null) {
            repEngine.reset()
            repEngine.startTracking()
        }

        while (imageFrames.size > 1) {
            delay(1_100L)
            currentImageIndex = (currentImageIndex + 1) % imageFrames.size
        }
    }

    if (activeExercise == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(30.dp)
                )

                Text(
                    text = if (routineIds.isEmpty()) {
                        "No exercises"
                    } else {
                        "No valid exercises"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Return to your workout plan and select exercises.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("BACK")
                }
            }
        }
        return
    }

    val exercise = activeExercise
    val image = imageFrames.getOrNull(currentImageIndex)
    val isFirstExercise = currentExerciseIndex == 0
    val isLastExercise = currentExerciseIndex >= validRoutine.lastIndex

    fun moveToExercise(index: Int) {
        if (index !in validRoutine.indices) return

        overlay = WorkoutOverlay.NONE
        currentExerciseIndex = index
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen image layer. The overlays use gradients so the image
        // remains visible while text stays readable in both app themes.
        if (image != null) {
            AsyncImage(
                model = remember(image, context) {
                    ImageRequest.Builder(context)
                        .data("file:///android_asset/$image")
                        .crossfade(true)
                        .listener(
                            onError = { _, result ->
                                Log.e(
                                    EXERCISE_IMAGE_TAG,
                                    "Failed to load exercise image: $image",
                                    result.throwable
                                )
                            }
                        )
                        .build()
                },
                contentDescription = "${exercise.name} demonstration",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Demonstration unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Top gradient header.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.94f),
                            Color.Black.copy(alpha = 0.68f),
                            Color.Transparent
                        )
                    )
                )
                .padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "EXERCISE ${currentExerciseIndex + 1} OF ${validRoutine.size}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val muscles = exercise.primaryMuscles
                .take(2)
                .joinToString { it.replace('_', ' ') }
                .ifBlank { "Full body" }

            Text(
                text = muscles.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.86f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Rep counter badge.
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.primaryContainer.copy(alpha = 0.97f))
                .padding(horizontal = 9.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REPS",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = hardwareReps.coerceAtLeast(0).toString(),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onPrimaryContainer,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }

        // Frame position indicator for animated exercise illustrations.
        if (imageFrames.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                imageFrames.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentImageIndex) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentImageIndex) {
                                    scheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.65f)
                                }
                            )
                    )
                }
            }
        }

        // Bottom controls stay readable and theme-aware.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 30.dp,
                    bottom = maxOf(8.dp, navigationBottom)
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { overlay = WorkoutOverlay.INSTRUCTIONS },
                    modifier = Modifier.size(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.surfaceContainerHigh,
                        contentColor = scheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Instructions"
                    )
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        repEngine.reset()
                    },
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Reset repetitions"
                    )
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { overlay = WorkoutOverlay.TIPS },
                    modifier = Modifier.size(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.surfaceContainerHigh,
                        contentColor = scheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Form tips"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (!isFirstExercise) {
                            moveToExercise(currentExerciseIndex - 1)
                        }
                    },
                    enabled = !isFirstExercise,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer,
                        disabledContainerColor = scheme.surfaceContainerLow,
                        disabledContentColor = scheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous exercise"
                    )
                }

                Button(
                    onClick = {
                        if (isLastExercise) {
                            repEngine.stopTracking()
                            if (!completionSent) {
                                completionSent = true
                                onComplete()
                            }
                        } else {
                            moveToExercise(currentExerciseIndex + 1)
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isLastExercise) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.ArrowForward
                        },
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isLastExercise) "FINISH" else "NEXT")
                }
            }
        }

        // Instructions and tips use a fully opaque themed panel to avoid
        // low contrast over exercise images.
        AnimatedVisibility(
            visible = overlay != WorkoutOverlay.NONE,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(120)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background.copy(alpha = 0.98f))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (overlay == WorkoutOverlay.INSTRUCTIONS) {
                            Icons.Default.Info
                        } else {
                            Icons.Default.Lightbulb
                        },
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(28.dp)
                    )

                    Text(
                        text = if (overlay == WorkoutOverlay.INSTRUCTIONS) {
                            "HOW TO"
                        } else {
                            "FORM TIPS"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    val lines = if (overlay == WorkoutOverlay.INSTRUCTIONS) {
                        exercise.instructions
                    } else {
                        exercise.tips
                    }

                    if (lines.isEmpty()) {
                        Text(
                            text = "No details available for this exercise.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        lines.take(5).forEachIndexed { index, line ->
                            Text(
                                text = "${index + 1}. $line",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (!exercise.description.isNullOrBlank()) {
                        Text(
                            text = exercise.description.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Button(
                        onClick = { overlay = WorkoutOverlay.NONE },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary
                        )
                    ) {
                        Text("CLOSE")
                    }
                }
            }
        }
    }
}