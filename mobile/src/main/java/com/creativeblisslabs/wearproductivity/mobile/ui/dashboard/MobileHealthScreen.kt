package com.creativeblisslabs.wearproductivity.mobile.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository

@Composable
fun MobileHealthScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("HEALTH & NUTRITION", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daily Macro Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    val caloriesProgress = if (state.nutritionTarget.targetCalories > 0) 
                        state.todayCalories.toFloat() / state.nutritionTarget.targetCalories.toFloat() else 0f
                    Text("Calories: ${state.todayCalories} / ${state.nutritionTarget.targetCalories} kcal", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { caloriesProgress }, modifier = Modifier.fillMaxWidth())

                    val proteinProgress = if (state.nutritionTarget.targetProteinGrams > 0) 
                        state.todayProtein.toFloat() / state.nutritionTarget.targetProteinGrams.toFloat() else 0f
                    Text("Protein: ${state.todayProtein.toInt()}g / ${state.nutritionTarget.targetProteinGrams.toInt()}g", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { proteinProgress }, modifier = Modifier.fillMaxWidth())

                    val carbsProgress = if (state.nutritionTarget.targetCarbsGrams > 0) 
                        state.todayCarbs.toFloat() / state.nutritionTarget.targetCarbsGrams.toFloat() else 0f
                    Text("Carbs: ${state.todayCarbs.toInt()}g / ${state.nutritionTarget.targetCarbsGrams.toInt()}g", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { carbsProgress }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fasting Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status: ${if (state.fast.active) "Active" else "Not Fasting"}", color = MaterialTheme.colorScheme.primary)
                    Text("Plan: ${state.fast.plan}")
                    
                    val metabolicStage = when {
                        !state.fast.active -> "Ready to Fast"
                        state.fast.progress >= 0.75f -> "Autophagy & Cellular Renewal"
                        state.fast.progress >= 0.5f -> "Ketosis & Fat Oxidation"
                        state.fast.progress >= 0.25f -> "Glycogen Depletion"
                        else -> "Anabolic / Digestion Stage"
                    }
                    Text("Metabolic Stage: $metabolicStage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.fast.progress }, 
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
