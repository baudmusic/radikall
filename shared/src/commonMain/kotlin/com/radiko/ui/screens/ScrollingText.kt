package com.radiko.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .basicMarquee(
                iterations = Int.MAX_VALUE,
                animationMode = MarqueeAnimationMode.Immediately,
                initialDelayMillis = 2000,
                repeatDelayMillis = 1500,
                spacing = MarqueeSpacing(32.dp),
                velocity = 22.dp,
            ),
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
    )
}
