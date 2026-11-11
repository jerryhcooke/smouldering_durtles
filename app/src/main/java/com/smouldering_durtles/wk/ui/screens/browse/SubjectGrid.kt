package com.smouldering_durtles.wk.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.util.ThemeUtil

/**
 * A wrapping grid of subject cards, ported from the legacy SubjectGridView:
 * the number of columns depends on the available width (about 90dp per
 * column); radicals and kanji take one column, vocabulary takes three (or the
 * full width when there are fewer than six columns).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubjectGrid(
    subjects: List<Subject>,
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    showMeaning: Boolean,
    showReading: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val widthDp = this@BoxWithConstraints.constraints.maxWidth / density.density
        val spans = ((widthDp + 10) / 90).toInt().coerceAtLeast(1)
        val gap = 4.dp
        val gapPx = with(density) { gap.toPx() }
        val cellWidthPx = (this@BoxWithConstraints.constraints.maxWidth - (spans - 1) * gapPx) / spans

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (subject in subjects) {
                val numSpans = when {
                    subject.getType().isRadical() || subject.getType().isKanji() -> 1
                    else -> if (spans >= 6) 3 else spans
                }
                SubjectCard(
                    subject = subject,
                    showMeaning = showMeaning,
                    showReading = showReading,
                    onClick = { onSubjectClick(subject.getId(), subjects.map { it.getId() }) },
                    modifier = Modifier.width(with(density) { (cellWidthPx * numSpans + gapPx * (numSpans - 1)).toDp() }),
                )
            }
        }
    }
}

/**
 * A subject card for the grids, ported from the legacy SubjectCardBinder with
 * the NORMAL card layouts: character (or radical image), meaning, reading and
 * the SRS progress line on the type's background color.
 */
@Composable
private fun SubjectCard(
    subject: Subject,
    showMeaning: Boolean,
    showReading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stage = subject.getSrsStage()
    Column(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color(subject.getButtonBackgroundColor()))
            .clickable(onClick = onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (subject.needsTitleImage()) {
            androidx.compose.foundation.Image(
                painter = painterResource(subject.getTitleImageId()),
                contentDescription = subject.getSlug() ?: "",
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color(subject.getTextColor())),
                modifier = Modifier.padding(top = 4.dp).height(28.dp),
            )
        }
        else {
            Text(
                text = subject.getCharacters() ?: subject.getSlug() ?: "",
                fontSize = 24.sp,
                color = Color(subject.getTextColor()),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (subject.hasMeanings() && showMeaning) {
            CardText(text = subject.getOneMeaning(), small = true)
        }
        if (subject.hasReadings() && showReading) {
            CardText(text = subject.getOneReading(), small = false)
        }
        else if (subject.getType().isKanaVocabulary()) {
            CardText(text = subject.getCharacters() ?: "", small = false)
        }
        if (!stage.isLocked()) {
            val stageName = if (subject.getType().isVocabulary() || subject.getType().isKanaVocabulary()) {
                stage.getName()
            }
            else {
                stage.getShortName()
            }
            val progressText = if (subject.getAvailableAt() == 0L) {
                stageName
            }
            else {
                "$stageName - ${subject.getShortNextReviewWaitTime()}"
            }
            CardText(text = progressText, small = true)
        }
    }
}

@Composable
private fun CardText(text: String, small: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = if (small) 12.sp else 14.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = Color(ThemeUtil.getColor(R.attr.colorPrimaryLight)),
        style = MaterialTheme.typography.bodyMedium.copy(
            shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 3f),
        ),
    )
}
