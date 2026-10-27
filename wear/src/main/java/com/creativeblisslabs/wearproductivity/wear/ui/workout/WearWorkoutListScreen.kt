package com.creativeblisslabs.wearproductivity.wear.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
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
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseDef
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository

@Composable
fun WearWorkoutListScreen(
    repository: ExerciseRepository,
    routineIds: List<String>,
    onStartWorkout: (List<String>) -> Unit
) {
    val state = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf<String?>(null) }
    val exercises = remember(routineIds, repository.allExercises) {
        val map = repository.allExercises.associateBy { it.id }

        android.util.Log.d(
            "WorkoutDebug",
            "Received routine IDs (${routineIds.size}): $routineIds"
        )

        val resolved = routineIds.mapNotNull(map::get)

        android.util.Log.d(
            "WorkoutDebug",
            "Resolved exercises (${resolved.size}): ${resolved.map { it.id to it.name }}"
        )

        resolved
    }

    ScreenScaffold(scrollState = state, timeText = { TimeText() }) { padding ->
        TransformingLazyColumn(
            state = state,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 12.dp,
                start = 12.dp, end = 12.dp
            ),
            modifier = Modifier.fillMaxSize().background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(title = "YOUR WORKOUT", badgeText = "${exercises.size} EXERCISES")
            }
            item {
                Text(
                    "${estimateMinutes(exercises.size)} min estimated · tap a card for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(exercises.size) { index ->
                val ex = exercises[index]

                android.util.Log.d(
                    "WorkoutDebug",
                    "Composing card ${index + 1}/${exercises.size}: ${ex.name}"
                )
                val expanded = expandedId == ex.id
                val image = remember(ex.id) { repository.getExerciseImagePath(ex) }
                Card(
                    onClick = { expandedId = if (expanded) null else ex.id },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            if (image != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data("file:///android_asset/$image").crossfade(true).build(),
                                    contentDescription = "${ex.name} demonstration",
                                    modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center) { Text("FIT", style = MaterialTheme.typography.labelSmall) }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(ex.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOfNotNull(ex.primaryMuscles.firstOrNull()?.humanize(), ex.equipment?.humanize(), ex.level.humanize()).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (expanded) {
                            if (!ex.description.isNullOrBlank()) DetailLine("ABOUT", ex.description)
                            DetailLine("CATEGORY", ex.category.humanize())
                            if (!ex.force.isNullOrBlank()) DetailLine("MOVEMENT", ex.force.humanize())
                            if (!ex.mechanic.isNullOrBlank()) DetailLine("MECHANIC", ex.mechanic.humanize())
                            if (ex.primaryMuscles.isNotEmpty()) DetailLine("PRIMARY", ex.primaryMuscles.joinToString { it.humanize() })
                            if (ex.secondaryMuscles.isNotEmpty()) DetailLine("SECONDARY", ex.secondaryMuscles.joinToString { it.humanize() })
                            if (!ex.effectiveBodyPart.isNullOrBlank()) DetailLine("BODY AREA", ex.effectiveBodyPart!!.humanize())
                            if (ex.effectiveBodyweight != null) DetailLine("BODYWEIGHT", if (ex.effectiveBodyweight == true) "Yes" else "No")
                            if (ex.effectiveUnilateral != null) DetailLine("UNILATERAL", if (ex.effectiveUnilateral == true) "Yes" else "No")
                            if (ex.met != null) DetailLine("MET", "%.1f".format(ex.met))
                            if (ex.goals.isNotEmpty()) DetailLine("GOALS", ex.goals.joinToString { it.humanize() })
                            if (ex.tags.isNotEmpty()) DetailLine("TAGS", ex.tags.joinToString { it.humanize() })
                            if (ex.instructions.isNotEmpty()) {
                                Text("HOW TO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                ex.instructions.take(4).forEachIndexed { i, text ->
                                    Text("${i + 1}. $text", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (ex.tips.isNotEmpty()) {
                                Text("TIPS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                ex.tips.take(3).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                            if (ex.relations.isNotEmpty()) DetailLine("RELATED IDS", ex.relations.take(4).joinToString())
                        }
                    }
                }
            }
            item {
                Button(onClick = { onStartWorkout(routineIds) }, modifier = Modifier.fillMaxWidth()) {
                    Text("START WORKOUT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun estimateMinutes(count: Int) = (count * 3).coerceAtLeast(5)
private fun String.humanize() = replace('_', ' ').replace('-', ' ')
    .replace(Regex("\\s+"), " ").trim().replaceFirstChar { it.uppercase() }
