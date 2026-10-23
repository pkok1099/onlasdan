package com.onlasdan.comic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.pressScale

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )

        if (actionText != null && onActionClick != null) {
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .pressScale(interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) { onActionClick() }
                    .padding(vertical = Spacing.xs, horizontal = Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentPink,
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AccentPink,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
