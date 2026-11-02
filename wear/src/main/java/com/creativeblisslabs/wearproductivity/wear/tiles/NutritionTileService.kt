package com.creativeblisslabs.wearproductivity.wear.tiles

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.creativeblisslabs.wearproductivity.wear.WearMainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class NutritionTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val launchIntent = Intent(this, WearMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "fasting")
        }
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
                    .setId("open_fasting_nutrition")
                    .build()
            )
            .build()

        val title = Text.Builder(this, "NUTRITION & WATER")
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(ColorBuilders.argb(0xFF79E2B0.toInt()))
            .build()

        val mainStat = Text.Builder(this, "Water: 1.5L / 2.0L")
            .setTypography(Typography.TYPOGRAPHY_TITLE3)
            .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
            .build()

        val subStat = Text.Builder(this, "Fast Target: 16:8 Window")
            .setTypography(Typography.TYPOGRAPHY_BODY2)
            .setColor(ColorBuilders.argb(0xFFB0B0B0.toInt()))
            .build()

        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(title)
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
