package comic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import comic.source.ComicSource
import comic.source.SourceRegistry
import comic.theme.*
import comic.util.pressScale

@Composable
fun ElasticNoirBrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Rombak komik 2026-10-27: wordmark pakai lettering Bangers (font komik POW!)
        // — identitas brand yang menyatu dengan tema, bukan sans generik.
        Text(
            text = "ONLASDAN",
            fontFamily = ComicLetteringFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 21.sp,
            letterSpacing = 1.sp,
            color = TextPrimary,
        )
        Text(
            text = ".",
            fontFamily = ComicLetteringFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            color = ComicSfxPink,
        )
    }
}

@Composable
fun SourceSwitcherTrigger(
    currentSource: ComicSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val dotColor = remember(currentSource.id) { ComicVibrantColors.forOrigin(currentSource.id) }
    Surface(
        shape = PillShape,
        color = NoirSurface.copy(alpha = 0.90f),
        border = BorderStroke(StrokeWidth.thick, dotColor.copy(alpha = 0.6f)),
        modifier = modifier
            .comicHardShadow(dotColor.copy(alpha = 0.45f), offsetX = 2.dp, offsetY = 2.dp, cornerRadius = 20.dp)
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            Text(
                text = currentSource.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary,
            )
            Icon(Icons.Default.KeyboardArrowDown, "Pilih Sumber", Modifier.size(16.dp), dotColor)
        }
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
