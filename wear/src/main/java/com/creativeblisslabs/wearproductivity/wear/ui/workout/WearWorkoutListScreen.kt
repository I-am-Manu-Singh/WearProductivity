package com.creativeblisslabs.wearproductivity.wear.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveHeader
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository

@Composable
fun WearWorkoutListScreen(
    repository: ExerciseRepository,
    routineIds: List<String>,
    onStartWorkout: (List<String>) -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    
    val exercises = remember(routineIds) {
        routineIds.mapNotNull { id -> repository.allExercises.find { it.id == id } }
    }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding(), start = 16.dp, end = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "WORKOUT AGENDA",
                    badgeText = "${exercises.size} EXERCISES"
                )
            }

            items(exercises.size) { idx ->
                val ex = exercises[idx]
                Card(
                    onClick = { /* TODO: Open Detail dialog if needed */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (ex.images.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data("file:///android_asset/repdb/images/classic/${ex.images[0]}")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = ex.name,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ex.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${ex.mechanic?.uppercase() ?: "COMPOUND"} · ${ex.primaryMuscles.firstOrNull()?.uppercase() ?: "FULL BODY"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onStartWorkout(routineIds) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BEGIN WORKOUT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
