package com.radiko.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.radiko.platform.PlatformEnvironment
import com.radiko.station.Station
import com.radiko.station.logoUrl
import com.radiko.ui.theme.RadikoColors

@Composable
fun StationCard(
    station: Station,
    isActive: Boolean,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val supportsPointerHover = PlatformEnvironment.supportsPointerHover
    val showDefaultBlueFrame = !supportsPointerHover

    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> RadikoColors.AccentRed
            hovered -> RadikoColors.PrimaryBlue.copy(alpha = 0.9f)
            showDefaultBlueFrame -> RadikoColors.PrimaryBlue.copy(alpha = 0.72f)
            else -> Color.Transparent
        },
        label = "station_card_border",
    )
    val containerColor by animateColorAsState(
        targetValue = RadikoColors.White,
        label = "station_card_container",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color = containerColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .then(
                if (supportsPointerHover) {
                    Modifier.hoverable(interactionSource = interactionSource)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(if (emphasized) 24.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = station.logoUrl,
            contentDescription = station.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (emphasized) 86.dp else 72.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = station.id,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = if (isActive) RadikoColors.AccentRed else RadikoColors.TextSecondary,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = station.name,
            style = if (emphasized) {
                androidx.compose.material3.MaterialTheme.typography.titleMedium
            } else {
                androidx.compose.material3.MaterialTheme.typography.titleSmall
            },
            fontWeight = FontWeight.Medium,
            color = RadikoColors.DarkText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}
