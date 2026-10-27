package comic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import comic.theme.ComicVibrantColors

private data class BadgeColorConfig(val accent: Color, val bg: Color)

private fun resolveCategoryColors(type: String): BadgeColorConfig {
    val lower = type.lowercase()
    return when {
        lower.contains("manhwa") -> BadgeColorConfig(ComicVibrantColors.ManhwaCyan, Color(0xFF041820))
        lower.contains("manhua") -> BadgeColorConfig(ComicVibrantColors.ManhuaOrange, Color(0xFF220E04))
        lower.contains("manga") -> BadgeColorConfig(ComicVibrantColors.MangaYellow, Color(0xFF201B04))
        lower.contains("doujin") -> BadgeColorConfig(ComicVibrantColors.ComicPink, Color(0xFF200612))
        else -> BadgeColorConfig(ComicVibrantColors.ComicPink, Color(0xFF181014))
    }
}

/**
 * Comic Manga Badge dengan aksen vibran berkarakter (Manhwa: Cyan, Manga: Yellow, Manhua: Orange, Doujin: Pink).
 */
@Composable
fun ComicCategoryBadge(
    type: String?,
    modifier: Modifier = Modifier,
) {
    if (type.isNullOrBlank()) return
    val cleanType = type.trim()
    val (accentColor, darkBgColor) = resolveCategoryColors(cleanType)

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = darkBgColor.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.70f)),
        modifier = modifier,
    ) {
        BadgeCapsuleContent(label = cleanType.uppercase(), color = accentColor)
    }
}

/**
 * Comic Manga Status Badge (Completed: Emerald Aura, Ongoing: Azure Sky).
 */
@Composable
fun ComicStatusBadge(
    status: String?,
    modifier: Modifier = Modifier,
) {
    if (status.isNullOrBlank()) return
    val cleanStatus = status.trim()
    val isCompleted = cleanStatus.equals("Completed", ignoreCase = true) || cleanStatus.equals("Tamat", ignoreCase = true)

    val accent = if (isCompleted) ComicVibrantColors.AuraEmerald else ComicVibrantColors.ManhwaSky
    val bg = if (isCompleted) Color(0xFF021B12) else Color(0xFF061828)
    val label = if (isCompleted) "TAMAT" else "ONGOING"

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
        modifier = modifier,
    ) {
        BadgeCapsuleContent(label = label, color = accent)
    }
}

@Composable
private fun BadgeCapsuleContent(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
            ),
            color = color,
        )
    }
}

/**
 * Comic Panel Bleed Border: Memberikan frame panel komik dengan efek
 * sudut beraksen warna-warni dan garis tepi dinamis.
 */
@Composable
fun ComicPanelBleedBox(
    modifier: Modifier = Modifier,
    borderColor: Color = ComicVibrantColors.ComicPink,
    cornerRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val borderBrush = Brush.linearGradient(
        listOf(
            borderColor.copy(alpha = 0.8f),
            ComicVibrantColors.ManhwaCyan.copy(alpha = 0.6f),
            borderColor.copy(alpha = 0.3f),
        ),
    )
    Box(
        modifier = modifier.border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(cornerRadius)),
    ) {
        content()
    }
}
