package com.onlasdan.comic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.onlasdan.comic.R

// Dialog sheet detail (Fase A2, dibangun ulang di atas AppConfirmDialog
// pada Fase B1): komponen bodoh — semua logika tetap di sheet pemanggil.
@Composable
fun MarkPreviousReadDialog(chapterTitle: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppConfirmDialog(
        title = stringResource(R.string.detail_mark_previous_read),
        message = stringResource(R.string.detail_mark_previous_body, chapterTitle),
        confirmLabel = stringResource(R.string.detail_mark_read_selected),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
fun DetailDeleteAllDialog(chapterCount: Int, sizeText: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppConfirmDialog(
        title = stringResource(R.string.detail_delete_all_downloads),
        message = pluralStringResource(R.plurals.detail_delete_all_downloads_confirm, chapterCount, chapterCount, sizeText),
        confirmLabel = stringResource(R.string.detail_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
    )
}
