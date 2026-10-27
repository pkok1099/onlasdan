package comic.util

import coil.ImageLoader
import coil.decode.BitmapFactoryDecoder
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import java.io.IOException

/**
 * Decoder factory Coil yang memeriksa integritas file gambar sebelum decoding.
 * Mencegah BitmapFactory mendekode JPEG terpotong menjadi gambar setengah kosong.
 */
class ImageIntegrityDecoderFactory : Decoder.Factory {
    override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder = ImageIntegrityDecoder(result.source, options)
}

class ImageIntegrityDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {
    private val delegate = BitmapFactoryDecoder(source, options)

    override suspend fun decode(): DecodeResult {
        val file = runCatching { source.fileOrNull()?.toFile() }.getOrNull()
        if (file != null && file.exists() && !ImageIntegrityValidator.isComplete(file)) {
            // Hapus file cache yang terpotong agar retry berikutnya mengunduh ulang yang utuh
            runCatching { file.delete() }
            throw IOException("File gambar terpotong atau tidak lengkap: ${file.name}")
        }
        return delegate.decode()
    }
}
