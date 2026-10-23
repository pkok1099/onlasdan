package com.onlasdan.comic.ui.components

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
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.pressScale

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        modifier = modifier
            .pressScale(interactionSource)
            .heightIn(min = 44.dp),
        enabled = enabled,
        shape = ButtonShape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryPink,
            contentColor = PureBlack,
            disabledContainerColor = DarkSurfaceElevated,
            disabledContentColor = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.sm),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
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
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .pressScale(interactionSource)
            .heightIn(min = 44.dp),
        enabled = enabled,
        shape = ButtonShape,
        interactionSource = interactionSource,
        border = BorderStroke(StrokeWidth.regular, DarkOutline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            disabledContainerColor = PureBlack,
            disabledContentColor = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = TextPrimary,
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
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
    val borderColor = if (selected) PrimaryPink else DarkOutline
    val bgColor = if (selected) DarkSurfaceElevated else DarkSurface
    val textColor = if (selected) AccentPink else TextSecondary

    Surface(
        modifier = modifier
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
        color = bgColor,
        border = BorderStroke(StrokeWidth.regular, borderColor),
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
                    tint = textColor,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
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
