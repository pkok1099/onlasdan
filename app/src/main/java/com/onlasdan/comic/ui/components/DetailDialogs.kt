package com.onlasdan.comic.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.theme.DarkSurfaceElevated
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.theme.TextPrimary
import com.onlasdan.comic.ui.theme.TextSecondary

// Dialog sheet detail yang diekstrak dari ComicDetailSheet (Fase A2):
// komponen bodoh — semua logika tetap di sheet pemanggil.
@Composable
fun MarkPreviousReadDialog(chapterTitle: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.detail_mark_previous_read), style = MaterialTheme.typography.titleMedium, color = TextPrimary) },
        text = { Text(text = "Tandai semua chapter sebelum $chapterTitle sebagai sudah dibaca?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.detail_mark_read_selected), color = PrimaryPink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.detail_cancel), color = TextSecondary) } },
        containerColor = DarkSurfaceElevated,
    )
}

@Composable
fun DetailDeleteAllDialog(chapterCount: Int, sizeText: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.detail_delete_all_downloads), style = MaterialTheme.typography.titleMedium, color = TextPrimary) },
        text = { Text(text = stringResource(R.string.detail_delete_all_downloads_confirm, chapterCount, sizeText), style = MaterialTheme.typography.bodyMedium, color = TextSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.detail_confirm), color = PrimaryPink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.detail_cancel), color = TextSecondary) } },
        containerColor = DarkSurfaceElevated,
    )
}
