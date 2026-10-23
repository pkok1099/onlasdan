package com.onlasdan.comic.ui.screens.reader

import androidx.compose.runtime.compositionLocalOf
import com.onlasdan.comic.ui.source.Chapter
import com.onlasdan.comic.ui.util.extractSlug

/** Referer untuk memuat gambar halaman source aktif (bug A15); diisi ReaderContent dari SOURCE_REFERERS. */
val LocalReaderPageReferer = compositionLocalOf<String?> { null }

enum class ReaderExpandedTab {
    Chapters,
    Settings,
}

sealed interface ReaderContinuousItem {
    data class Page(
        val chapterSlug: String,
        val chapterTitle: String?,
        val chapterNumber: String,
        val pageIndex: Int,
        val totalPages: Int,
        val imageUrl: String,
    ) : ReaderContinuousItem

    data class ChapterDivider(
        val completedChapterSlug: String,
        val completedChapterNumber: String,
        val nextChapterSlug: String,
        val nextChapterNumber: String,
    ) : ReaderContinuousItem

    data class EndOfSeries(
        val nextChapterTitle: String?,
        val prevChapterTitle: String?,
    ) : ReaderContinuousItem
}

data class LoadedChapter(
    val slug: String,
    val title: String?,
    val number: String,
    val images: List<String>,
)

/** Detail komik untuk kebutuhan reader: judul, sampul, dan daftar chapter (model ui.source). */
data class ReaderComicDetail(
    val title: String,
    val cover: String?,
    val chapterList: List<Chapter>,
)

data class ReaderState(
    val title: String,
    val chapterSlug: String,
    val chapterTitle: String?,
    val chapterNumber: String,
    val chapterIndex: Int,
    val chapterCount: Int,
    val pageIndex: Int,
    val pageCount: Int,
    val isBookmarked: Boolean,
    val source: String,
    val showControls: Boolean,
    val autoScrollActive: Boolean,
    val autoNextEnabled: Boolean,
    val comicDetails: ReaderComicDetail?,
)

data class ReaderActions(
    val onBack: () -> Unit,
    val onToggleBookmark: () -> Unit,
    val onChapters: () -> Unit,
    val onSettings: () -> Unit,
    val onNavigateChapter: (String) -> Unit,
    val onAutoScroll: () -> Unit,
    val onToggleAutoNext: () -> Unit,
    val onScrollTop: () -> Unit,
    val onScrollBottom: () -> Unit,
)

fun extractChapterNumber(
    item: Chapter?,
    title: String?,
    index: Int,
    total: Int,
): String {
    val parsedNum = item?.number?.trim()?.toDoubleOrNull()
    if (parsedNum != null && parsedNum > 0.0 && parsedNum < 5000.0) {
        return if (parsedNum % 1.0 == 0.0) parsedNum.toInt().toString() else parsedNum.toString()
    }

    if (!title.isNullOrBlank()) {
        val match = Regex("""(?i)\b(?:ch(?:apter)?\.?|ep(?:isode)?\.?|bab)\s*(\d+(?:\.\d+)?)""").find(title)
        val numStr = match?.groupValues?.get(1)
        val numVal = numStr?.toDoubleOrNull()
        if (numVal != null && numVal > 0.0 && numVal < 5000.0) {
            return if (numVal % 1.0 == 0.0) numVal.toInt().toString() else numVal.toString()
        }
    }

    val order = if (total > 0 && index in 0 until total) {
        total - index
    } else {
        index + 1
    }
    return order.toString()
}

fun isChapterSlugMatch(item: Chapter, slug: String): Boolean {
    if (slug.isBlank()) return false
    val cleanSlug = slug.trim()
    val rawId = item.id?.trim().orEmpty()
    val rawUrl = item.url?.trim().orEmpty()
    val extractedFromUrl = extractSlug(rawUrl)
    val urlLastSegment = rawUrl.trimEnd('/').substringAfterLast('/')

    val idAfterColon = rawId.substringAfterLast(':')
    val slugAfterColon = cleanSlug.substringAfterLast(':')

    return cleanSlug.equals(rawId, ignoreCase = true) ||
        cleanSlug.equals(extractedFromUrl, ignoreCase = true) ||
        cleanSlug.equals(urlLastSegment, ignoreCase = true) ||
        slugAfterColon.equals(idAfterColon, ignoreCase = true) ||
        slugAfterColon.equals(rawId, ignoreCase = true) ||
        slugAfterColon.equals(extractedFromUrl, ignoreCase = true) ||
        cleanSlug.equals(idAfterColon, ignoreCase = true)
}

