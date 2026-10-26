package com.creativeblisslabs.wearproductivity.mobile.ui.components

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.creativeblisslabs.wearproductivity.mobile.R
import com.creativeblisslabs.wearproductivity.mobile.ui.theme.LocalDimens
import com.creativeblisslabs.wearproductivity.mobile.ui.util.normalize
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun TempoPicker(
    value: Int,
    label: String,
    onValueChangeDelta: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val previousValueRef = remember { intArrayOf(value) }
    val isIncreasing = remember(value) {
        val increasing = value >= previousValueRef[0]
        previousValueRef[0] = value
        increasing
    }

    var isDragged by remember { mutableStateOf(false) }
    var totalRotation by remember { mutableFloatStateOf(0f) }
    var touchPos by remember { mutableStateOf(Offset.Zero) }

    val spatialSpring = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val effectsSpring = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    val morphFactor by animateFloatAsState(
        targetValue = if (isDragged) 1f else 0f,
        animationSpec = spatialSpring,
        label = "morphFactor",
    )

    val colorFraction by animateFloatAsState(
        targetValue = if (isDragged) 0.85f else 0f,
        animationSpec = effectsSpring,
        label = "colorFraction",
    )

    val colorDefault = MaterialTheme.colorScheme.primaryContainer
    val colorDrag1 = MaterialTheme.colorScheme.tertiaryContainer
    val colorDrag2 = MaterialTheme.colorScheme.primaryContainer
    val colorDrag3 = MaterialTheme.colorScheme.secondaryContainer

    val fontWeight = (600f + (colorFraction * 300f)).toInt()
    val fontWidth = 100f + (colorFraction * 5f)

    val dynamicFontFamily = remember(fontWeight, fontWidth) {
        FontFamily(
            Font(
                resId = R.font.google_sans_flex_variable,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(fontWeight),
                    FontVariation.width(fontWidth),
                    FontVariation.Setting("ROND", 100f)
                ),
            )
        )
    }

    val morph = remember {
        Morph(
            normalize(
                MaterialShapes.Cookie12Sided,
                true,
                RectF(-1f, -1f, 1f, 1f)
            ),
            normalize(
                MaterialShapes.SoftBurst,
                true,
                RectF(-1f, -1f, 1f, 1f)
            ),
        )
    }

    val androidPath = remember { android.graphics.Path() }
    val composePath = remember { androidPath.asComposePath() }
    val matrix = remember { Matrix() }

    Box(
        modifier = modifier
            .size(dimens.tempoPickerSize)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = minOf(size.width, size.height) / 2f
                    val innerRadius = 20.dp.toPx()

                    val dist = hypot(down.position.x - cx, down.position.y - cy)

                    if (dist <= radius) {
                        isDragged = true
                        touchPos = down.position

                        var isStartedInCenter = dist <= innerRadius
                        var currAngle = if (!isStartedInCenter) calculateAngle(touchPos, cx, cy) else 0.0
                        var degreeStorage = 0f
                        var hasDragged = false

                        do {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.first()
                            touchPos = pointer.position

                            val currentDist = hypot(touchPos.x - cx, touchPos.y - cy)
                            val isOutsideCenter = currentDist > innerRadius

                            if (isOutsideCenter) {
                                val angle = calculateAngle(touchPos, cx, cy)
                                if (isStartedInCenter) {
                                    isStartedInCenter = false
                                    currAngle = angle
                                }

                                val prevAngle = currAngle
                                currAngle = angle

                                var delta = (currAngle - prevAngle).toFloat()
                                if (delta > 180f) delta -= 360f
                                else if (delta < -180f) delta += 360f

                                if (delta != 0f) {
                                    hasDragged = true
                                    totalRotation += delta
                                    degreeStorage += delta

                                    if (degreeStorage > 12f) {
                                        onValueChangeDelta(if (isRtl) -1 else 1)
                                        degreeStorage = 0f
                                    } else if (degreeStorage < -12f) {
                                        onValueChangeDelta(if (isRtl) 1 else -1)
                                        degreeStorage = 0f
                                    }
                                }
                            }
                            pointer.consume()
                        } while (event.changes.any { it.pressed })

                        isDragged = false
                        if (!hasDragged) {
                            onClick()
                        }
                    }
                }
            }
            .drawWithContent {
                androidPath.rewind()
                morph.toPath(morphFactor, androidPath)

                matrix.reset()
                matrix.setScale(size.width / 2f, size.height / 2f)
                matrix.postRotate(totalRotation)
                matrix.postTranslate(size.width / 2f, size.height / 2f)
                androidPath.transform(matrix)

                val brush = Brush.radialGradient(
                    0.0f to lerp(colorDefault, colorDrag1, colorFraction),
                    0.1f to lerp(colorDefault, colorDrag1, colorFraction),
                    0.5f to lerp(colorDefault, colorDrag2, colorFraction),
                    0.9f to lerp(colorDefault, colorDrag3, colorFraction),
                    center = touchPos,
                    radius = size.width
                )

                drawPath(
                    path = composePath,
                    brush = brush
                )

                clipPath(path = composePath) {
                    this@drawWithContent.drawContent()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val slideSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
            val fadeSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

            AnimatedContent(
                targetState = label,
                transitionSpec = {
                    val direction = if (isIncreasing) SlideDirection.Start else SlideDirection.End
                    (slideIntoContainer(
                        towards = direction,
                        animationSpec = slideSpec,
                        initialOffset = { offset -> offset / 3 }
                    ) + fadeIn(animationSpec = fadeSpec)) togetherWith
                            (slideOutOfContainer(
                                towards = direction,
                                animationSpec = slideSpec,
                                targetOffset = { offset -> offset / 3 }
                            ) + fadeOut(animationSpec = fadeSpec))
                },
                label = "labelAnim",
                modifier = Modifier.fillMaxWidth(),
            ) { targetLabel ->
                Text(
                    text = targetLabel,
                    style = dimens.tempoPickerLabelTextStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = value.toString(),
                style = dimens.tempoPickerBpmTextStyle.copy(
                    fontFamily = dynamicFontFamily,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun calculateAngle(pos: Offset, cx: Float, cy: Float): Double {
    val angleRaw = Math.toDegrees(atan2((pos.x - cx).toDouble(), (cy - pos.y).toDouble()))
    return if (angleRaw >= 0) angleRaw else 180 + (180 - abs(angleRaw))
}
