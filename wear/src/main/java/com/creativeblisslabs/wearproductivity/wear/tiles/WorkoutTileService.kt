package com.creativeblisslabs.wearproductivity.wear.tiles

import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.creativeblisslabs.wearproductivity.wear.WearMainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class WorkoutTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val clickAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(WearMainActivity::class.java.name)
                    .build()
            )
            .build()

        val chipModifier = ModifiersBuilders.Modifiers.Builder()
            .setClickable(
                ModifiersBuilders.Clickable.Builder()
                    .setOnClick(clickAction)
                    .setId("open_workout_studio")
                    .build()
            )
            .build()

        val header = Text.Builder(this, "WORKOUT STUDIO")
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(ColorBuilders.argb(0xFFFF8A93.toInt()))
            .build()

        val mainStat = Text.Builder(this, "Strength / Cardio")
            .setTypography(Typography.TYPOGRAPHY_TITLE3)
            .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
            .build()

        val subStat = Text.Builder(this, "Tap to start workout session")
            .setTypography(Typography.TYPOGRAPHY_BODY2)
            .setColor(ColorBuilders.argb(0xFFB0B0B0.toInt()))
            .build()

        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(header)
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())
            .addContent(mainStat)
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(2f)).build())
            .addContent(subStat)
            .setModifiers(chipModifier)
            .build()

        val layout = LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .addContent(column)
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1.0")
            .setFreshnessIntervalMillis(5 * 60 * 1000L)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(layout)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    @Deprecated("Deprecated in Java")
    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1.0")
                .build()
        )
    }
}
