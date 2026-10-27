package comic.reader

import android.content.Context
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Size
import comic.R
import comic.components.AppToast
import comic.components.ReaderAutoRetryMaxAttempts
import comic.components.ReaderPageSlot
import comic.components.ReaderPageSlotState
import comic.util.NoirMotion
import comic.util.animateNoirFloatAsState
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val ReaderPagePrefetchAhead = 8

// ---- Fisika fling daftar halaman (v1.2.34) ----------------------------------
// Profil S-curve “carry → brake → soft tail” (feedback user: awal glide masih
// terasa berat — model lama mendesak paling keras tepat saat jari lepas).
//   1. CARRY (0..ReaderFlingCarryTime): decel naik halus 0 -> puncak via
//      smoothstep — kecepatan nyaris tak turun (hard flick kehilangan ±12%
//      pada 0.35s pertama, sebelumnya 36%) -> awal terasa ringan/terbawa.
//   2. BRAKE: puncak pengereman proporsional kecepatan lepas
//      (ReaderFlingBrakeFloor + ReaderFlingBrakeScale * |v0|) — sapuan
//      kencang dibunuh efisien, momentum panjang tetap terjaga.
//   3. SOFT TAIL: cap lembut ReaderFlingTailDecel + ReaderFlingTailFriction*|v|
//      — ekor eksponensial pendek, berhenti tanpa sentakan & tanpa micro-crawl.
// decel efektif = min(puncak-ramp, cap-ekor). Total momentum hard flick
// tetap ±1.9 layar, hanya didistribusikan ulang: ringan di awal, kuat di tengah.
// Integrasi mulai persis dari kecepatan lepas jari (handoff mulus), loop frame
// manual (withFrameNanos) di luar MotionDurationScale — fisika scroll kebal
// skala animasi 0x, selaras NoirMotion & native View.
private const val ReaderFlingCarryTime = 0.35f
private const val ReaderFlingBrakeFloor = 650f
private const val ReaderFlingBrakeScale = 0.55f
private const val ReaderFlingTailDecel = 350f
private const val ReaderFlingTailFriction = 1.4f
private const val ReaderFlingStopVelocity = 45f
private const val ReaderFlingMinVelocity = 50f
private const val ReaderFlingMaxFrameDelta = 0.1f
private const val ReaderFlingNanosToSeconds = 1e-9f

@Composable
private fun rememberReaderFlingBehavior(): FlingBehavior {
    return remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                // Sapuan sangat pelan: tidak dianimasikan — biarkan presisi penuh.
                if (abs(initialVelocity) < ReaderFlingMinVelocity) return initialVelocity
                return glideReaderPages(initialVelocity)
            }
        }
    }
}

// Inti fisika: integrasi profil S-curve per frame sampai berhenti atau mentok tepi.
private suspend fun ScrollScope.glideReaderPages(velocityIn: Float): Float {
    var velocity = velocityIn
    var lastFrame = withFrameNanos { it }
    var elapsed = 0f
    while (abs(velocity) >= ReaderFlingStopVelocity) {
        val frame = withFrameNanos { it }
        // Clamp dt: jank/gc tidak boleh membuat lompatan telepor.
        val dt = ((frame - lastFrame) * ReaderFlingNanosToSeconds).coerceIn(0f, ReaderFlingMaxFrameDelta)
        lastFrame = frame
        elapsed += dt
        val decel = readerFlingDecel(elapsed, abs(velocityIn), abs(velocity))
        // Semi-implicit Euler + clamp nol: tidak pernah overshoot melewati nol.
        val next = if (abs(velocity) <= decel * dt) {
            0f
        } else {
            velocity - decel * dt * sign(velocity)
        }
        val delta = (velocity + next) * 0.5f * dt // trapezoid: posisi mulus
        val consumed = scrollBy(delta)
        velocity = next
        if (abs(consumed - delta) > 0.5f) break // tepi list — sisakan velocity
    }
    return velocity
}

