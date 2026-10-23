package com.onlasdan.comic.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlasdan.comic.ui.source.SourcePreferences
import com.onlasdan.comic.ui.source.SourceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SourceManager {
    const val SOURCE_KOMIKINDO = "komikindo"
    const val SOURCE_KOMIKU = "komiku"
    const val SOURCE_SHINIGAMI = "shinigami"
    const val SOURCE_DOUJINDESU = "doujindesu"

    val AVAILABLE_SOURCES = listOf(
        SOURCE_KOMIKINDO,
        SOURCE_KOMIKU,
        SOURCE_SHINIGAMI,
        SOURCE_DOUJINDESU,
    )

    fun isValidSource(source: String): Boolean = source.lowercase() in AVAILABLE_SOURCES
}

@Composable
fun rememberSelectedSource(): String {
    val context = LocalContext.current
    val sourceFlow = remember(context) { SourcePreferences.selectedSourceId(context) }
    val source by sourceFlow.collectAsStateWithLifecycle(initialValue = SourceRegistry.defaultSource.id)
    return source
}

fun setSelectedSource(context: Context, source: String) {
    CoroutineScope(Dispatchers.IO).launch {
        SourcePreferences.setSelectedSourceId(context, source)
    }
}
