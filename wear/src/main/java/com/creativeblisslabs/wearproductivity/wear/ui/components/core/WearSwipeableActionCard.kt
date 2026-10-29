
package com.creativeblisslabs.wearproductivity.wear.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import kotlinx.coroutines.launch
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberRevealState

/**
 * Reusable Wear OS Material 3 card with swipe-to-reveal Edit and Delete actions.
 *
 * The caller provides the primary card content. Swipe reveals the secondary
 * edit action and primary delete action.
 */
@Composable
fun WearSwipeableActionCard(
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    content: @Composable () -> Unit
) {
    val revealState = rememberRevealState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    SwipeToReveal(
        revealState = revealState,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        onSwipePrimaryAction = {
            kotlinx.coroutines.CoroutineScope(scope.coroutineContext).launch { revealState.animateTo(androidx.wear.compose.material3.RevealValue.Covered) }
            onDeleteClick()
        },
        primaryAction = {
            IconButton(
                onClick = {
                    kotlinx.coroutines.CoroutineScope(scope.coroutineContext).launch { revealState.animateTo(androidx.wear.compose.material3.RevealValue.Covered) }
                    onDeleteClick()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete"
                )
            }
        },
        secondaryAction = {
            IconButton(
                onClick = {
                    kotlinx.coroutines.CoroutineScope(scope.coroutineContext).launch { revealState.animateTo(androidx.wear.compose.material3.RevealValue.Covered) }
                    onEditClick()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit"
                )
            }
        }
    ) {
        Card(
            onClick = onCardClick,
            modifier = Modifier.fillMaxWidth(),
            transformation = transformation,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                content()
            }
        }
    }
}
