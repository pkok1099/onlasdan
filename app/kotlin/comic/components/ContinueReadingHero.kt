package comic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import comic.R
import comic.db.ComicDatabase
import comic.db.HistoryEntity
import comic.theme.CardShape
import comic.theme.DarkSurfaceElevated
import comic.theme.InkLine
import comic.theme.PrimaryPink
import comic.theme.Spacing
import comic.theme.TextPrimary
import comic.theme.TextSecondary
import comic.util.noirClickable

/**
 * Hero "Lanjutkan Membaca" (C2 laporan detail kecil): kartu resume di puncak
 * beranda — cover + judul + posisi terakhir ("Ch. N · NN%") + tombol lanjut.
 * Sumber data = entri riwayat terbaru (observeLatest, reaktif via invalidasi
 * Room) sehingga kartu otomatis pindah ke bacaan terbaru tanpa polling.
 * Sembunyi sifatnya per-sesi supaya kehadirannya tenang; kembali saat restart.
 */
@Composable
fun ContinueReadingHero(
    onResume: (comicSlug: String, sourceId: String, chapterSlug: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dismissedThisSession by remember { mutableStateOf(false) }
    val entry = rememberLatestHistoryEntry() ?: return
    if (dismissedThisSession) return

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, InkLine.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .noirClickable(pressedScale = 0.97f) {
                    onResume(entry.comicSlug, entry.source, entry.chapterSlug)
                }
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            HeroCoverThumb(thumbnail = entry.thumbnail, title = entry.title)
            HeroInfo(entry, modifier = Modifier.weight(1f))
            HeroTrailing(onDismiss = { dismissedThisSession = true })
        }
    }
}

/** Entri riwayat terbaru sebagai state reaktif (Flow Room). */
@Composable
private fun rememberLatestHistoryEntry(): HistoryEntity? {
    val context = LocalContext.current
    val latest by remember { ComicDatabase.get(context).historyDao().observeLatest() }
        .collectAsStateWithLifecycle(initialValue = null)
    return latest
}

/** Cover 52dp rasio 2:3, sudut CardShape senada kartu komik lain. */
@Composable
private fun HeroCoverThumb(thumbnail: String?, title: String) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .aspectRatio(2f / 3f)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AsyncImage(
            model = thumbnail,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Label aksi + judul + posisi baca ("Ch. N · NN%"). */
@Composable
private fun HeroInfo(entry: HistoryEntity, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.resume_hero_label).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PrimaryPink,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = resumePositionText(entry),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Tombol play dekoratif (baris utuh bisa ditekan) + sembunyi per-sesi. */
@Composable
private fun HeroTrailing(onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryPink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.resume_hero_cd),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.resume_hero_dismiss_cd),
            tint = TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier
                .size(18.dp)
                .noirClickable(pressedScale = 0.85f) { onDismiss() },
        )
    }
}

/** "Ch. 45 · 62%" — persen dari halaman tersimpan; tanpa totalPages = judul saja. */
private fun resumePositionText(entry: HistoryEntity): String {
    val chapter = entry.chapterTitle ?: entry.chapterSlug
    val percent = if (entry.totalPages > 0) {
        ((entry.lastPage.coerceAtLeast(1) * 100) / entry.totalPages).coerceIn(1, 100)
    } else {
        null
    }
    return if (percent != null) "$chapter · $percent%" else chapter
}
