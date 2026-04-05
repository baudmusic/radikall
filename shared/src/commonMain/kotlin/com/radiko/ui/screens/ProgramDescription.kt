package com.radiko.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiko.i18n.appStrings
import com.radiko.platform.openUrl
import com.radiko.station.ProgramEntry
import com.radiko.ui.theme.RadikoColors
import com.radiko.ui.theme.radikoPanelBorderColor
import com.radiko.ui.theme.radikoPanelColor
import com.radiko.ui.theme.radikoPrimaryTextColor
import com.radiko.ui.theme.radikoSecondaryTextColor

@Composable
fun ProgramDescription(
    program: ProgramEntry?,
) {
    val strings = appStrings()
    val descriptionBlocks = buildList {
        program?.description?.takeIf { it.isNotBlank() }?.let { add(stripHtml(it)) }
        program?.info?.takeIf { it.isNotBlank() }?.let { add(stripHtml(it)) }
    }.filter { it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = strings.programDetailsTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = RadikoColors.White,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = radikoPanelColor(),
                    shape = RoundedCornerShape(24.dp),
                )
                .border(
                    width = 1.dp,
                    color = radikoPanelBorderColor(),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (descriptionBlocks.isEmpty()) {
                Text(
                    text = strings.noProgramDetails,
                    style = MaterialTheme.typography.bodyMedium,
                    color = radikoSecondaryTextColor(0.76f),
                )
            } else {
                descriptionBlocks.forEach { block ->
                    Text(
                        text = block,
                        style = MaterialTheme.typography.bodyLarge,
                        color = radikoPrimaryTextColor(),
                    )
                }
            }
        }

        program?.url?.takeIf { it.isNotBlank() }?.let { url ->
            ProgramSiteSection(
                title = strings.programWebsiteTitle,
                ctaLabel = strings.openWebsite,
                url = url,
                onOpen = { openUrl(url) },
            )
        }
    }
}

@Composable
private fun ProgramSiteSection(
    title: String,
    ctaLabel: String,
    url: String,
    onOpen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = radikoPanelColor(),
                shape = RoundedCornerShape(24.dp),
            )
            .border(
                width = 1.dp,
                color = radikoPanelBorderColor(),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = radikoPrimaryTextColor(),
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodyLarge,
            color = radikoPrimaryTextColor(),
        )
        TextButton(onClick = onOpen) {
            Text(
                text = ctaLabel,
                color = RadikoColors.PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

fun stripHtml(html: String): String {
    var text = decodeHtmlEntities(html)
    text = text
        .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
        .replace(Regex("(?i)<\\s*br\\s*/?>"), "\n")
        .replace(Regex("(?i)</\\s*(p|div|tr|table|tbody|section|article|ul|ol|h[1-6])\\s*>"), "\n")
        .replace(Regex("(?i)<\\s*li[^>]*>"), "\n- ")
        .replace(Regex("(?i)<\\s*td[^>]*>"), " ")
        .replace(Regex("(?i)<\\s*th[^>]*>"), " ")
        .replace(Regex("<[^>]+>"), "")

    text = decodeHtmlEntities(text)

    return text
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private fun decodeHtmlEntities(value: String): String {
    return value
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
}
