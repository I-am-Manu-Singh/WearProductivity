package com.creativeblisslabs.wearproductivity.wear.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState

enum class WearListMode {
    LIST,
    GRID
}

/**
 * Shared reusable Wear OS List/Grid View component that seamlessly toggles
 * between 1-column scaling TransformingLazyColumn (LIST mode) and 2-column LazyVerticalGrid (GRID mode).
 */
@Composable
fun <T> WearReusableListGridView(
    items: List<T>,
    isGridView: Boolean,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    gridItemContent: @Composable (T) -> Unit,
    listItemContent: @Composable TransformingLazyColumnItemScope.(T) -> Unit
) {
    if (isGridView) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items.size) { index ->
                    gridItemContent(items[index])
                }
            }
        }
    } else {
        val columnState = rememberTransformingLazyColumnState()
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 24.dp),
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (header != null) {
                item {
                    header()
                }
            }
            items(items.size) { index ->
                listItemContent(items[index])
            }
            if (footer != null) {
                item {
                    footer()
                }
            }
        }
    }
}
