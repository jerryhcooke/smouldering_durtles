package com.smouldering_durtles.wk.ui

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.smouldering_durtles.wk.Constants
import com.smouldering_durtles.wk.util.TextUtil

/**
 * The support/feedback screen, ported from the legacy SupportActivity: a single
 * scrollable document with the same text and clickable links.
 *
 * @param onBack invoked to leave this screen
 * @param modifier the modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support / feedback") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            factory = { context ->
                TextView(context).apply {
                    text = TextUtil.renderHtml(Constants.SUPPORT_DOCUMENT)
                    movementMethod = LinkMovementMethod.getInstance()
                }
            },
        )
    }
}
