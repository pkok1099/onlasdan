package comic.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import comic.R
import comic.db.ComicDatabase
import comic.theme.Spacing
import comic.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Fase D (Loss Aversion): teks pelindung cadangan menyebut aset nyata milik
// pengguna (jumlah markah + riwayat) alih-alih deskripsi teknis. Jumlah
// dibaca dari basis data lokal lewat query COUNT (tanpa memuat entitas);
// selama belum termuat atau data masih nol, tampil teks ramah tanpa angka.

@Composable
fun rememberLibraryCounts(): Pair<Int, Int>? {
    val context = LocalContext.current
    var counts by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(Unit) {
        counts = withContext(Dispatchers.IO) {
            val db = ComicDatabase.get(context)
            db.bookmarkDao().countAll() to db.historyDao().countAll()
        }
    }
    return counts
}

@Composable
fun backupProtectMessage(bookmarkCount: Int, historyCount: Int): String = when {
    bookmarkCount > 0 && historyCount > 0 ->
        pluralStringResource(R.plurals.backup_protect_both, bookmarkCount, bookmarkCount, historyCount)

    bookmarkCount > 0 ->
        pluralStringResource(R.plurals.backup_protect_bookmarks, bookmarkCount, bookmarkCount)

    historyCount > 0 ->
        pluralStringResource(R.plurals.backup_protect_history, historyCount, historyCount)

    else -> stringResource(R.string.backup_protect_default)
}

@Composable
fun BackupProtectText() {
    val counts = rememberLibraryCounts()
    Text(
        text = counts?.let { backupProtectMessage(it.first, it.second) } ?: stringResource(R.string.backup_protect_default),
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md),
    )
}
