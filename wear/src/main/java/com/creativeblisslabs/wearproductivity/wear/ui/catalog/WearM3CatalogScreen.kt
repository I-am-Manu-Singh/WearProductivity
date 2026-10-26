package com.creativeblisslabs.wearproductivity.wear.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.DurationPicker
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearActionButtonGroup
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearAnchoredEdgeButton
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveCheckboxRow
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveHeader
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveHorizontalSlider
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressivePrevNextRow
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveSwitchRow
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearHeatMapCard
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearMetricCard
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearStatusBadge
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearSwipeableActionCard
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearTransformingCard
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.delay

private enum class CatalogOverlayState {
    NONE,
    HOURS_MINUTES_PICKER,
    DATE_PICKER,
    TIME_PICKER,
    ALERT_DIALOG,
    CONFIRMATION_SUCCESS,
    CONFIRMATION_GENERIC,
    CONFIRMATION_FAILURE,
    CONFIRMATION_AUTH,
    OPEN_ON_PHONE
}


/**
 * In-Viewport Constrained Confirmation / OpenOnPhone Overlay so it never escapes
 * the 416.dp circular watch bezel on mobile phone previews.
 */
