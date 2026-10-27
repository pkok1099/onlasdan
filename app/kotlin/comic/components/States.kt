package comic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import comic.R
import comic.theme.*
import comic.util.NoirAnimatedVisibility

// ============ Perlengkapan state gaya komik (rombak komik 2026-10-27) ============

/** Starburst sticker untuk ikon state — bentuk dipakai ulang agar border & clip identik. */
private val StateBurstShape = ComicBurstShape(points = 12, innerRatio = 0.68f, rotationDegrees = -8f)

/** Caption box komik untuk pesan state — ekor menunjuk ke bawah (arah aksi). */
private val StateBubbleShape = ComicSpeechBubbleShape(cornerRatio = 0.18f, tailAnchorX = 0.74f)

/**
 * Sticker burst komik: ikon state duduk di dalam action-burst kuning/merah ber-outline
 * tinta tebal + hard shadow tipis — penggati ikon "float" generik.
 */
@Composable
private fun StateBurstBadge(icon: ImageVector, container: Color, iconTint: Color) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .comicHardShadow(container.copy(alpha = 0.30f), offsetX = 2.dp, offsetY = 3.dp, cornerRadius = 30.dp)
            .clip(StateBurstShape)
            .background(container)
            .border(BorderStroke(1.5.dp, InkBlack), StateBurstShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Speech bubble komik untuk pesan state (menggantikan teks polos). */
@Composable
private fun ComicStateBubble(message: String, accent: Color, textColor: Color) {
    Surface(
        shape = StateBubbleShape,
        color = DarkSurfaceElevated,
        border = BorderStroke(1.25.dp, accent),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xs),
        )
    }
}

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
            StateBurstBadge(icon = icon, container = PopYellow, iconTint = InkBlack)
            ComicStateBubble(message = message, accent = InkLine.copy(alpha = 0.5f), textColor = TextSecondary)
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
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    var showDetail by remember { mutableStateOf(false) }
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
            ErrorStateHeader(icon = icon, message = message)
            Spacer(modifier = Modifier.height(Spacing.xs))
            PrimaryButton(
                text = stringResource(R.string.common_try_again),
                onClick = onRetry,
                icon = Icons.Default.Refresh,
            )
            ErrorStateExtras(
                actionLabel = actionLabel,
                onAction = onAction,
                debugMessage = debugMessage,
                detailExpanded = showDetail,
                onToggleDetail = { showDetail = !showDetail },
            )
        }
    }
}

@Composable
private fun ErrorDebugDetail(debugMessage: String, expanded: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val copiedText = stringResource(R.string.error_detail_copied)
    TextButton(onClick = onToggle, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
        Text(
            text = stringResource(if (expanded) R.string.error_hide_detail else R.string.error_show_detail),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
    NoirAnimatedVisibility(visible = expanded, slideDistance = 16.dp) {
        ErrorDebugDetailBody(context = context, debugMessage = debugMessage, copiedText = copiedText)
    }
}

@Composable
private fun ErrorDebugDetailBody(context: android.content.Context, debugMessage: String, copiedText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = debugMessage,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = TextSecondary.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("error", debugMessage))
                AppToast.success(copiedText)
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(text = stringResource(R.string.error_copy_detail), style = MaterialTheme.typography.labelSmall, color = PrimaryPink)
        }
    }
}

@Composable
private fun ErrorStateHeader(icon: ImageVector, message: String) {
    StateBurstBadge(icon = icon, container = PopRed, iconTint = InkBlack)
    ComicStateBubble(message = message, accent = PopRed.copy(alpha = 0.6f), textColor = TextPrimary)
}

// Aksi sekunder (Fase C: tawaran unduhan saat offline) + gerbang detail
// teknis di balik toggle kecil + salin untuk laporan bug beta (Fase B2).
@Composable
private fun ErrorStateExtras(
    actionLabel: String?,
    onAction: (() -> Unit)?,
    debugMessage: String?,
    detailExpanded: Boolean,
    onToggleDetail: () -> Unit,
) {
    if (actionLabel != null && onAction != null) {
        SecondaryButton(text = actionLabel, onClick = onAction)
    }
    if (!debugMessage.isNullOrBlank()) {
        ErrorDebugDetail(debugMessage = debugMessage, expanded = detailExpanded, onToggle = onToggleDetail)
    }
}
