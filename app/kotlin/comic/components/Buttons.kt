package comic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import comic.theme.*
import comic.util.pressScale

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    PrimaryButton(text = text, onClick = onClick, modifier = modifier, icon = icon, enabled = enabled, isLoading = false)
}

// Varian berstatus loading (Fase A UX): saat isLoading, tombol menampilkan
// spinner seukuran ikon dan menonaktifkan dirinya (anti double-tap untuk
// operasi async seperti backup/restore).
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        modifier = modifier
            // Rombak komik 2026-10-27: hard shadow kuning ala cetakan + efek tekan
            // "tenggelam ke bayangan" (urutan penting: shadow LUAR, press DALAM).
            .comicHardShadow(ComicSfxYellow, offsetX = 3.dp, offsetY = 3.dp, cornerRadius = ButtonCornerRadius)
            .comicPress(interactionSource, offsetX = 3.dp, offsetY = 3.dp, pressedScale = 0.97f)
            .heightIn(min = 44.dp),
        enabled = enabled && !isLoading,
        shape = ButtonShape,
        interactionSource = interactionSource,
        border = BorderStroke(1.5.dp, InkBlack),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink, contentColor = PureBlack, disabledContainerColor = DarkSurfaceElevated, disabledContentColor = TextSecondary),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.sm),
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PureBlack, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(Spacing.sm))
            }

            icon != null -> {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Spacing.sm))
            }
        }
        Text(text = text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SecondaryButton(text = text, onClick = onClick, modifier = modifier, icon = icon, enabled = enabled, isLoading = false)
}

// Varian berstatus loading — pola yang sama dengan PrimaryButton.
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .comicHardShadow(InkLine.copy(alpha = 0.16f), offsetX = 2.dp, offsetY = 2.dp, cornerRadius = ButtonCornerRadius)
            .comicPress(interactionSource, offsetX = 2.dp, offsetY = 2.dp, pressedScale = 0.97f)
            .heightIn(min = 44.dp),
        enabled = enabled && !isLoading,
        shape = ButtonShape,
        interactionSource = interactionSource,
        border = BorderStroke(StrokeWidth.thick, InkLine.copy(alpha = 0.42f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = TextPrimary, disabledContainerColor = PureBlack, disabledContentColor = TextSecondary),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(Spacing.xs))
            }

            icon != null -> {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

private data class AppChipColors(val border: Color, val bg: Color, val text: Color)

private fun appChipColors(selected: Boolean) = if (selected) {
    AppChipColors(ComicSfxPink, ComicSfxPink.copy(alpha = 0.14f), BlushWhite)
} else {
    AppChipColors(DarkOutline, DarkSurface, TextSecondary)
}

/** Hard shadow pink hanya pada chip terpilih (diekstrak agar kompleksitas AppChip rendah). */
private fun Modifier.appChipComicShadow(selected: Boolean): Modifier = if (selected) {
    comicHardShadow(ComicSfxPink.copy(alpha = 0.35f), offsetX = 2.dp, offsetY = 2.dp, cornerRadius = ChipCornerRadius)
} else {
    this
}

@Composable
fun AppChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = appChipColors(selected)

    Surface(
        modifier = modifier
            .appChipComicShadow(selected)
            .then(
                if (onClick != null) {
                    Modifier
                        .pressScale(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                } else {
                    Modifier
                },
            ),
        shape = ChipShape,
        color = colors.bg,
        border = BorderStroke(if (selected) StrokeWidth.thick else StrokeWidth.regular, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = colors.text,
                maxLines = 1,
            )
        }
    }
}

/**
 * Segmented filter pill berdasar Gestalt Law of Uniform Connectedness & Similarity.
 * Menyatukan pilihan kategori/filter dalam kontainer tunggal yang konsisten.
 */
@Composable
fun <T> SegmentedFilterPill(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(19.dp),
        color = DarkSurface,
        border = BorderStroke(StrokeWidth.regular, DarkOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val targetBg = if (isSelected) PrimaryPink else Color.Transparent
                val targetText = if (isSelected) PureBlack else TextSecondary
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(targetBg)
                        .pressScale(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            onItemSelected(item)
                        }
                        .padding(horizontal = Spacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelProvider(item),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = targetText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
