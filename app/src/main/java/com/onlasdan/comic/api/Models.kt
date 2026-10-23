package com.onlasdan.comic.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(val data: T? = null, val error: String? = null)

data class HomeLatestChapter(val title: String? = null, val url: String? = null)

data class HomeComicItem(
    val title: String,
    val url: String? = null,
    @SerializedName("cover") val cover: String? = null,
    val type: String? = null,
    val latestChapter: HomeLatestChapter? = null,
    val rating: String? = null,
    val lastUpdated: String? = null,
)

data class HomeHeroComic(
    val title: String,
    val url: String? = null,
    @SerializedName("cover") val cover: String? = null,
    val rating: String? = null,
    val author: String? = null,
    val type: String? = null,
    val description: String? = null,
    val latestChapter: HomeLatestChapter? = null,
)

data class HomeGenreCard(val name: String, val url: String, val image: String)

data class HomeStats(val totalTitles: String? = null, val totalChapters: String? = null)

data class HomepageData(
    val heroComic: HomeHeroComic? = null,
    val latestComics: List<HomeComicItem> = emptyList(),
    val popularComics: List<HomeComicItem> = emptyList(),
    // Komiku specific
    val hotManga: List<HomeComicItem>? = null,
    val hotManhwa: List<HomeComicItem>? = null,
    val hotManhua: List<HomeComicItem>? = null,
    val genreCards: List<HomeGenreCard>? = null,
    val stats: HomeStats? = null,
    val source: String? = null,
)

data class ComicDetail(
    val id: String? = null,
    val title: String,
    val thumbnail: String? = null,
    val rating: String? = null,
    val status: String? = null,
    val type: String? = null,
    val author: String? = null,
    val illustrator: String? = null,
    val genres: List<String> = emptyList(),
    val synopsis: String? = null,
    @SerializedName("chapterList") val chapterList: List<ChapterItem> = emptyList(),
    val alternativeTitles: List<String> = emptyList(),
    val lastUpdated: String? = null,
    val officialLinks: List<ExternalLink> = emptyList(),
    val infoLinks: List<ExternalLink> = emptyList(),
    val recommendations: List<HomeComicItem> = emptyList(),
    val source: String? = null,
    val groups: List<String> = emptyList(),
    val series: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
    val views: String? = null,
)

data class ExternalLink(val name: String, val url: String)

data class ChapterItem(
    val id: String? = null,
    val number: String? = null,
    val title: String,
    val url: String? = null,
    val realUrl: String? = null,
    val lastUpdated: String? = null,
    val downloadUrl: String? = null,
)

data class ChapterData(val images: List<String> = emptyList())

data class ComicListItem(
    val title: String,
    val url: String? = null,
    @SerializedName("cover") val cover: String? = null,
    val type: String? = null,
    val rating: Double? = null,
    val latestChapter: HomeLatestChapter? = null,
    val lastUpdated: String? = null,
)

data class BrowseFilterOption(val label: String, val value: String)

data class BrowseFilterOptions(
    val genres: List<BrowseFilterOption> = emptyList(),
    val demografis: List<BrowseFilterOption> = emptyList(),
    val tema: List<BrowseFilterOption> = emptyList(),
    val konten: List<BrowseFilterOption> = emptyList(),
    val status: List<BrowseFilterOption> = emptyList(),
    val type: List<BrowseFilterOption> = emptyList(),
    val format: List<BrowseFilterOption> = emptyList(),
    @SerializedName("orderby") val orderBy: List<BrowseFilterOption> = emptyList(),
    @SerializedName("sorttime") val sortTime: List<BrowseFilterOption> = emptyList(),
)

data class PaginationData(
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val nextPageUrl: String? = null,
    val prevPageUrl: String? = null,
)

data class BrowseData(
    val comics: List<ComicListItem> = emptyList(),
    val filterOptions: BrowseFilterOptions? = null,
    val pagination: PaginationData? = null,
)

data class SearchData(val comics: List<ComicListItem> = emptyList(), val totalPages: Int? = null)

data class GenrePageData(
    val genreName: String? = null,
    val genreSlug: String? = null,
    val comics: List<HomeComicItem> = emptyList(),
    val pagination: PaginationData? = null,
)

data class ProjectsData(val projects: List<ComicListItem> = emptyList())

data class UpdatesData(val comics: List<ComicListItem> = emptyList(), val totalPages: Int? = null)
