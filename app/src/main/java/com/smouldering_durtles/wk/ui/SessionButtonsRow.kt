package com.smouldering_durtles.wk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A row of buttons to start lessons, start reviews, or resume an active session.
 *
 * Renders nothing when there is nothing to show: no active session and no available lessons or reviews.
 */
@Composable
fun SessionButtonsRow(
    availableLessons: Int,
    availableReviews: Int,
    sessionActive: Boolean,
    onStartLessons: () -> Unit,
    onStartReviews: () -> Unit,
    onResumeSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sessionActive) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onResumeSession,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Resume session")
            }
        }
    }
    else if (availableLessons > 0 || availableReviews > 0) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (availableLessons > 0) {
                Button(
                    onClick = onStartLessons,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Start lessons")
                }
            }
            if (availableReviews > 0) {
                Button(
                    onClick = onStartReviews,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Start reviews")
                }
            }
        }
    }
}
