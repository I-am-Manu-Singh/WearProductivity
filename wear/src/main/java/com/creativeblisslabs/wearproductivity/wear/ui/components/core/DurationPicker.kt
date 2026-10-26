package com.creativeblisslabs.wearproductivity.wear.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.rememberPickerState
// Make sure to import your WearAnchoredEdgeButton if it resides in a separate package:
// import com.creativeblisslabs.wearproductivity.wear.ui.components.WearAnchoredEdgeButton

/**
 * Fully Native Material 3 Wear OS Duration Picker with a visible bottom WearAnchoredEdgeButton.
 */
@Composable
fun DurationPicker(
    title: String,
    initialMinutes: Int = 25,
    onConfirm: (totalMinutes: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutePickerState = rememberPickerState(
        initialNumberOfOptions = 120,
        initiallySelectedIndex = (initialMinutes - 1).coerceIn(0, 119)
    )

    val contentDescription by remember {
        derivedStateOf { "${minutePickerState.selectedOptionIndex + 1} minutes" }
    }

    // Use a standard Box layout since this is a static configuration screen
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top Time Text indicator
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            TimeText()
        }

        // Center content (Title + Picker) with bottom padding so it doesn't overlap the edge button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Native System Label Header
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Native Picker + Unit Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Picker(
                    state = minutePickerState,
                    contentDescription = { contentDescription },
                    onSelected = {},
                    userScrollEnabled = true,
                    modifier = Modifier
                        .width(68.dp)
                        .height(84.dp)
                ) { optionIndex ->
                    val minuteValue = optionIndex + 1
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(java.util.Locale.ROOT, "%02d", minuteValue),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = "m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
            }
        }

        // Anchored Edge Button at the bottom of the Box screen
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            WearAnchoredEdgeButton(
                onClick = {
                    val chosenMinutes = minutePickerState.selectedOptionIndex + 1
                    onConfirm(chosenMinutes)
                },
                label = "Save",
                icon = Icons.Rounded.Check
            )
        }
    }
}