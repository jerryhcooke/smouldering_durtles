package com.smouldering_durtles.wk.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * A resizable widget displaying the current count of available lessons and reviews.
 */
class LessonsAndReviewsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = WkApplication.getDatabase()
        val maxLevel = withContext(Dispatchers.IO) {
            database.propertiesDao().userMaxLevelGranted
        }
        val now = System.currentTimeMillis()
        val alertContext = withContext(Dispatchers.IO) {
            database.subjectAggregatesDao().getAlertContext(maxLevel, now)
        }
        val lessonCount = alertContext?.numLessons ?: 0
        val reviewCount = alertContext?.numReviews ?: 0

        provideContent {
            WidgetContent(lessonCount, reviewCount, context)
        }
    }

    @Composable
    private fun WidgetContent(lessons: Int, reviews: Int, context: Context) {
        GlanceTheme {
            val size = LocalSize.current
            val width = size.width.value
            val height = size.height.value
            
            // Adjust scaling factors based on size to make text scalable
            val isHorizontal = width > height * 1.5f
            
            // For text fitting, we want to scale based on the available space for each cell
            val cellWidth = if (isHorizontal) width / 2f else width
            val cellHeight = if (isHorizontal) height else height / 2f
            
            val countFontSize = (min(cellWidth, cellHeight) * 0.45f).coerceIn(12f, 48f).sp
            val labelFontSize = (min(cellWidth, cellHeight) * 0.25f).coerceIn(8f, 18f).sp

            if (isHorizontal) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            .background(GlanceTheme.colors.primaryContainer).padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = lessons.toString(),
                            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = countFontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                        Text(
                            text = "Lessons",
                            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = labelFontSize, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
                        )
                    }
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            .background(GlanceTheme.colors.tertiaryContainer).padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = reviews.toString(),
                            style = TextStyle(color = GlanceTheme.colors.onTertiaryContainer, fontSize = countFontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                        Text(
                            text = "Reviews",
                            style = TextStyle(color = GlanceTheme.colors.onTertiaryContainer, fontSize = labelFontSize, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
                        )
                    }
                }
            } else {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                            .background(GlanceTheme.colors.primaryContainer).padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = lessons.toString(),
                            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = countFontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                        Text(
                            text = "Lessons",
                            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = labelFontSize, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
                        )
                    }
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                            .background(GlanceTheme.colors.tertiaryContainer).padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = reviews.toString(),
                            style = TextStyle(color = GlanceTheme.colors.onTertiaryContainer, fontSize = countFontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                        Text(
                            text = "Reviews",
                            style = TextStyle(color = GlanceTheme.colors.onTertiaryContainer, fontSize = labelFontSize, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Receiver for the Lessons and Reviews combined widget.
 */
class LessonsAndReviewsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LessonsAndReviewsWidget()
}
