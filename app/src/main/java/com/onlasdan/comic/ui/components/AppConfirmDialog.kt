package com.onlasdan.comic.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.theme.ComicVibrantColors
import com.onlasdan.comic.ui.theme.DarkSurfaceElevated
import com.onlasdan.comic.ui.theme.ErrorRed
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.theme.TextPrimary
import com.onlasdan.comic.ui.theme.TextSecondary

/**
 * Satu-satunya dialog konfirmasi di aplikasi (Fase B1) — menggantikan
 * AlertDialog mentah per-layar dan ConfirmDialog privat Settings.
 *
 * @param destructive true untuk aksi hapus/timpa data: label konfirmasi
 * berwarna merah error.
 * @param isLoading true saat aksi konfirmasi sedang berjalan: tombol
 * konfirmasi berubah jadi spinner dan dialog tidak bisa ditutup.
 */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    destructive: Boolean = false,
    isLoading: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        modifier = modifier,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        },
        confirmButton = {
            ConfirmButton(label = confirmLabel, destructive = destructive, isLoading = isLoading, onClick = onConfirm)
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(text = dismissLabel ?: stringResource(R.string.common_cancel), color = TextSecondary)
            }
        },
        containerColor = DarkSurfaceElevated,
    )
}

// Tombol aksi utama dialog: terisi penuh (Fase B2) — merah error untuk
// aksi destruktif, pink primer untuk aksi normal; spinner saat loading.
@Composable
private fun ConfirmButton(label: String, destructive: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) ComicVibrantColors.ManhuaCrimson else ComicVibrantColors.ComicPink,
            contentColor = Color.White,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text = label)
        }
    }
}
