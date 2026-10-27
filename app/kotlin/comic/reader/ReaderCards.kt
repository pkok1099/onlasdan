package comic.reader

import android.os.BatteryManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import comic.R
import comic.components.PrimaryButton
import comic.theme.*
import comic.util.noirClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ConnectingNextChapterCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NoirSurface,
            border = BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.35f)),
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    color = PrimaryPink,
                    trackColor = DarkOutline,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.reader_connecting_next_chapter),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
fun ChapterDividerCard(
    completedChapterNumber: String,
    nextChapterNumber: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl, horizontal = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NoirSurface,
            border = BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.45f)),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryPink,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.reader_chapter_divider_title, "Ch. $completedChapterNumber"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary,
                )
                Text(
                    text = "•",
                    color = PrimaryPink,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(R.string.reader_chapter_divider_next, "Ch. $nextChapterNumber"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryPink,
                )
            }
        }
    }
}

@Composable
fun EndOfChapterCard(
    nextChapterTitle: String?,
    prevChapterTitle: String?,
    onNavigateNext: (() -> Unit)?,
    onNavigatePrev: (() -> Unit)?,
    onBackToDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLatest = onNavigateNext == null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(StrokeWidth.regular, DarkOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = if (isLatest) Icons.Default.CheckCircle else Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = PrimaryPink,
                    modifier = Modifier.size(36.dp),
                )

                Text(
                    text = stringResource(if (isLatest) R.string.reader_end_latest_title else R.string.reader_end_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = if (isLatest) {
                        stringResource(R.string.reader_end_latest_desc)
                    } else {
                        stringResource(R.string.reader_end_next_label, nextChapterTitle.orEmpty())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                if (onNavigateNext != null) {
                    PrimaryButton(
                        text = stringResource(R.string.reader_end_btn_next),
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onNavigateNext,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (onNavigatePrev != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = BorderStroke(StrokeWidth.regular, DarkOutline),
                            modifier = Modifier
                                .weight(1f)
                                .noirClickable(pressedScale = 0.94f) { onNavigatePrev() },
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.reader_end_btn_prev),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextSecondary,
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLatest) PrimaryPink else DarkSurface,
                        border = BorderStroke(StrokeWidth.regular, if (isLatest) PrimaryPink else DarkOutline),
                        modifier = Modifier
                            .weight(1f)
                            .noirClickable(pressedScale = 0.94f) { onBackToDetail() },
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isLatest) PureBlack else TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.reader_end_btn_detail),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isLatest) PureBlack else TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmbientStatusOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    var batteryPercent by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                batteryPercent = (level * 100) / scale
            }
            delay(30_000L)
        }
    }

    val statusText = if (batteryPercent >= 0) "$currentTime • $batteryPercent%" else currentTime

    Surface(
        color = PureBlack.copy(alpha = 0.50f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(StrokeWidth.hairline, DarkOutline.copy(alpha = 0.4f)),
        modifier = modifier.padding(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = TextSecondary.copy(alpha = 0.75f),
            )
        }
    }
}