// decel = min(puncak-ramp S-curve, cap ekor lembut).
private fun readerFlingDecel(elapsed: Float, initialSpeed: Float, speed: Float): Float {
    val x = (elapsed / ReaderFlingCarryTime).coerceIn(0f, 1f)
    val smoothstep = x * x * (3f - 2f * x) // C1 mulus di release & saat pengereman penuh
    val peak = (ReaderFlingBrakeFloor + ReaderFlingBrakeScale * initialSpeed) * smoothstep
    val tail = ReaderFlingTailDecel + ReaderFlingTailFriction * speed
    return min(peak, tail)
}

@Composable
@Suppress("kotlin:S107", "kotlin:S3776")
fun ReaderImageList(
    items: List<ReaderContinuousItem>,
    isFetchingNextChapter: Boolean,
    listState: LazyListState,
    context: Context,
    onNavigateNext: (() -> Unit)? = null,
    onNavigatePrev: (() -> Unit)? = null,
    onBackToDetail: () -> Unit = {},
    onUserInteraction: () -> Unit,
    onImageSettled: () -> Unit,
    onPageLoaded: (Int) -> Unit,
) {
    val pageReferer = LocalReaderPageReferer.current
    val imageLoader = remember(context) { Coil.imageLoader(context) }
    val displayWidthPx = remember(context) { context.resources.displayMetrics.widthPixels }
    val scope = rememberCoroutineScope()
    // B9: halaman yang di-hold -> dialog Simpan ke Galeri / Bagikan.
    var pageActionTarget by remember { mutableStateOf<ReaderContinuousItem.Page?>(null) }
    val pageSavedText = stringResource(R.string.reader_page_saved)
    val pageFailedText = stringResource(R.string.reader_page_action_failed)

    // Prefetch based on scroll position (single coroutine), not per-item effects.
    val prefetched = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(items, listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val range = calculatePrefetchRange(lastVisible, items.size, ReaderPagePrefetchAhead) ?: return@collect
                for (i in range) {
                    val nextItem = items.getOrNull(i) as? ReaderContinuousItem.Page ?: continue
                    if (prefetched.putIfAbsent(nextItem.imageUrl, true) == null) {
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .apply { pageReferer?.let { addHeader("Referer", it) } }
                                .data(nextItem.imageUrl)
                                .size(Size(Dimension.Pixels(displayWidthPx), Dimension.Undefined))
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .build(),
                        )
                    }
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onUserInteraction()
                    waitForUpOrCancellation()
                    onUserInteraction()
                }
            },
        state = listState,
        flingBehavior = rememberReaderFlingBehavior(),
        contentPadding = PaddingValues(0.dp),
    ) {
        itemsIndexed(
            items = items,
            key = { _, itm ->
                when (itm) {
                    is ReaderContinuousItem.Page -> "${itm.chapterSlug}_${itm.pageIndex}_${itm.imageUrl}"
                    is ReaderContinuousItem.ChapterDivider -> "divider_${itm.completedChapterSlug}_${itm.nextChapterSlug}"
                    is ReaderContinuousItem.EndOfSeries -> "end_of_series"
                }
            },
            contentType = { _, itm ->
                when (itm) {
                    is ReaderContinuousItem.Page -> "reader_page"
                    is ReaderContinuousItem.ChapterDivider -> "reader_divider"
                    is ReaderContinuousItem.EndOfSeries -> "reader_end_of_series"
                }
            },
        ) { idx, item ->
            when (item) {
                is ReaderContinuousItem.Page -> {
                    var slotState by remember(item.imageUrl) { mutableStateOf<ReaderPageSlotState>(ReaderPageSlotState.Loading) }
                    var retryAttempt by remember(item.imageUrl) { mutableIntStateOf(0) }
                    var reloadKey by remember(item.imageUrl) { mutableIntStateOf(0) }
                    val request = remember(item.imageUrl, reloadKey, pageReferer) {
                        ImageRequest.Builder(context)
                            .apply { pageReferer?.let { addHeader("Referer", it) } }
                            .data(item.imageUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }

                    LaunchedEffect(slotState) {
                        if (slotState is ReaderPageSlotState.Retrying) {
                            delay(1500)
                            reloadKey++
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            // B9: long-press halaman -> aksi simpan/bagikan. Tunggu
                            // long-press TANPA konsumsi tap/drag agar toggle kontrol
                            // (clickable induk) dan scroll daftar tetap berfungsi.
                            .pointerInput(item.imageUrl) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    onUserInteraction()
                                    if (awaitLongPressOrCancellation(down.id) != null) {
                                        onUserInteraction()
                                        pageActionTarget = item
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        var isLoaded by remember(item.imageUrl) { mutableStateOf(false) }
                        var settledReported by remember(item.imageUrl) { mutableStateOf(false) }
                        val imageAlpha by animateNoirFloatAsState(
                            targetValue = if (isLoaded) 1f else 0f,
                            animationSpec = NoirMotion.Effects.Fast,
                            label = "readerImageAlpha",
                        )
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { alpha = imageAlpha },
                            contentScale = ContentScale.FillWidth,
                            onState = { state ->
                                isLoaded = state is AsyncImagePainter.State.Success
                                when (state) {
                                    is AsyncImagePainter.State.Error -> {
                                        if (!settledReported) {
                                            settledReported = true
                                            onImageSettled()
                                        }
                                        val nextAttempt = retryAttempt + 1
                                        if (nextAttempt <= ReaderAutoRetryMaxAttempts) {
                                            retryAttempt = nextAttempt
                                            slotState = ReaderPageSlotState.Retrying(
                                                nextAttempt,
                                                ReaderAutoRetryMaxAttempts,
                                            )
                                        } else {
                                            slotState = ReaderPageSlotState.Error
                                            onPageLoaded(idx)
                                        }
                                    }

                                    is AsyncImagePainter.State.Success -> {
                                        if (!settledReported) {
                                            settledReported = true
                                            onImageSettled()
                                        }
                                        onPageLoaded(idx)
                                    }

                                    else -> Unit
                                }
                            },
                        )
                        if (!isLoaded) {
                            ReaderPageSlot(
                                pageNumber = item.pageIndex + 1,
                                state = slotState,
                                onRetry = {
                                    retryAttempt = 0
                                    settledReported = false
                                    slotState = ReaderPageSlotState.Loading
                                    reloadKey++
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                is ReaderContinuousItem.ChapterDivider -> {
                    SideEffect {
                        onPageLoaded(idx)
                    }
                    ChapterDividerCard(
                        completedChapterNumber = item.completedChapterNumber,
                        nextChapterNumber = item.nextChapterNumber,
                    )
                }

                is ReaderContinuousItem.EndOfSeries -> {
                    SideEffect {
                        onPageLoaded(idx)
                    }
                    EndOfChapterCard(
                        nextChapterTitle = item.nextChapterTitle,
                        prevChapterTitle = item.prevChapterTitle,
                        onNavigateNext = onNavigateNext,
                        onNavigatePrev = onNavigatePrev,
                        onBackToDetail = onBackToDetail,
                    )
                }
            }
        }

        if (isFetchingNextChapter) {
            item(key = "reader_connecting_next_chapter", contentType = "reader_connecting") {
                SideEffect {
                    onPageLoaded(items.size)
                }
                ConnectingNextChapterCard()
            }
        }
    }

    pageActionTarget?.let { page ->
        ReaderPageActionsDialog(
            pageNumber = page.pageIndex + 1,
            chapterNumber = page.chapterNumber,
            onDismiss = { pageActionTarget = null },
            onSave = {
                pageActionTarget = null
                scope.launch {
                    val ok = ReaderPageExport.saveToGallery(
                        context = context,
                        imageUrl = page.imageUrl,
                        referer = pageReferer,
                        baseName = "Onlasdan-Ch${page.chapterNumber}-p${page.pageIndex + 1}",
                    )
                    if (ok) AppToast.success(pageSavedText) else AppToast.error(pageFailedText)
                }
            },
            onShare = {
                pageActionTarget = null
                scope.launch {
                    val ok = ReaderPageExport.share(
                        context = context,
                        imageUrl = page.imageUrl,
                        referer = pageReferer,
                        baseName = "Onlasdan-Ch${page.chapterNumber}-p${page.pageIndex + 1}",
                    )
                    if (!ok) AppToast.error(pageFailedText)
                }
            },
        )
    }
}
