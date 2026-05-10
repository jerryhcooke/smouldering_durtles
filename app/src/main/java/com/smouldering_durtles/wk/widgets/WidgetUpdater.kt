package com.smouldering_durtles.wk.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper to update widgets from Java.
 */
object WidgetUpdater {
    @JvmStatic
    fun updateAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LessonsWidget().updateAll(context)
                ReviewsWidget().updateAll(context)
                LessonsAndReviewsWidget().updateAll(context)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
