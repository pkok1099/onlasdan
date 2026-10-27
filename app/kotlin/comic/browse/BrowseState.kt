package comic.browse

import androidx.compose.ui.text.input.TextFieldValue
import comic.source.Comic
import comic.source.Paged

data class BrowseState(
    val source: String,
    val showFilters: Boolean,
    val page: Int,
    val data: Paged<Comic>?,
    val filterOptions: BrowseFilterOptions,
    val error: String?,
    val isLoading: Boolean,
    val komiku: KomikuFilters,
    val komikindo: KomikindoFilters,
    val shinigami: ShinigamiFilters,
    val doujindesu: DoujindesuFilters = DoujindesuFilters(),
    val searchTerm: TextFieldValue,
    val activeQuery: String?,
)

/** Satu opsi filter untuk sheet filter Browse (label tampil + nilai kirim). */
data class BrowseFilterOption(val label: String, val value: String)

/** Kumpulan opsi filter per kelompok, dibangun dari ComicSource.availableFilters(). */
data class BrowseFilterOptions(
    val genres: List<BrowseFilterOption> = emptyList(),
    val demografis: List<BrowseFilterOption> = emptyList(),
    val tema: List<BrowseFilterOption> = emptyList(),
    val konten: List<BrowseFilterOption> = emptyList(),
    val status: List<BrowseFilterOption> = emptyList(),
    val type: List<BrowseFilterOption> = emptyList(),
    val format: List<BrowseFilterOption> = emptyList(),
    val orderBy: List<BrowseFilterOption> = emptyList(),
    val sortTime: List<BrowseFilterOption> = emptyList(),
)

data class KomikuFilters(
    val orderby: String = "modified",
    val tipe: String = "",
    val status: String = "",
    val genre: String = "",
    val sorttime: String = "",
)

data class ShinigamiFilters(
    val sort: String = "latest",
    val format: String = "",
    val status: String = "",
    val genre: String = "",
)

data class KomikindoFilters(
    val title: String? = null,
    val type: String = "",
    val status: String = "",
    val format: String = "",
    val order: String = "",
    val genres: Set<String> = emptySet(),
    val demografis: Set<String> = emptySet(),
    val konten: Set<String> = emptySet(),
    val tema: Set<String> = emptySet(),
)

data class DoujindesuFilters(
    val sort: String = "latest_chapter",
    val type: String = "",
    val status: String = "",
    val genre: String = "",
)
