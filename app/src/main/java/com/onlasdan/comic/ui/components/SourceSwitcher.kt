package com.onlasdan.comic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.ui.source.ComicSource
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.pressScale

@Composable
fun SourceSwitcherTrigger(
    currentSource: ComicSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = Spacing.xs, horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currentSource.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Pilih Sumber",
            tint = AccentPink,
            modifier = Modifier.size(18.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSwitcherSheet(
    selectedSourceId: String,
    onSourceSelect: (ComicSource) -> Unit,
    onDismiss: () -> Unit,
) {
    val sources = remember { SourceRegistry.sources }

    NoirBottomSheet(
        onDismiss = onDismiss,
        sheetHeight = 440.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = Spacing.xxl),
        ) {
            Text(
                text = "Pilih Sumber Komik",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )

            HorizontalDivider(color = DarkOutline, thickness = StrokeWidth.regular)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                overscrollEffect = null,
            ) {
                items(
                    items = sources,
                    key = { it.id },
                    contentType = { "source_item" },
                ) { source ->
                    val isSelected = source.id.equals(selectedSourceId, ignoreCase = true)
                    val interactionSource = remember { MutableInteractionSource() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(interactionSource)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                onSourceSelect(source)
                                onDismiss()
                            }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            Surface(
                                shape = ChipShape,
                                color = if (isSelected) PrimaryPink.copy(alpha = 0.15f) else DarkSurfaceElevated,
                                border = BorderStroke(
                                    StrokeWidth.regular,
                                    if (isSelected) PrimaryPink else DarkOutline,
                                ),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = source.iconRes),
                                        contentDescription = null,
                                        tint = if (isSelected) PrimaryPink else TextSecondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) AccentPink else TextPrimary,
                                )
                                Text(
                                    text = "${source.capabilities.size} Fitur",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Aktif",
                                tint = PrimaryPink,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = DarkOutline, thickness = StrokeWidth.hairline)
                }
            }
        }
    }
}
