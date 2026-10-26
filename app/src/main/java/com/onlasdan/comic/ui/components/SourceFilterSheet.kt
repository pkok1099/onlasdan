package com.onlasdan.comic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.source.Filter
import com.onlasdan.comic.ui.theme.DarkOutline
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.theme.TextPrimary
import com.onlasdan.comic.ui.theme.TextSecondary
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import kotlinx.coroutines.withContext

/**
 * Bottom sheet filter generik: me-render [Filter] apa pun dari [com.onlasdan.comic.ui.source.ComicSource]
 * sebagai data. Tidak ada cabang per source di sini.
 *
 * State di-hoist: [selected] adalah Map<key, value>, untuk filter multi
 * nilai digabung koma. [onSelectionChange] dipanggil tiap toggle,
 * [onApply]/[onReset] untuk aksi footer.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SourceFilterSheet(
    filters: List<Filter>,
    selected: Map<String, String>,
    onSelectionChange: (Map<String, String>) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Visibilitas ditentukan data (enabledIf), bukan kondisi manual.
    val visibleFilters = remember(filters, selected) {
        filters.filter { filter ->
            filter.enabledIf?.all { (key, value) -> selected[key] == value } ?: true
        }
    }

    NoirBottomSheet(onDismiss = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            overscrollEffect = null,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.browse_filters_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            items(
                items = visibleFilters,
                key = { it.key },
                contentType = { "filter_group" },
            ) { filter ->
                FilterGroup(
                    filter = filter,
                    currentValue = selected[filter.key],
                    onSingleSelect = { optionId ->
                        onSelectionChange(
                            if (selected[filter.key] == optionId) {
                                selected - filter.key
                            } else {
                                selected + (filter.key to optionId)
                            },
                        )
                    },
                    onMultiToggle = { optionId ->
                        val current = selected[filter.key]
                            ?.split(",")
                            ?.filter { it.isNotBlank() }
                            .orEmpty()
                            .toMutableSet()
                        if (!current.add(optionId)) current.remove(optionId)
                        onSelectionChange(
                            if (current.isEmpty()) {
                                selected - filter.key
                            } else {
                                selected + (filter.key to current.joinToString(","))
                            },
                        )
                    },
                    onClear = { onSelectionChange(selected - filter.key) },
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                FilterSheetFooter(
                    onReset = onReset,
                    onApply = onApply,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(
    filter: Filter,
    currentValue: String?,
    onSingleSelect: (String) -> Unit,
    onMultiToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = filter.label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filter.multi) {
                val selectedIds = currentValue?.split(",").orEmpty().toSet()
                filter.options.forEach { option ->
                    SelectableChip(
                        label = option.label,
                        selected = option.id in selectedIds,
                        onClick = { onMultiToggle(option.id) },
                    )
                }
            } else {
                SelectableChip(
                    label = stringResource(R.string.browse_all),
                    selected = currentValue == null,
                    onClick = onClear,
                )
                filter.options.forEach { option ->
                    SelectableChip(
                        label = option.label,
                        selected = currentValue == option.id,
                        onClick = { onSingleSelect(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // Tactile punch: tiap tap membuat chip "memantul" kecil via NoirSpatialFast.
    // Tidak menyentuh click handling internal M3 FilterChip.
    var punch by remember { mutableIntStateOf(0) }
    val punchScale = remember { Animatable(1f) }
    LaunchedEffect(punch) {
        if (punch > 0) {
            withContext(NoirForceMotionScale) {
                punchScale.animateTo(0.88f, NoirMotion.SpatialFast.asFloat)
                punchScale.animateTo(1f, NoirMotion.SpatialFast.asFloat)
            }
        }
    }
    FilterChip(
        selected = selected,
        onClick = {
            punch++
            onClick()
        },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = Modifier.graphicsLayer {
            scaleX = punchScale.value
            scaleY = punchScale.value
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryPink.copy(alpha = 0.18f),
            selectedLabelColor = PrimaryPink,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = DarkOutline,
            selectedBorderColor = PrimaryPink,
        ),
    )
}

@Composable
private fun FilterSheetFooter(onReset: () -> Unit, onApply: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(
            text = stringResource(R.string.browse_btn_reset),
            onClick = onReset,
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = stringResource(R.string.browse_btn_apply_filter),
            onClick = onApply,
            modifier = Modifier.weight(1f),
        )
    }
}
