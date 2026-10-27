package com.spoolstudio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spoolstudio.app.domain.models.FilamentSpool
import com.spoolstudio.app.ui.theme.SpoolStudioColors

fun spoolmanDropdownLabel(filament: FilamentSpool): String {
    return "${filament.brand} - ${filament.spoolmanName} - ${filament.displayName}"
}

fun spoolmanDropdownListLabel(filament: FilamentSpool): String {
    val idLabel = filament.id?.let { "ID #$it" } ?: "No ID"
    return "$idLabel - ${spoolmanDropdownLabel(filament)}"
}

fun filterSpoolmanDropdownFilaments(
    filaments: List<FilamentSpool>,
    searchQuery: String
): List<FilamentSpool> {
    val query = searchQuery.trim()
    if (query.isBlank()) return filaments

    return filaments.filter { filament ->
        listOf(
            filament.id?.toString().orEmpty(),
            filament.brand,
            filament.spoolmanName.orEmpty(),
            filament.material,
            filament.materialModifier,
            filament.displayName,
            filament.variant,
            filament.location.orEmpty(),
            filament.lotNr.orEmpty(),
            filament.colorHex.orEmpty()
        ).any { value ->
            value.contains(query, ignoreCase = true)
        }
    }
}

@Composable
fun SpoolRemainingWeightBadge(
    remainingWeight: Float?,
    modifier: Modifier = Modifier
) {
    val weight = remainingWeight?.takeIf { it >= 0f }
    val (text, textColor, bgColor, borderColor) = when {
        weight == null -> {
            WeightBadgeStyle(
                text = "—",
                textColor = SpoolStudioColors.OnGraphiteMuted.copy(alpha = 0.7f),
                bgColor = SpoolStudioColors.Graphite.copy(alpha = 0.55f),
                borderColor = SpoolStudioColors.GraphiteMuted.copy(alpha = 0.45f)
            )
        }
        weight >= 500f -> {
            WeightBadgeStyle(
                text = formatBadgeWeight(weight),
                textColor = Color(0xFF4ADE80), // Bright high-contrast emerald green
                bgColor = Color(0xFF132E1E),     // Dark forest green background
                borderColor = Color(0xFF22C55E).copy(alpha = 0.55f)
            )
        }
        weight >= 100f -> {
            WeightBadgeStyle(
                text = formatBadgeWeight(weight),
                textColor = Color(0xFFFBBF24), // Bright warm amber
                bgColor = Color(0xFF332408),     // Dark warm amber background
                borderColor = Color(0xFFF59E0B).copy(alpha = 0.55f)
            )
        }
        else -> {
            WeightBadgeStyle(
                text = formatBadgeWeight(weight),
                textColor = Color(0xFFF87171), // Bright coral red
                bgColor = Color(0xFF381414),     // Dark red background
                borderColor = Color(0xFFEF4444).copy(alpha = 0.60f)
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp
            ),
            color = textColor,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private data class WeightBadgeStyle(
    val text: String,
    val textColor: Color,
    val bgColor: Color,
    val borderColor: Color
)

private fun formatBadgeWeight(weight: Float): String {
    return if (weight >= 1500f) {
        val kg = weight / 1000f
        if (kg == kg.toInt().toFloat()) "${kg.toInt()}kg" else "%.1fkg".format(kg)
    } else {
        "${weight.toInt()}g"
    }
}
