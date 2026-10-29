package com.creativeblisslabs.wearproductivity.wear.complications

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class LifeOSComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("LifeOS").build(),
                    contentDescription = PlainComplicationText.Builder("LifeOS Complication").build()
                )
                    .setTitle(PlainComplicationText.Builder("Status").build())
                    .build()
            }
            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("LifeOS").build(),
            contentDescription = PlainComplicationText.Builder("Preview").build()
        )
            .setTitle(PlainComplicationText.Builder("Ready").build())
            .build()
    }
}
