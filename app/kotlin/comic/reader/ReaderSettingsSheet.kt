package comic.reader

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import comic.R
import comic.theme.*
import kotlin.math.roundToInt

@Composable
@Suppress("kotlin:S107", "kotlin:S3776")
fun ReaderSettingsSheetContent(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    autoNextEnabled: Boolean,
    onAutoNextChange: (Boolean) -> Unit,
    autoNextDelaySeconds: Float,
    onAutoNextDelayChange: (Float) -> Unit,
    immersiveFullscreen: Boolean,
    onToggleFullscreen: (Boolean) -> Unit,
    keepScreenOnEnabled: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    seamlessChaptersEnabled: Boolean,
    onSeamlessChange: (Boolean) -> Unit,
    nightDimmerAlpha: Float,
    onNightDimmerChange: (Float) -> Unit,
    onClearFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Section 1: Auto Scroll Speed
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.reader_auto_scroll_speed),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reader_speed_value, speed.roundToInt()),
                    color = PrimaryPink,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
                var textInput by remember(speed) { mutableStateOf(speed.roundToInt().toString()) }
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { newVal ->
                        textInput = newVal
                        newVal.toFloatOrNull()?.let {
                            if (it in 10f..4000f) onSpeedChange(it)
                        }
                    },
                    modifier = Modifier.width(90.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onClearFocus() }),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = DarkOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    placeholder = { Text(stringResource(R.string.reader_speed_placeholder), style = MaterialTheme.typography.labelSmall) },
                )
            }
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 10f..3500f,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryPink,
                    activeTrackColor = PrimaryPink,
                    inactiveTrackColor = DarkOutline,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                OutlinedButton(
                    onClick = { onSpeedChange((speed - 50f).coerceAtLeast(10f)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = 4.dp),
                ) { Text(stringResource(R.string.reader_speed_minus_50), style = MaterialTheme.typography.labelSmall) }
                OutlinedButton(
                    onClick = { onSpeedChange((speed - 10f).coerceAtLeast(10f)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = 4.dp),
                ) { Text(stringResource(R.string.reader_speed_minus_10), style = MaterialTheme.typography.labelSmall) }
                OutlinedButton(
                    onClick = { onSpeedChange((speed + 10f).coerceAtMost(4000f)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = 4.dp),
                ) { Text(stringResource(R.string.reader_speed_plus_10), style = MaterialTheme.typography.labelSmall) }
                OutlinedButton(
                    onClick = { onSpeedChange((speed + 50f).coerceAtMost(4000f)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = 4.dp),
                ) { Text(stringResource(R.string.reader_speed_plus_50), style = MaterialTheme.typography.labelSmall) }
            }
            if (speed > 2000f) {
                Text(
                    text = stringResource(R.string.reader_warning_speed_high),
                    color = PrimaryPink,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }

        HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f), thickness = StrokeWidth.hairline)

        // Section 2: Auto Next (Terkunci Aktif jika Seamless ON, Delay OFF)
        val isAutoNextLockedBySeamless = seamlessChaptersEnabled
        val effectiveAutoNextChecked = if (isAutoNextLockedBySeamless) true else autoNextEnabled

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(
                            text = stringResource(R.string.reader_auto_next),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        if (isAutoNextLockedBySeamless) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryPink.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, PrimaryPink.copy(alpha = 0.4f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = PrimaryPink,
                                        modifier = Modifier.size(10.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.reader_autonext_locked_badge),
                                        color = PrimaryPink,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = if (isAutoNextLockedBySeamless) {
                            stringResource(R.string.reader_autonext_seamless_locked_desc)
                        } else {
                            stringResource(R.string.reader_auto_next_desc)
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = effectiveAutoNextChecked,
                    onCheckedChange = { if (!isAutoNextLockedBySeamless) onAutoNextChange(it) },
                    enabled = !isAutoNextLockedBySeamless,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = PrimaryPink,
                        disabledCheckedThumbColor = PureBlack.copy(alpha = 0.8f),
                        disabledCheckedTrackColor = PrimaryPink.copy(alpha = 0.5f),
                    ),
                )
            }

            // Delay OFF saat Seamless ON. Hanya tampil jika Auto Next aktif dan mode Seamless tidak aktif.
            if (effectiveAutoNextChecked && !isAutoNextLockedBySeamless) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.reader_autonext_delay_label),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = stringResource(R.string.reader_autonext_delay_value, autoNextDelaySeconds),
                            color = PrimaryPink,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Slider(
                        value = autoNextDelaySeconds,
                        onValueChange = onAutoNextDelayChange,
                        valueRange = 1.0f..8.0f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryPink,
                            activeTrackColor = PrimaryPink,
                            inactiveTrackColor = DarkOutline,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.reader_autonext_delay_desc),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f), thickness = StrokeWidth.hairline)

        // Section 3: Fullscreen Immersive
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reader_setting_fullscreen),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.reader_setting_fullscreen_desc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = immersiveFullscreen,
                onToggleFullscreen,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureBlack,
                    checkedTrackColor = PrimaryPink,
                ),
            )
        }

        HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f), thickness = StrokeWidth.hairline)

        // Section 4: Keep Screen Awake
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reader_keep_screen_on_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.reader_keep_screen_on_desc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = keepScreenOnEnabled,
                onCheckedChange = onKeepScreenOnChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureBlack,
                    checkedTrackColor = PrimaryPink,
                ),
            )
        }

        HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f), thickness = StrokeWidth.hairline)

        // Section 5: Night Dimmer (Peredup Layar)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reader_night_dimmer_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "${(nightDimmerAlpha * 100).toInt()}%",
                    color = if (nightDimmerAlpha > 0.05f) PrimaryPink else TextSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            Text(
                text = stringResource(R.string.reader_night_dimmer_desc, (nightDimmerAlpha * 100).toInt()),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Slider(
                value = nightDimmerAlpha,
                onValueChange = onNightDimmerChange,
                valueRange = 0f..0.70f,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryPink,
                    activeTrackColor = PrimaryPink,
                    inactiveTrackColor = DarkOutline,
                ),
                modifier = Modifier.padding(vertical = Spacing.xxs),
            )
        }

        HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f), thickness = StrokeWidth.hairline)

        // Section 7: Seamless Next Chapter (Baca Berkelanjutan)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reader_seamless_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.reader_seamless_desc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = seamlessChaptersEnabled,
                onCheckedChange = onSeamlessChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureBlack,
                    checkedTrackColor = PrimaryPink,
                ),
            )
        }

        HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f), thickness = StrokeWidth.hairline)

        // Section 8: Paging Tombol Volume (B1) — state dibaca langsung dari
        // holder global ReaderVolumeKeys (dikonsumsi MainActivity), persist ke prefs.
        val context = LocalContext.current
        val volumePrefs = remember(context) {
            context.getSharedPreferences("comic_prefs", Context.MODE_PRIVATE)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reader_volume_paging_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.reader_volume_paging_desc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = ReaderVolumeKeys.enabled,
                onCheckedChange = { on ->
                    ReaderVolumeKeys.enabled = on
                    volumePrefs.edit { putBoolean("reader_volume_paging_enabled", on) }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureBlack,
                    checkedTrackColor = PrimaryPink,
                ),
            )
        }
        if (ReaderVolumeKeys.enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reader_volume_invert_title),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = stringResource(R.string.reader_volume_invert_desc),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = ReaderVolumeKeys.invert,
                    onCheckedChange = { inv ->
                        ReaderVolumeKeys.invert = inv
                        volumePrefs.edit { putBoolean("reader_volume_invert", inv) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = PrimaryPink,
                    ),
                )
            }
        }
    }
}
