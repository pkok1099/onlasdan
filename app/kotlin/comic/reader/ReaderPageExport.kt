package comic.reader

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.size.Size
import comic.R
import comic.theme.DarkSurfaceElevated
import comic.theme.PrimaryPink
import comic.theme.Spacing
import comic.theme.TextPrimary
import comic.theme.TextSecondary
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog aksi halaman reader (B9 — long-press halaman): simpan ke galeri
 * atau bagikan, ditata gaya panel Noir (satu-satunya dialog memakai
 * AppConfirmDialog; ini varian dua-aksi karena butuh pilihan, bukan konfirmasi).
 */
@Composable
fun ReaderPageActionsDialog(
    pageNumber: Int,
    chapterNumber: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = DarkSurfaceElevated,
        title = { ReaderPageActionsTitle(pageNumber = pageNumber, chapterNumber = chapterNumber) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                PageActionButton(icon = Icons.Default.SaveAlt, label = stringResource(R.string.reader_page_save), onClick = onSave)
                PageActionButton(icon = Icons.Default.Share, label = stringResource(R.string.reader_page_share), onClick = onShare)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = TextSecondary)
            }
        },
    )
}

@Composable
private fun ReaderPageActionsTitle(pageNumber: Int, chapterNumber: String) {
    Text(
        text = stringResource(R.string.reader_page_actions_title, pageNumber) + " · Ch. " + chapterNumber,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
    )
}

@Composable
private fun PageActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryPink,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * Ekspor halaman reader (B9): bitmap diambil ulang lewat Coil (cache disk,
 * file unduhan "file://", atau jaringan dengan header referer source), lalu
 * disimpan ke Pictures/Onlasdan via MediaStore atau dibagikan lewat
 * FileProvider (cache/share/). Min SDK 35: MediaStore RELATIVE_PATH tanpa izin.
 */
object ReaderPageExport {
    private const val GALLERY_DIR = "Onlasdan"
    private const val JPEG_QUALITY = 95

    private suspend fun loadBitmap(context: Context, imageUrl: String, referer: String?): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .apply { referer?.let { addHeader("Referer", it) } }
                .allowHardware(false)
                .size(Size.ORIGINAL)
                .build()
            Coil.imageLoader(context).execute(request).drawable?.toBitmap()
        }.getOrNull()
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.replace(Regex("[\\\\/:*?\"<>|]"), "-").trim().take(60)
        return cleaned.ifEmpty { "page" }
    }

    /** Simpan halaman ke MediaStore (Pictures/Onlasdan). @return sukses. */
    suspend fun saveToGallery(context: Context, imageUrl: String, referer: String?, baseName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = loadBitmap(context, imageUrl, referer) ?: error("bitmap null")
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "${sanitizeFileName(baseName)}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$GALLERY_DIR")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("insert null")
            resolver.openOutputStream(uri)?.use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) { "compress false" }
            } ?: error("output stream null")
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }.isSuccess
    }

    /** Tulis halaman ke cache/share lalu buka chooser berbagi. @return sukses. */
    suspend fun share(context: Context, imageUrl: String, referer: String?, baseName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = loadBitmap(context, imageUrl, referer) ?: error("bitmap null")
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(dir, "${sanitizeFileName(baseName)}.jpg")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(send, null))
            }
        }.isSuccess
    }
}
