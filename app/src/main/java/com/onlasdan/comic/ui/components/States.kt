package com.onlasdan.comic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.ui.theme.*

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    message: String = "Tidak ada konten yang ditemukan",
    icon: ImageVector = Icons.Default.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                SecondaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ErrorOutline,
    debugMessage: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MutedRed,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            // Baris diagnostik: pesan error mentah agar akar masalah bisa
            // dilaporkan (bukan sekadar pesan ramah yang generik).
            if (!debugMessage.isNullOrBlank()) {
                Text(
                    text = debugMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            PrimaryButton(
                text = "Coba Lagi",
                onClick = onRetry,
                icon = Icons.Default.Refresh,
            )
        }
    }
}