// Nomor bab dari sinyal asli saja (field number / pola judul) — TANPA
// fallback indeks. Dipakai deteksi arah agar tidak sirkular (bug B13):
// extractChapterNumber mencampur fallback (total - index) yang selalu
// membuat elemen pertama tampak terbesar, apa pun urutan aslinya.
private fun parsedChapterNumber(item: Chapter): Double? {
    val fromField = item.number?.trim()?.toDoubleOrNull()
    if (fromField != null && fromField > 0.0 && fromField < 5000.0) return fromField
    val match = Regex("""(?i)\b(?:ch(?:apter)?\.?|ep(?:isode)?\.?|bab)\s*(\d+(?:\.\d+)?)""").find(item.title)
    val fromTitle = match?.groupValues?.get(1)?.toDoubleOrNull()
    return if (fromTitle != null && fromTitle > 0.0 && fromTitle < 5000.0) fromTitle else null
}

private fun isDescendingOrder(chapterList: List<Chapter>): Boolean {
    if (chapterList.size < 2) return true
    val firstNum = parsedChapterNumber(chapterList.first())
    val lastNum = parsedChapterNumber(chapterList.last())
    // Tanpa sinyal nomor yang terbaca, ikuti asumsi descending yang sama
    // dengan fallback penomoran di extractChapterNumber (total - index).
    if (firstNum == null || lastNum == null || firstNum == lastNum) return true
    return firstNum > lastNum
}

fun resolveNextChapterItem(
    chapterList: List<Chapter>,
    currentChapterSlug: String,
): Chapter? {
    if (chapterList.isEmpty()) return null
    val currentIdx = chapterList.indexOfFirst { isChapterSlugMatch(it, currentChapterSlug) }
    if (currentIdx < 0) return null

    return if (isDescendingOrder(chapterList)) {
        if (currentIdx > 0) chapterList[currentIdx - 1] else null
    } else {
        if (currentIdx in 0 until chapterList.lastIndex) chapterList[currentIdx + 1] else null
    }
}

fun resolvePrevChapterItem(
    chapterList: List<Chapter>,
    currentChapterSlug: String,
): Chapter? {
    if (chapterList.isEmpty()) return null
    val currentIdx = chapterList.indexOfFirst { isChapterSlugMatch(it, currentChapterSlug) }
    if (currentIdx < 0) return null

    return if (isDescendingOrder(chapterList)) {
        if (currentIdx in 0 until chapterList.lastIndex) chapterList[currentIdx + 1] else null
    } else {
        if (currentIdx > 0) chapterList[currentIdx - 1] else null
    }
}

fun buildContinuousItems(
    loadedChapters: List<LoadedChapter>,
    seamlessChaptersEnabled: Boolean,
    comicDetails: ReaderComicDetail?,
    initialChapterSlug: String,
    initialChapterTitle: String?,
    initialChapterNumber: String,
    initialImages: List<String>,
): List<ReaderContinuousItem> {
    val result = mutableListOf<ReaderContinuousItem>()
    if (!seamlessChaptersEnabled || loadedChapters.isEmpty()) {
        val currentImages = loadedChapters.firstOrNull { it.slug == initialChapterSlug }?.images ?: initialImages
        currentImages.forEachIndexed { idx, url ->
            result.add(
                ReaderContinuousItem.Page(
                    chapterSlug = initialChapterSlug,
                    chapterTitle = initialChapterTitle,
                    chapterNumber = initialChapterNumber,
                    pageIndex = idx,
                    totalPages = currentImages.size,
                    imageUrl = url,
                ),
            )
        }
        val nextCh = comicDetails?.chapterList?.let { resolveNextChapterItem(it, initialChapterSlug) }
        val prevCh = comicDetails?.chapterList?.let { resolvePrevChapterItem(it, initialChapterSlug) }
        result.add(
            ReaderContinuousItem.EndOfSeries(
                nextChapterTitle = nextCh?.title,
                prevChapterTitle = prevCh?.title,
            ),
        )
    } else {
        loadedChapters.forEachIndexed { chIdx, ch ->
            if (chIdx > 0) {
                val prevCh = loadedChapters[chIdx - 1]
                result.add(
                    ReaderContinuousItem.ChapterDivider(
                        completedChapterSlug = prevCh.slug,
                        completedChapterNumber = prevCh.number,
                        nextChapterSlug = ch.slug,
                        nextChapterNumber = ch.number,
                    ),
                )
            }
            ch.images.forEachIndexed { pIdx, url ->
                result.add(
                    ReaderContinuousItem.Page(
                        chapterSlug = ch.slug,
                        chapterTitle = ch.title,
                        chapterNumber = ch.number,
                        pageIndex = pIdx,
                        totalPages = ch.images.size,
                        imageUrl = url,
                    ),
                )
            }
        }
        val lastLoadedSlug = loadedChapters.lastOrNull()?.slug
        val hasMoreChapters = lastLoadedSlug != null &&
            resolveNextChapterItem(comicDetails?.chapterList.orEmpty(), lastLoadedSlug) != null
        if (!hasMoreChapters && loadedChapters.isNotEmpty()) {
            val prevCh = comicDetails?.chapterList?.let { resolvePrevChapterItem(it, lastLoadedSlug ?: initialChapterSlug) }
            result.add(
                ReaderContinuousItem.EndOfSeries(
                    nextChapterTitle = null,
                    prevChapterTitle = prevCh?.title,
                ),
            )
        }
    }
    return result
}
