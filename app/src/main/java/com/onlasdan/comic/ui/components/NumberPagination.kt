package com.onlasdan.comic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.noirClickable

/**
 * Pagination angka tunggal untuk semua layar (gaya Noir, sama seperti section
 * Update di home Shinigami): jendela nomor halaman dengan panah geser. Bila
 * [totalPages] null (total tak diketahui), tampilkan panah + nomor saat ini.
 */
@Composable
fun NumberPagination(
    currentPage: Int,
    totalPages: Int?,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalPages != null && totalPages <= 1) return

    val pagesToShow = remember(currentPage, totalPages) {
        if (totalPages != null) paginationWindow(currentPage, totalPages) else emptyList()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaginationNavChip(label = "‹", enabled = currentPage > 1) { onPageSelected(currentPage - 1) }
        Spacer(modifier = Modifier.width(Spacing.xs))
        PaginationPages(currentPage, totalPages, pagesToShow, onPageSelected)
        Spacer(modifier = Modifier.width(Spacing.xs))
        PaginationNavChip(label = "›", enabled = totalPages == null || currentPage < totalPages) {
            onPageSelected(currentPage + 1)
        }
    }
}

@Composable
private fun PaginationPages(
    currentPage: Int,
    totalPages: Int?,
    pagesToShow: List<Int?>,
    onPageSelected: (Int) -> Unit,
) {
    if (totalPages != null) {
        pagesToShow.forEach { pageNum ->
            if (pageNum == null) {
                PaginationEllipsis()
            } else {
                PaginationPageChip(
                    text = pageNum.toString(),
                    selected = pageNum == currentPage,
                    enabled = pageNum != currentPage,
                ) { onPageSelected(pageNum) }
            }
        }
    } else {
        PaginationPageChip(text = currentPage.toString(), selected = true, enabled = false) {}
    }
}

@Composable
private fun PaginationNavChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) DarkSurfaceElevated else DarkSurface,
        border = BorderStroke(StrokeWidth.hairline, if (enabled) DarkOutline else Color.Transparent),
        modifier = Modifier
            .size(34.dp)
            .noirClickable(pressedScale = 0.90f, enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun PaginationPageChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) PrimaryPink else DarkSurfaceElevated,
        border = BorderStroke(StrokeWidth.hairline, if (selected) PrimaryPink else DarkOutline),
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(34.dp)
            .noirClickable(pressedScale = 0.90f, enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) Color.White else TextPrimary,
            )
        }
    }
}

@Composable
private fun PaginationEllipsis() {
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "…", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Jendela nomor halaman untuk pagination: halaman tepi selalu tampil,
 * tengah diringkas dengan penanda null (ellipsis).
 */
fun paginationWindow(currentPage: Int, totalPages: Int): List<Int?> {
    if (totalPages <= 5) return (1..totalPages).map { it }
    val middle: List<Int?> = when {
        currentPage <= 3 -> listOf(2, 3, 4, null)
        currentPage >= totalPages - 2 -> listOf(null, totalPages - 3, totalPages - 2, totalPages - 1)
        else -> listOf(null, currentPage - 1, currentPage, currentPage + 1, null)
    }
    return listOf(1) + middle + totalPages
}
